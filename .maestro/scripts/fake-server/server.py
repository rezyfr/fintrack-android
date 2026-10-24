#!/usr/bin/env python3
"""A small in-memory stand-in for the FinTrack server (api/openapi.yaml) that the app calls.

Usage: python3 server.py [--port 8765] [--log requests.log]

Data routes: the /v1 endpoints of the FinTrack API. The store keeps one list of rows per table: accounts,
categories, subcategories, transactions, app_settings, rules_doc, budget_lines, card_billing, installments.
The wallet balances are computed from the rest.

Faults and the request log keep one vocabulary for every flow. Each request is named by a method and a table,
for example "POST accounts" for PUT /v1/accounts/<id>, and "PATCH transactions" for a transaction edit.

GET /health answers {"status": "ok"}, as the real server does.

Control routes:
  GET  /__health
  POST /__reset                      empty store, no faults, empty request log
  POST /__seed/<name>                load a seed set from ../../data/seeds.py
  GET  /__info                       today, cycle start day and the cycle range text
  GET  /__state/<table>              dump a table
  GET  /__log[?clear=1]              the request log as a JSON list of strings
  POST /__fault  {"route": "POST accounts", "mode": "500"}
                 mode: 500 | 401 | slow | empty | off. Optional "ms" for slow.
                 route is "<METHOD> <table>", "<table>" for any method, or "*".
  POST /__fault/clear                remove every fault
  POST /__run/<script> {"args": [...]}  run a helper script from .maestro/scripts (emulator only), so a flow
                                        can post a notification or switch airplane mode in the middle of a run.
                                        Allowed scripts: post_notification, set_listener, set_airplane, set_font_scale.

The server listens on 127.0.0.1 only. It accepts any bearer token. The emulator reaches it as http://10.0.2.2:<port>.
"""
import argparse
import datetime as dt
import json
import os
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qsl, urlparse

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.normpath(os.path.join(HERE, "..", "..", "data")))
import seeds  # noqa: E402

LOCK = threading.RLock()
STORE = seeds.empty_store()
FAULTS = {}
REQUESTS = []
LOG_FILE = None
TABLES = set(STORE) | {"wallet_balances"}
NEXT_ID = {}
RUNNABLE = {"post_notification", "set_listener", "set_airplane", "set_font_scale"}
SCRIPT_DIR = os.path.normpath(os.path.join(HERE, ".."))


def log(line):
    with LOCK:
        REQUESTS.append(line)
        if LOG_FILE:
            LOG_FILE.write(line + "\n")
            LOG_FILE.flush()


def load(name):
    global STORE
    with LOCK:
        STORE = seeds.build(name)
        for t in STORE:
            ids = [r.get("id", 0) for r in STORE[t] if isinstance(r.get("id"), int)]
            NEXT_ID[t] = max(ids + [0]) + 1


def reset():
    global STORE
    with LOCK:
        STORE = seeds.empty_store()
        NEXT_ID.clear()
        FAULTS.clear()
        REQUESTS.clear()


def wallet_balances():
    net = {}
    for t in STORE["transactions"]:
        w, kind, amount = t.get("wallet"), t.get("tx_type"), t.get("amount") or 0
        if kind == "income":
            net[w] = net.get(w, 0) + amount
        elif kind in ("expense", "transfer"):
            net[w] = net.get(w, 0) - amount
        if kind == "transfer" and t.get("to_wallet"):
            got = t.get("to_amount") if t.get("to_amount") is not None else amount
            net[t["to_wallet"]] = net.get(t["to_wallet"], 0) + got
    rows = sorted(STORE["accounts"], key=lambda a: (a.get("sort_order", 0), a["id"]))
    return [{"id": a["id"], "name": a["name"], "currency": a["currency"], "type": a.get("type", "checking"),
             "balance": (a.get("opening_balance") or 0) + net.get(a["id"], 0)} for a in rows]


def balances_at(days):
    out = []
    for day in sorted(set(days)):
        net = {}
        for t in STORE["transactions"]:
            if (t.get("date") or "") > day:
                continue
            w, kind, amount = t.get("wallet"), t.get("tx_type"), t.get("amount") or 0
            if kind == "income":
                net[w] = net.get(w, 0) + amount
            elif kind in ("expense", "transfer"):
                net[w] = net.get(w, 0) - amount
            if kind == "transfer" and t.get("to_wallet"):
                got = t.get("to_amount") if t.get("to_amount") is not None else amount
                net[t["to_wallet"]] = net.get(t["to_wallet"], 0) + got
        for a in sorted(STORE["accounts"], key=lambda a: a["id"]):
            out.append({"wallet_id": a["id"], "as_of": day, "balance": (a.get("opening_balance") or 0) + net.get(a["id"], 0)})
    return out


# ---- the FinTrack API ----

TX_COLUMNS = ("id", "item", "amount", "category", "subcategory", "date", "tab", "note", "wallet", "tx_type",
              "to_wallet", "to_amount", "budget_line_id", "installment_id")
TABLE_OF = {"budget-lines": "budget_lines", "card-billing": "card_billing", "wallet-balances": "wallet_balances",
            "transfer-rates": "transactions", "rules": "rules_doc", "settings": "app_settings"}


class ApiError(Exception):
    def __init__(self, status, code, message):
        self.status, self.code, self.message = status, code, message


def not_found(what):
    return ApiError(404, "not_found", f"{what} not found")


def tx_view(t):
    return {k: t.get(k) for k in TX_COLUMNS}


def first(params, key, default=None):
    return next((v for k, v in params if k == key), default)


def next_id(table):
    value = NEXT_ID.get(table, 1)
    NEXT_ID[table] = value + 1
    return value


def parse_ids(text):
    try:
        return [int(x) for x in text.split(",") if x]
    except ValueError:
        raise ApiError(400, "invalid_request", "ids must be integers")


def classify(method, parts, params):
    """Name a request the way the flows do: a legacy method, a table and a legacy query text."""
    resource = parts[1] if len(parts) > 1 else ""
    table = TABLE_OF.get(resource, resource)
    rest = parts[2:]
    legacy = "POST" if method == "PUT" else method
    query = "&".join(f"{k}={v}" for k, v in params)
    if method == "PUT" and resource in ("accounts", "settings", "rules"):
        query = "on_conflict=id"
    elif method == "PUT" and resource == "categories":
        query = "on_conflict=name"
    elif resource in ("accounts", "transactions", "installments") and rest and method in ("PATCH", "DELETE"):
        query = f"id=eq.{rest[0]}"
    elif resource == "transactions" and method == "PATCH":
        query = ""
    elif resource == "transactions" and method == "DELETE":
        query = f"id=in.({first(params, 'ids', '')})"
    elif resource == "categories" and method == "DELETE":
        query = f"name=eq.{first(params, 'name', '')}"
    elif resource == "subcategories" and method == "DELETE":
        query = f"category=eq.{first(params, 'category', '')}&name=eq.{first(params, 'name', '')}"
    return legacy, table, query


def settings_view(row):
    others = row.get("currencies")
    if others is None and row.get("foreign_code"):
        others = [{"code": row["foreign_code"], "label": row.get("foreign_label", ""),
                   "rate": row.get("fallback_rate") or None}]
    return {"home_code": row["home_code"], "home_label": row["home_label"],
            "cycle_start_day": row.get("cycle_start_day", 1), "currencies": others or []}


def upsert(table, key, obj, defaults):
    existing = next((r for r in STORE[table] if r.get(key) == obj.get(key)), None)
    merged = dict(defaults, **obj)
    if existing is not None:
        existing.update(merged)
        return existing
    STORE[table].append(merged)
    return merged


def transactions_list(params):
    q = dict(params)
    search = (q.get("search") or "").lower()

    def keep(t):
        day = t.get("date") or ""
        if q.get("date_from") or q.get("date_to"):
            if q.get("date_from") and day < q["date_from"]:
                return False
            if q.get("date_to") and day > q["date_to"]:
                return False
        elif q.get("month") and not day.startswith(q["month"]):
            return False
        if q.get("wallet") and t.get("wallet") != q["wallet"]:
            return False
        if q.get("involving_wallet") and q["involving_wallet"] not in (t.get("wallet"), t.get("to_wallet")):
            return False
        if q.get("tx_type") and t.get("tx_type") != q["tx_type"]:
            return False
        if q.get("category") and t.get("category") != q["category"]:
            return False
        if search and search not in (t.get("item") or "").lower():
            return False
        amount = t.get("amount") or 0
        if q.get("amount_min") not in (None, "") and amount < float(q["amount_min"]):
            return False
        if q.get("amount_max") not in (None, "") and amount > float(q["amount_max"]):
            return False
        return True

    rows = sorted((t for t in STORE["transactions"] if keep(t)),
                  key=lambda t: (t.get("date") or "", t.get("id") or 0), reverse=True)
    limit, offset = int(q.get("limit", 200)), int(q.get("offset", 0))
    page = rows[offset:offset + limit]
    more = offset + limit < len(rows)
    return {"items": [tx_view(t) for t in page], "next_offset": offset + limit if more else None}


def transfer_rates(params):
    q = dict(params)
    src, dst = set(q.get("from_wallets", "").split(",")), set(q.get("to_wallets", "").split(","))
    rows = sorted((t for t in STORE["transactions"]
                   if t.get("tx_type") == "transfer" and t.get("wallet") in src and t.get("to_wallet") in dst
                   and (t.get("amount") or 0) > 0 and (t.get("to_amount") or 0) > 0),
                  key=lambda t: (t.get("date") or "", t.get("id") or 0), reverse=True)
    return {"rates": [t["to_amount"] / t["amount"] for t in rows[: int(q.get("limit", 10))]]}


def handle(method, parts, params, body, empty):
    """Run one API request on the store. Returns (status, payload). Raises ApiError."""
    resource = parts[1] if len(parts) > 1 else ""
    rest = parts[2:]
    one = rest[0] if rest else None
    tx = STORE["transactions"]

    if resource == "transactions":
        if method == "GET":
            return 200, {"items": [], "next_offset": None} if empty else transactions_list(params)
        if method == "POST":
            if not isinstance(body, dict):
                raise ApiError(400, "invalid_request", "body must be an object")
            row = {col: None for col in TX_COLUMNS}
            row.update(body, id=next_id("transactions"), created_at=dt.datetime.now().isoformat())
            if not empty:
                tx.append(row)
            return 201, tx_view(row)
        if method == "PATCH" and one is None:
            ids = set(body.get("ids", []))
            for row in (tx if not empty else []):
                if row.get("id") in ids:
                    row["category"] = body.get("category")
            return 204, None
        if method == "DELETE" and one is None:
            doomed = set(parse_ids(first(params, "ids", "")))
            if not empty:
                STORE["transactions"] = [r for r in tx if r.get("id") not in doomed]
            return 204, None
        row = next((r for r in tx if str(r.get("id")) == one), None)
        if row is None and not empty:
            raise not_found("Transaction")
        if method == "PATCH":
            if row is not None:
                row.update({k: v for k, v in body.items() if k in TX_COLUMNS and k != "id"})
            return 200, tx_view(row) if row else {}
        if method == "DELETE":
            if row is not None:
                tx.remove(row)
            return 204, None

    if resource == "transfer-rates" and method == "GET":
        return 200, {"rates": []} if empty else transfer_rates(params)

    if resource == "installments":
        rows = STORE["installments"]
        if method == "GET":
            return 200, [] if empty else sorted(rows, key=lambda r: (r.get("due_day", 0), r.get("id", 0)))
        row = next((r for r in rows if str(r.get("id")) == one), None)
        if method == "PATCH":
            if row is None and not empty:
                raise not_found("Installment")
            if row is not None:
                row.update({k: v for k, v in body.items() if k in ("merchant", "current_step", "status", "excluded")})
            return 200, row or {}

    if resource == "budget-lines" and method == "GET":
        rows = [r for r in STORE["budget_lines"] if r.get("active", True)]
        return 200, [] if empty else sorted(rows, key=lambda r: (r.get("sort_order", 0), r.get("id", 0)))

    if resource == "card-billing" and method == "GET":
        return 200, [] if empty else sorted(STORE["card_billing"], key=lambda r: r["wallet"])

    if resource == "accounts":
        if method == "GET":
            rows = sorted(STORE["accounts"], key=lambda a: (a.get("sort_order", 0), a["id"]))
            return 200, [] if empty else [dict({"opening_balance": 0}, **a) for a in rows]
        if method == "PUT" and one:
            defaults = {"sort_order": 0, "is_default": False, "type": "checking", "opening_balance": 0}
            fields = {k: v for k, v in body.items() if k != "id"}
            row = {"id": one, **fields} if empty else upsert("accounts", "id", {"id": one, **fields}, defaults)
            return 200, row
        if method == "DELETE" and one:
            before = len(STORE["accounts"])
            if not empty:
                STORE["accounts"] = [a for a in STORE["accounts"] if a["id"] != one]
                if len(STORE["accounts"]) == before:
                    raise not_found("Account")
            return 204, None

    if resource == "categories":
        name = first(params, "name")
        if method == "GET":
            return 200, [] if empty else sorted(STORE["categories"], key=lambda c: (c.get("sort_order", 0), c["name"]))
        if method == "PUT":
            row = upsert("categories", "name", dict(body), {"sort_order": 0, "is_default": False}) if not empty else dict(body)
            return 200, row
        if method == "DELETE":
            if not empty:
                before = len(STORE["categories"])
                STORE["categories"] = [c for c in STORE["categories"] if c["name"] != name]
                STORE["subcategories"] = [s for s in STORE["subcategories"] if s["category"] != name]
                if len(STORE["categories"]) == before:
                    raise not_found("Category")
            return 204, None

    if resource == "subcategories":
        if method == "GET":
            rows = sorted(STORE["subcategories"], key=lambda s: (s["category"], s.get("sort_order", 0), s["name"]))
            return 200, [] if empty else rows
        if method == "POST":
            row = {"sort_order": 0, **body, "id": next_id("subcategories")}
            if not empty:
                STORE["subcategories"].append(row)
            return 201, row
        if method == "DELETE":
            cat, name = first(params, "category"), first(params, "name")
            if not empty:
                before = len(STORE["subcategories"])
                STORE["subcategories"] = [s for s in STORE["subcategories"] if not (s["category"] == cat and s["name"] == name)]
                if len(STORE["subcategories"]) == before:
                    raise not_found("Subcategory")
            return 204, None

    if resource == "rules":
        if method == "GET":
            row = STORE["rules_doc"][0] if STORE["rules_doc"] and not empty else {}
            return 200, {"rules": row.get("rules", []), "updated_at_ms": row.get("updated_at_ms", 0)}
        if method == "PUT":
            doc = {"id": 1, "rules": body.get("rules", []), "updated_at_ms": body.get("updated_at_ms", 0)}
            if not empty:
                STORE["rules_doc"] = [doc]
            return 200, {"rules": doc["rules"], "updated_at_ms": doc["updated_at_ms"]}

    if resource == "settings":
        if method == "GET":
            if empty or not STORE["app_settings"]:
                raise not_found("Settings")
            return 200, settings_view(STORE["app_settings"][0])
        if method == "PUT":
            row = {"id": 1, "home_code": body["home_code"], "home_label": body["home_label"],
                   "cycle_start_day": body.get("cycle_start_day", 1), "currencies": body.get("currencies", [])}
            if not empty:
                STORE["app_settings"] = [row]
            return 200, settings_view(row)

    if resource == "wallet-balances" and method == "GET":
        if one == "history":
            return 200, [] if empty else balances_at([d for d in first(params, "dates", "").split(",") if d])
        return 200, [] if empty else wallet_balances()

    raise ApiError(404, "not_found", "No such endpoint")


def info():
    today = dt.date.today()
    settings = STORE["app_settings"][0] if STORE["app_settings"] else {}
    day = settings.get("cycle_start_day", 26)
    months_back = 0 if today.day >= day else 1
    first = (today.replace(day=1) - dt.timedelta(days=1)).replace(day=1) if months_back else today.replace(day=1)
    start = first.replace(day=day)
    nxt = (start.replace(day=1) + dt.timedelta(days=32)).replace(day=1)
    end = nxt.replace(day=day) - dt.timedelta(days=1)
    fmt = lambda d: f"{d.day} {d.strftime('%b')}"
    return {"today": today.isoformat(), "cycle_start_day": day, "cycle_from": start.isoformat(),
            "cycle_to": end.isoformat(), "cycle_range": f"{fmt(start)} - {fmt(end)}",
            "accounts": len(STORE["accounts"]), "transactions": len(STORE["transactions"])}


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def send_json(self, code, payload=None):
        body = b"" if payload is None else json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def read_body(self):
        n = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(n).decode() if n else ""
        try:
            return json.loads(raw) if raw else None
        except ValueError:
            return None

    def route(self, method):
        url = urlparse(self.path)
        params = parse_qsl(url.query, keep_blank_values=True)
        body = self.read_body() if method in ("POST", "PATCH", "PUT") else None
        if method == "DELETE":
            self.read_body()
        if url.path.startswith("/__"):
            return self.control(method, url, params, body)
        # The health check of the real server. Setup calls it when it tests the connection.
        if url.path == "/health" and method == "GET":
            return self.send_json(200, {"status": "ok"})
        parts = [p for p in url.path.split("/") if p]
        if len(parts) < 2 or parts[0] != "v1":
            return self.send_json(404, {"error": {"code": "not_found", "message": "No such endpoint"}})
        legacy, table, query = classify(method, parts, params)
        shown = body if body is not None else ""
        log(f"{legacy} {table}?{query} {json.dumps(shown) if shown != '' else ''}".rstrip())
        fault = FAULTS.get(f"{legacy} {table}") or FAULTS.get(table) or FAULTS.get("*")
        mode = fault["mode"] if fault else None
        if mode == "500":
            return self.send_json(500, {"error": {"code": "internal", "message": "fault: internal error"}})
        if mode == "401":
            return self.send_json(401, {"error": {"code": "unauthorized", "message": "fault: unauthorized"}})
        if mode == "slow":
            time.sleep(fault.get("ms", 4000) / 1000.0)
        try:
            with LOCK:
                status, payload = handle(method, parts, params, body, mode == "empty")
        except ApiError as err:
            return self.send_json(err.status, {"error": {"code": err.code, "message": err.message}})
        except (KeyError, TypeError, AttributeError):
            return self.send_json(400, {"error": {"code": "invalid_request", "message": "bad request"}})
        self.send_json(status, payload)

    def control(self, method, url, params, body):
        path = url.path
        if path == "/__health":
            return self.send_json(200, {"ok": True})
        if path == "/__reset" and method == "POST":
            reset()
            return self.send_json(200, {"ok": True})
        if path.startswith("/__seed/") and method == "POST":
            name = path.rsplit("/", 1)[-1]
            try:
                reset()
                load(name)
            except KeyError:
                return self.send_json(404, {"message": f"unknown seed {name}", "seeds": seeds.SEED_NAMES})
            log(f"SEED {name}")
            return self.send_json(200, {"ok": True, "seed": name})
        if path == "/__info":
            return self.send_json(200, info())
        if path.startswith("/__state/"):
            table = path.rsplit("/", 1)[-1]
            with LOCK:
                return self.send_json(200, wallet_balances() if table == "wallet_balances" else STORE.get(table, []))
        if path == "/__log":
            with LOCK:
                lines = list(REQUESTS)
                if dict(params).get("clear") == "1":
                    REQUESTS.clear()
            return self.send_json(200, lines)
        if path == "/__fault/clear" and method == "POST":
            FAULTS.clear()
            return self.send_json(200, {"ok": True})
        if path == "/__fault" and method == "POST" and isinstance(body, dict):
            route, mode = body.get("route", "*"), str(body.get("mode", "off"))
            if mode == "off":
                FAULTS.pop(route, None)
            elif mode in ("500", "401", "slow", "empty"):
                FAULTS[route] = {"mode": mode, "ms": int(body.get("ms", 4000))}
            else:
                return self.send_json(400, {"message": "mode must be 500, 401, slow, empty or off"})
            log(f"FAULT {route} {mode}")
            return self.send_json(200, {"ok": True, "faults": FAULTS})
        if path.startswith("/__run/") and method == "POST":
            name = path.rsplit("/", 1)[-1]
            args = body.get("args", []) if isinstance(body, dict) else []
            if name not in RUNNABLE or not all(isinstance(a, str) and len(a) < 400 for a in args):
                return self.send_json(400, {"message": "script not allowed"})
            log(f"RUN {name} {json.dumps(args)}")
            env = dict(os.environ, ANDROID_SERIAL=os.environ.get("ANDROID_SERIAL", "emulator-5554"))
            done = subprocess.run(["bash", os.path.join(SCRIPT_DIR, name + ".sh")] + args, env=env,
                                  capture_output=True, text=True, timeout=90)
            return self.send_json(200 if done.returncode == 0 else 500,
                                  {"code": done.returncode, "out": done.stdout[-500:], "err": done.stderr[-500:]})
        self.send_json(404, {"message": "unknown control route"})

    def do_GET(self):
        self.route("GET")

    def do_POST(self):
        self.route("POST")

    def do_PATCH(self):
        self.route("PATCH")

    def do_PUT(self):
        self.route("PUT")

    def do_DELETE(self):
        self.route("DELETE")


def main():
    global LOG_FILE
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8765)
    ap.add_argument("--log", default="")
    args = ap.parse_args()
    if args.log:
        LOG_FILE = open(args.log, "a")
    ThreadingHTTPServer.daemon_threads = True
    ThreadingHTTPServer(("127.0.0.1", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
