"""Seed sets for the fake FinTrack server.

Every date is relative to today, so the Today, Yesterday, Last 7 days and Older groups
always exist. Each build function returns a dict of table name to a list of rows.
"""
import datetime as dt
import random

DEFAULT_CATEGORIES = [
    ("Bills", ["Rent", "Electricity", "Water", "Internet", "Mobile / telco", "Insurance"]),
    ("Subscriptions", ["Streaming", "Music", "Software / cloud"]),
    ("Entertainment", ["Movies", "Games", "Events", "Hobbies"]),
    ("Food & Drink", ["Restaurant", "Cafe / coffee", "Food delivery", "Snacks"]),
    ("Groceries", []),
    ("Health & Wellbeing", ["Gym / fitness", "Pharmacy", "Doctor / medical", "Sports gear"]),
    ("Family", ["Household"]),
    ("Other", []),
    ("Shopping", ["Clothing", "Electronics", "Home", "Personal care"]),
    ("Transport", ["Ride-hailing", "Fuel", "Toll", "E-money"]),
    ("Travel", ["Flights", "Hotels", "Local transport", "Activities"]),
    ("Business", ["Supplies", "Services", "Fees"]),
    ("Gifts", ["Family", "Friends", "Charity"]),
]

SEED_NAMES = [
    "empty", "accounts_only", "base", "one_currency", "three_currency", "cycle_1", "cycle_28",
    "five_currency", "cards_states", "over_budget", "low_balance", "large_list", "no_cards", "no_installments",
    "base_with_rules", "suggest_rules", "showcase",
]


def empty_store():
    return {
        "accounts": [], "categories": [], "subcategories": [], "transactions": [],
        "app_settings": [], "budget_lines": [], "card_billing": [], "installments": [], "rules_doc": [],
    }


def dynamic_cycle_day(today):
    """A start day that keeps at least 11 days of recent rows inside the current cycle."""
    day = (today - dt.timedelta(days=14)).day
    return day if day <= 28 else 1


def _categories():
    cats, subs = [], []
    for i, (name, children) in enumerate(DEFAULT_CATEGORIES):
        cats.append({"name": name, "sort_order": i, "is_default": True})
        for j, sub in enumerate(children):
            subs.append({"category": name, "name": sub, "sort_order": j})
    for n, s in enumerate(subs, start=1):
        s["id"] = n
    return cats, subs


def _account(id_, name, currency, order, kind="checking"):
    return {"id": id_, "name": name, "currency": currency, "sort_order": order,
            "is_default": False, "type": kind}


def _settings(home, cycle_day, others):
    first = others[0] if others else None
    return {
        "id": 1, "home_code": home, "home_label": home,
        "foreign_code": first["code"] if first else "",
        "foreign_label": first["label"] if first else "",
        "cycle_start_day": cycle_day,
        "fallback_rate": (first.get("rate") or 0.0) if first else 0.0,
        "currencies": others,
    }


def _tx(today, rows, home):
    """rows: (days_ago, wallet, type, item, amount, category, subcategory, to_wallet, to_amount)"""
    out = []
    for n, r in enumerate(rows, start=1):
        days, wallet, kind, item, amount, cat, sub, to_wallet, to_amount = r
        income = kind == "income"
        out.append({
            "id": n, "item": item, "amount": amount, "category": cat,
            "date": (today - dt.timedelta(days=days)).isoformat(),
            "tab": ("IDR_INCOME" if income else "IDR_EXPENSES") if wallet in home else ("INCOME" if income else "EXPENSES"),
            "note": None, "created_at": today.isoformat() + "T08:00:00+00:00",
            "wallet": wallet, "tx_type": kind, "to_wallet": to_wallet, "to_amount": to_amount,
            "subcategory": sub, "budget_line_id": None, "installment_id": None,
        })
    return out


def _card(wallet, cutoff_ago, due_in, pct=10):
    today = dt.date.today()
    return {
        "wallet": wallet,
        "cutoff_day": (today - dt.timedelta(days=cutoff_ago)).day,
        "due_day": (today + dt.timedelta(days=due_in)).day,
        "min_percent": pct, "min_full_installments": False,
    }


def _line(id_, name, kind, currency, target, category, sub=None):
    return {"id": id_, "name": name, "kind": kind, "currency": currency, "target": target,
            "due_day": 5 if kind == "fixed" else None, "match_wallets": None,
            "match_pattern": None, "match_categories": None, "rec_min": None,
            "rec_median": None, "rec_max": None, "rec_cycles": 0, "sort_order": id_,
            "active": True, "category": category, "subcategory": sub}


def accounts_only(today=None):
    cats, subs = _categories()
    s = empty_store()
    s["accounts"] = [_account("MAIN", "Main", "USD", 0), _account("CASH", "Cash", "USD", 1)]
    s["categories"], s["subcategories"] = cats, subs
    s["app_settings"] = [_settings("USD", 1, [])]
    return s


def base(today=None, cycle_day=None, eur=True, jpy=False):
    today = today or dt.date.today()
    cats, subs = _categories()
    s = empty_store()
    accounts = [
        _account("MAIN", "Main", "USD", 0),
        _account("CASH", "Cash", "USD", 1),
        _account("GOLD", "Gold card", "USD", 2, "credit"),
        _account("BLUE", "Blue card", "USD", 3, "credit"),
    ]
    others = []
    if eur:
        accounts.append(_account("EURBANK", "Euro bank", "EUR", 4))
        others.append({"code": "EUR", "label": "EUR", "rate": 1.1})
    if jpy:
        accounts.append(_account("YEN", "Yen wallet", "JPY", 5))
        others.append({"code": "JPY", "label": "JPY", "rate": 0.0067})
    home = {"MAIN", "CASH", "GOLD", "BLUE"}
    rows = [
        (0, "MAIN", "expense", "Morning coffee", 5, "Food & Drink", "Cafe / coffee", None, None),
        (0, "GOLD", "expense", "Grocery run", 60, "Groceries", None, None, None),
        (1, "MAIN", "expense", "Bus ticket", 12, "Transport", "Ride-hailing", None, None),
        (3, "MAIN", "income", "Freelance payment", 500, "Freelance", None, None, None),
        (3, "MAIN", "transfer", "Cash withdrawal", 100, "Transfer", None, "CASH", None),
        (2, "GOLD", "expense", "Online shop order", 120, "Shopping", "Electronics", None, None),
        (2, "MAIN", "expense", "Lunch with team", 25, "Food & Drink", "Restaurant", None, None),
        (8, "MAIN", "expense", "Monthly rent", 800, "Bills", "Rent", None, None),
        (5, "MAIN", "investment", "Index fund", 200, "Investment", None, None, None),
        (16, "MAIN", "expense", "Coffee with friend", 6, "Food & Drink", "Cafe / coffee", None, None),
        (20, "GOLD", "expense", "Phone plan", 15, "Bills", "Mobile / telco", None, None),
        (20, "BLUE", "expense", "Streaming bundle", 40, "Subscriptions", "Streaming", None, None),
        (24, "MAIN", "income", "Salary", 3000, "Salary", None, None, None),
        (30, "MAIN", "expense", "Pharmacy visit", 18, "Health & Wellbeing", "Pharmacy", None, None),
        (40, "GOLD", "expense", "Weekend trip", 150, "Travel", "Hotels", None, None),
        (47, "MAIN", "expense", "Cinema night", 30, "Entertainment", "Movies", None, None),
        (54, "MAIN", "income", "Salary", 3000, "Salary", None, None, None),
        (62, "MAIN", "expense", "Taxi ride", 22, "Transport", "Ride-hailing", None, None),
        (70, "MAIN", "expense", "Monthly rent", 800, "Bills", "Rent", None, None),
        (78, "MAIN", "expense", "Grocery run", 70, "Groceries", None, None, None),
        (84, "MAIN", "income", "Salary", 3000, "Salary", None, None, None),
    ]
    if eur:
        rows += [
            (1, "EURBANK", "expense", "Museum ticket", 20, "Travel", "Activities", None, None),
            (4, "MAIN", "transfer", "Transfer to Euro bank", 200, "Transfer", None, "EURBANK", 180),
            (28, "EURBANK", "income", "Refund from shop", 50, "Other", None, None, None),
        ]
    else:
        rows += [(4, "MAIN", "transfer", "Transfer to Cash", 150, "Transfer", None, "CASH", None)]
    if jpy:
        rows += [
            (2, "YEN", "expense", "Ramen lunch", 1200, "Food & Drink", "Restaurant", None, None),
            (3, "MAIN", "transfer", "Transfer to Yen wallet", 100, "Transfer", None, "YEN", 15000),
        ]
    rows.sort(key=lambda r: r[0])
    s["accounts"] = accounts
    s["categories"], s["subcategories"] = cats, subs
    s["app_settings"] = [_settings("USD", cycle_day or dynamic_cycle_day(today), others)]
    s["transactions"] = _tx(today, rows, home)
    lines = [
        _line(1, "Food", "flex", "USD", 300, "Food & Drink"),
        _line(2, "Transport", "flex", "USD", 100, "Transport"),
        _line(3, "Shopping", "flex", "USD", 150, "Shopping"),
        _line(4, "Rent", "fixed", "USD", 800, "Bills", "Rent"),
    ]
    if eur:
        lines.append(_line(5, "Travel", "flex", "EUR", 200, "Travel"))
    else:
        lines.append(_line(5, "Fun", "flex", "USD", 80, "Entertainment"))
    s["budget_lines"] = lines
    s["card_billing"] = [_card("GOLD", 5, 5), _card("BLUE", 12, 3)]
    s["installments"] = [
        {"id": 1, "merchant": "Laptop plan", "merchant_pattern": "LAPTOP", "wallet": "GOLD",
         "installment_amount": 50, "total_installments": 12, "current_step": 3,
         "due_day": (today + dt.timedelta(days=4)).day, "status": "active", "excluded": False},
        {"id": 2, "merchant": "Phone plan", "merchant_pattern": "PHONE", "wallet": "BLUE",
         "installment_amount": 30, "total_installments": 6, "current_step": 2,
         "due_day": 20, "status": "active", "excluded": False},
        {"id": 3, "merchant": "Old sofa", "merchant_pattern": "SOFA", "wallet": "BLUE",
         "installment_amount": 25, "total_installments": 6, "current_step": 6,
         "due_day": 10, "status": "completed", "excluded": False},
    ]
    return s


def one_currency(today=None):
    return base(today, eur=False)


def three_currency(today=None):
    return base(today, jpy=True)


def cycle_1(today=None):
    return base(today, cycle_day=1)


def cycle_28(today=None):
    return base(today, cycle_day=28)


def five_currency(today=None):
    s = base(today, jpy=True)
    for n, (code, label) in enumerate([("AUD", "AUD"), ("GBP", "GBP")]):
        s["accounts"].append(_account(f"{code}ACC", f"{label} account", code, 10 + n))
        s["app_settings"][0]["currencies"].append({"code": code, "label": label, "rate": 0.7})
    return s


def cards_states(today=None):
    s = base(today)
    s["accounts"].append(_account("SILVER", "Silver card", "USD", 6, "credit"))
    s["transactions"].append({
        "id": 100, "item": "Furniture order", "amount": 90, "category": "Shopping",
        "date": (dt.date.today() - dt.timedelta(days=20)).isoformat(), "tab": "IDR_EXPENSES",
        "note": None, "created_at": dt.date.today().isoformat() + "T08:00:00+00:00",
        "wallet": "SILVER", "tx_type": "expense", "to_wallet": None, "to_amount": None,
        "subcategory": "Home", "budget_line_id": None, "installment_id": None,
    })
    s["card_billing"] = [_card("GOLD", 5, 3), _card("BLUE", 12, 0), _card("SILVER", 12, -2)]
    return s


def over_budget(today=None):
    s = base(today)
    for line in s["budget_lines"]:
        if line["kind"] == "flex" and line["currency"] == "USD":
            line["target"] = {"Food": 5, "Transport": 2, "Shopping": 10}.get(line["name"], line["target"])
        if line["currency"] == "EUR":
            line["target"] = 5
    return s


def low_balance(today=None):
    """Main holds little money, so the budget is limited by the balance."""
    s = base(today)
    s["transactions"] = [t for t in s["transactions"] if not (t["wallet"] == "MAIN" and t["tx_type"] == "income")]
    s["transactions"].append({
        "id": 101, "item": "Small deposit", "amount": 20, "category": "Other",
        "date": (dt.date.today() - dt.timedelta(days=2)).isoformat(), "tab": "IDR_INCOME",
        "note": None, "created_at": dt.date.today().isoformat() + "T08:00:00+00:00",
        "wallet": "CASH", "tx_type": "income", "to_wallet": None, "to_amount": None,
        "subcategory": None, "budget_line_id": None, "installment_id": None,
    })
    return s


def large_list(today=None):
    today = today or dt.date.today()
    s = base(today)
    for n in range(1, 2001):
        s["transactions"].append({
            "id": 1000 + n, "item": f"Bulk row {n}", "amount": 1 + n % 50, "category": "Other",
            "date": (today - dt.timedelta(days=n % 120)).isoformat(), "tab": "IDR_EXPENSES",
            "note": None, "created_at": today.isoformat() + "T08:00:00+00:00",
            "wallet": "MAIN", "tx_type": "expense", "to_wallet": None, "to_amount": None,
            "subcategory": None, "budget_line_id": None, "installment_id": None,
        })
    return s


def no_cards(today=None):
    s = base(today)
    s["card_billing"] = []
    return s


def no_installments(today=None):
    s = base(today)
    s["installments"] = []
    return s


def suggest_rules():
    """Accounts and categories, plus saved transactions with patterns a rule can sort:
    a fare of 65 (some rows already labeled), and a payment of 700 on day 26 of each month."""
    today = dt.date.today()
    s = accounts_only()
    rows = []
    for i in range(6):
        rows.append((3 + i * 5, "MAIN", "expense", "Transfer from your account", 65, "Other", None, None, None))
    for i in range(4):
        rows.append((4 + i * 6, "MAIN", "expense", "Metro fare", 65, "Transport", None, None, None))
    # Other amounts with the same text, so the text alone does not point to one rule.
    for i in range(8):
        rows.append((33 + i * 7, "MAIN", "expense", "Transfer from your account", 31 + i * 13, "Other", None, None, None))
    for months_back in range(1, 5):
        first = today.replace(day=1)
        for _ in range(months_back):
            first = (first - dt.timedelta(days=1)).replace(day=1)
        rows.append(((today - first.replace(day=26)).days, "MAIN", "expense", "Transfer from your account", 700, "Other", None, None, None))
    s["transactions"] = _tx(today, rows, {"MAIN", "CASH"})
    return s


def base_with_rules():
    """The base set, plus a shared rule list that another phone saved. Its edit time is far in the future, so it wins."""
    s = base()
    s["rules_doc"] = [{
        "id": 1, "updated_at_ms": 4102444800000,
        "rules": [{
            "id": "remote-1", "name": "Shared taxi rule", "enabled": True,
            "conditions": {"text": {"mode": "CONTAINS", "value": "grab"}, "accounts": []},
            "action": {"category": "Transport", "ignore": False},
        }],
    }]
    return s


def _showcase_history(today):
    """About 110 days of everyday spending, the same every run, so the budget chart has three past cycles to compare."""
    rng = random.Random(7)
    cafes = ["Morning coffee", "Cafe latte", "Bakery", "Lunch bowl", "Pizza night", "Noodle bar", "Burger lunch"]
    shops = ["Market run", "Corner store", "Weekly groceries", "Fresh produce"]
    rides = ["Bus ticket", "Ride home", "Metro top-up", "Taxi ride"]
    rows = []
    for ago in range(0, 110):
        if rng.random() < 0.55:
            rows.append((ago, "MAIN", "expense", rng.choice(cafes), rng.randint(6, 28), "Food & Drink", None, None, None))
        if ago % 6 == 2:
            rows.append((ago, "MAIN", "expense", rng.choice(shops), rng.randint(35, 80), "Groceries", None, None, None))
        if ago % 4 == 1:
            rows.append((ago, "MAIN", "expense", rng.choice(rides), rng.randint(8, 20), "Transport", None, None, None))
    return rows


def showcase():
    """The base set plus three rules and a long history of everyday spending, for screenshots."""
    s = base()
    today = dt.date.today()
    extra = _tx(today, _showcase_history(today), {"MAIN", "CASH", "GOLD", "BLUE"})
    offset = len(s["transactions"])
    for row in extra:
        row["id"] += offset
    s["transactions"] += extra
    s["rules_doc"] = [{
        "id": 1, "updated_at_ms": 4102444800000,
        "rules": [
            {"id": "r1", "name": "Rides", "enabled": True,
             "conditions": {"text": {"mode": "CONTAINS", "value": "grab"}, "amount": {"min": 20.0, "max": 80.0}, "accounts": []},
             "action": {"category": "Transport", "subcategory": "Ride-hailing", "description": "Ride", "ignore": False}},
            {"id": "r2", "name": "Coffee", "enabled": True,
             "conditions": {"text": {"mode": "CONTAINS", "value": "cafe"}, "accounts": []},
             "action": {"category": "Food & Drink", "subcategory": "Cafe / coffee", "ignore": False}},
            {"id": "r3", "name": "Rent day", "enabled": True,
             "conditions": {"amount": {"min": 800.0, "max": 800.0}, "date": {"day": 8, "weekdays": []}, "accounts": ["MAIN"]},
             "action": {"category": "Bills", "subcategory": "Rent", "ignore": False}},
        ],
    }]
    return s


def build(name):
    if name == "empty":
        return empty_store()
    fn = globals().get(name)
    if name not in SEED_NAMES or fn is None:
        raise KeyError(name)
    return fn()
