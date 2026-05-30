#!/usr/bin/env python3
"""
One-time migration: read all rows from Google Sheets, POST to Supabase.

Requirements:
    pip install google-auth google-auth-httplib2 google-api-python-client requests

Environment variables (required):
    SPREADSHEET_ID     - Google Sheets spreadsheet ID
    SUPABASE_URL       - e.g. https://abcdef.supabase.co
    SUPABASE_ANON_KEY  - anon key from Supabase project settings

Arguments:
    --service-account  Path to Google service account JSON key file

Usage:
    SPREADSHEET_ID=xxx SUPABASE_URL=https://xxx.supabase.co SUPABASE_ANON_KEY=xxx \
        python3 migrate_sheets_to_supabase.py --service-account service_account.json
"""

import argparse
import json
import os
import sys
import datetime
import requests
from google.oauth2 import service_account
from googleapiclient.discovery import build

SPREADSHEET_ID = os.environ.get("SPREADSHEET_ID", "")

TABS = [
    {"name": "Expenses",     "tab": "EXPENSES"},
    {"name": "IDR Expenses", "tab": "IDR_EXPENSES"},
    {"name": "Income",       "tab": "INCOME"},
    {"name": "IDR Income",   "tab": "IDR_INCOME"},
]

SCOPES = ["https://www.googleapis.com/auth/spreadsheets.readonly"]


def parse_date(raw: str) -> str:
    """Parse M/D/YYYY or D/M/YYYY into ISO 8601 YYYY-MM-DD."""
    raw = raw.strip()
    parts = raw.split("/")
    if len(parts) == 3:
        a, b, y = int(parts[0]), int(parts[1]), int(parts[2])
        # Try M/D/YYYY first (sheet format observed), fall back to D/M/YYYY
        for m, d in [(a, b), (b, a)]:
            try:
                return datetime.date(y, m, d).isoformat()
            except ValueError:
                continue
    raise ValueError(f"Cannot parse date: {raw!r}")


def fetch_tab(service, tab_name: str) -> list:
    result = service.spreadsheets().values().get(
        spreadsheetId=SPREADSHEET_ID,
        range=f"{tab_name}!A:E",
        valueRenderOption="FORMATTED_VALUE"
    ).execute()
    return result.get("values", [])


def rows_to_transactions(rows: list, tab: str) -> list:
    transactions = []
    for row in rows[1:]:  # skip header row
        try:
            if tab in ("EXPENSES", "IDR_EXPENSES"):
                if len(row) < 3:
                    continue
                date_raw = row[0]
                item = row[1]
                amount_raw = row[2]
                category = row[3] if len(row) > 3 else "Other"
            else:  # INCOME, IDR_INCOME — columns: empty, date, item, empty, amount
                if len(row) < 5:
                    continue
                date_raw = row[1]
                item = row[2]
                amount_raw = row[4]
                category = "Income"

            amount = float(str(amount_raw).replace(",", ""))
            transactions.append({
                "tab": tab,
                "date": parse_date(date_raw),
                "merchant": item,
                "item": item,
                "amount": amount,
                "category": category,
                "channel": "Unknown",
            })
        except (ValueError, IndexError) as e:
            print(f"  Skipping row {row!r}: {e}")
    return transactions


def post_batch(supabase_url: str, api_key: str, transactions: list) -> int:
    if not transactions:
        return 0
    resp = requests.post(
        f"{supabase_url}/rest/v1/transactions",
        headers={
            "apikey": api_key,
            "Content-Type": "application/json",
            "Prefer": "return=minimal",
        },
        data=json.dumps(transactions),
        timeout=30,
    )
    if not resp.ok:
        print(f"  ERROR HTTP {resp.status_code}: {resp.text}")
        return 0
    return len(transactions)


def main():
    parser = argparse.ArgumentParser(description="Migrate Google Sheets data to Supabase")
    parser.add_argument("--service-account", required=True, help="Path to service account JSON key file")
    args = parser.parse_args()

    if not SPREADSHEET_ID:
        sys.exit("Error: SPREADSHEET_ID environment variable is required")

    supabase_url = os.environ.get("SUPABASE_URL", "").rstrip("/")
    api_key = os.environ.get("SUPABASE_ANON_KEY", "")
    if not supabase_url or not api_key:
        sys.exit("Error: SUPABASE_URL and SUPABASE_ANON_KEY environment variables are required")

    creds = service_account.Credentials.from_service_account_file(args.service_account, scopes=SCOPES)
    service = build("sheets", "v4", credentials=creds)

    total = 0
    for tab_info in TABS:
        tab_name = tab_info["name"]
        print(f"Fetching tab: {tab_name}")
        rows = fetch_tab(service, tab_name)
        transactions = rows_to_transactions(rows, tab_info["tab"])
        print(f"  Parsed {len(transactions)} rows")
        inserted = post_batch(supabase_url, api_key, transactions)
        print(f"  Inserted {inserted}")
        total += inserted

    print(f"\nDone. Total inserted: {total}")


if __name__ == "__main__":
    main()
