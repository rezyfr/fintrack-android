# FinTrack: Feature Factory

> Read this file at the start of every session before taking any action.

## What This Repo Is

`fintrack-android` is both the application and its governance harness: the Android app lives under `app/`, while `governance/` holds the gates, conventions, and contracts that define what valid output looks like. The architecture pyramid is:

```
ViewModel  ->  UseCase  ->  Repository  ->  DataSource  ->  Dao / Service
 (ui/)        (domain/    (data/         (data/         (data/db/,
               usecase/)   repository/)   datasource/)   sheets/, ...)
```

Every line of code in `app/` must trace back to `tasks/task.json` and respect the gates in `governance/`.

## Mandatory Session Start

```bash
bash governance/session_resume.sh
```

This prints the last checkpoint, current task brief, task-drift check, last 5 gate runs, and `git status`. Run it before reading anything else.

## Approach

- Read existing files before writing. Don't re-read unless changed.
- Thorough in reasoning, concise in output.
- Skip files over 100KB unless required.
- No sycophantic openers or closing fluff.
- No emojis or em-dashes.
- Do not guess APIs, versions, flags, commit SHAs, or package names. Verify by reading code or docs before asserting.

## Gate Commands

```bash
bash governance/check_all.sh                              # run every gate
python3 governance/check_<name>.py                        # run a single gate

LOOP_RESET=yes bash governance/gate_retry_loop.sh         # start a new 3-strike cycle
bash governance/gate_retry_loop.sh                        # continue cycle
```

Every `check_all.sh` run appends a one-line audit entry to `governance/gate-log.jsonl`. Gates exit 0 on pass, 1 on fail; the retry loop additionally returns 2 on UNRESOLVED.

## Non-Negotiable Rules

| Rule                                                                    | Gate    |
| ----------------------------------------------------------------------- | ------- |
| `task.json` must target a story whose capability is Approved/In Progress | G0      |
| UI layer must not touch Supabase or HTTP infrastructure                 | G1      |
| Web UI layer must route Supabase calls through `web/src/api/`            | G1-W    |
| No hardcoded `Color(...)` literals outside `ui/theme/`                  | G3      |
| No hardcoded user-facing strings inside `Text(...)` composables         | G-STR   |
| Layered architecture + SOLID dependency-inversion (R1-R9)               | G-ARCH  |
| Room `version` bumps come with a registered, unbroken migration chain   | G-DB    |
| Manifest graph (capabilities/stories/flows) has no broken links/orphans | G7      |
| Flow `actor==app` steps declare navigation; `triggerElement` resolves   | G16     |
| Every story.acceptance[] line is covered by a `// ac: <id>` annotation  | G31     |
| Manifest IDs are kebab-case + filename; `task.feature` is snake_case of capability | G-NAME |
| `product/registry.md` is in sync with the manifest (regenerate, don't hand-edit) | G-REG  |
| Android module must compile (`./gradlew :app:compileDebugKotlin`)        | G-BUILD |

Full rule book with pass / fail examples: `governance/docs/conventions.md`.

## Adding a New Feature

1. Write or update `tasks/task.json` with `feature`, `storyId`, and `acceptanceCriteria`.
2. Implement under the existing pyramid layers; obey the gates as you write, not after.
3. Drive `bash governance/gate_retry_loop.sh` until green.
4. Append session-end entries with `bash governance/worklog.sh task` and `bash governance/worklog.sh retro <attempts> <escalations> "<notes>"`.
5. Open the PR. Reviewers read the result, not the steps.

## Pausing Mid-Feature

```bash
bash governance/session_checkpoint.sh "where I left off"
```

Saves `governance/.session-checkpoint.json` (gitignored). Pick up with `session_resume.sh` next session.

## Worklog Rule

Every session must append BOTH a `type=task` and a `type=retrospective` entry to `governance/worklog.jsonl`. The worklog is the cross-session audit trail and is committed to git. Use `governance/worklog.sh` rather than hand-editing.

## Recurring Data-Import Scripts

These are one-off data operations against the live Supabase database, not app/web behavior changes — the governance harness (task.json / gates) does not apply. Treat them as any other write to shared state: confirm scope with the user, never blind-force past a dedup warning.

- **Parse a bank statement PDF (BCA Credit Card, BCA savings/checking, Mandiri consolidated savings, Mandiri Credit Card, or Bangkok Bank savings)**: `node scripts/parse_bank_statement.mjs <pdf...>` lists parsed rows with no writes. Add `--insert` to submit to Supabase (aborts if it finds existing rows on the same wallet(s) already in the parsed date range — a prior manual lump-sum entry can double-count against a statement's own line items, as happened for the May 2026 BCA CC payment and, in a different shape, the May 2026 Mandiri CC payment). Add `--force` only after confirming there's no real overlap — a date-range hit is often just the previous statement's tail; check item/amount before forcing. Add `--skip-payments` when the card payment is already in the DB as one manual lump-sum row covering every card on the statement (confirmed 2026-09 for the Aug/Sep BCA CC statements: existing "Payment Credit BCA" rows of 414,427 / 1,404,511 / 4,523,704 each equal that statement date's own per-card PEMBAYARAN lines summed) — it drops every `*_CC` payment transfer and inserts the expense rows only. Auto-detects `BCA_CC` / `BCA_SAVINGS` / `MANDIRI_SAVINGS` / `MANDIRI_CC` / `BBL` per file (tries no password first, falls back to the BCA CC password) — pass a mix of formats in one run. Reuses `parseBcaCc`/`parseBcaSavings`/`parseMandiriCc`/`parseBbl`/`detectFormat` from `web/src/utils/pdfParser.js`, so it stays in sync with the web Import PDF UI. The BCA CC PDF password must be supplied via the `BCA_CC_PDF_PASSWORD` env var (the Mandiri consolidated statement uses the same one); BCA savings, Mandiri CC, and BBL statements have no password. Do not hardcode the password anywhere in the repo. Row conventions:
  - BCA_CC: expense rows -> `wallet=BCA_CC, tx_type=expense, category=Other`; "PEMBAYARAN - MYBCA" rows -> `wallet=BCA, to_wallet=BCA_CC, tx_type=transfer, category=Transfer`. A charge followed same-day by a "REVERSAL CICILAN BCA <merchant>" credit of the identical amount is BCA converting the purchase to an installment plan — both rows are dropped (net zero), since the debt bills as separate "CICILAN BCA KE nn DARI nn" rows; verified against the statement's own SUBTOTAL TRANSAKSI, which counts the pair as zero. Note BCA CC statements abbreviate August as **AGU**, not AGT (both are mapped; before 2026-09 the missing alias silently dropped every August-dated row).
  - BCA_SAVINGS: "KARTU KREDIT/PL ... BCA CARD" rows are **skipped** (same CC payment already recorded from the BCA_CC side — confirmed by amounts summing exactly); "...TRANSFER KE 008 FIDRIYANTO..." rows -> `wallet=BCA, to_wallet=MANDIRI, tx_type=transfer, category=Transfer` — this is a genuine BCA->MANDIRI savings transfer, a separate hop from the Mandiri CC payment below, even when dates/amounts line up with a MANDIRI_CC "PAYMENT THANK YOU" row (confirmed 2026-07: the user funds Mandiri CC payments FROM the Mandiri savings account, not directly from BCA). The paired "BIAYA TXN KE 008" fee line stays a plain expense; everything else -> `wallet=BCA, tx_type=income/expense, category=Other` (recategorize afterward in the web app). **Before inserting any statement credit, check it against existing BBL->BCA transfers**: those are recorded from the BBL side in THB with the IDR in `to_amount`, and the reconcile's (date, amount, direction) key cannot match them when the two sides group the money differently — confirmed 2026-08, where rows 1667 + 1668 (2,500,000 + 500,000) are the statement's single 3,000,000 credit on 01/08, and row 1803's 25,918,161 is the statement's two credits of 11,800,000 + 14,118,161 on 26/08. Both slipped through a `--reconcile --insert` and had to be deleted by hand; the statement's own MUTASI CR is the check that catches it.
  - MANDIRI_SAVINGS (the "Rekening Koran / Statement of Account" consolidated PDF, whose savings section is the only part imported — its credit-card summary is a total, not line items): credit rows -> `wallet=MANDIRI, tx_type=income`, debit rows -> `tx_type=expense`, `category=Other`. A row whose whole detail is a bare 16-digit card number ("-4259456201423591") is a card payment -> `wallet=MANDIRI, to_wallet=MANDIRI_CC, tx_type=transfer, category=Transfer`, the same shape the MANDIRI_CC statement's "PAYMENT THANK YOU - Livin" rows take, so `--reconcile` matches the ones already imported from the card side instead of duplicating them. Credits whose reference contains **CENAIDJA** (BCA's SWIFT code) are **skipped** — that is the receiving leg of a BCA->MANDIRI transfer already recorded from the BCA statement side (confirmed 2026-08: 3,450,000 on 26/08 = DB row 1811, 1,000,000 on 02/08 = row 1681). Row dates are DD/MM only; the year comes from the "Periode / Period : 1/08/26 s/d 31/08/26" line, not the system clock. Use `--reconcile`, not `--insert`: the wallet is already fed by transfers recorded from the BCA, BBL, Investment, and Mandiri CC sides.
  - MANDIRI_CC: expense rows (including $0 "BUNGA CICILAN" installment-interest rows) -> `wallet=MANDIRI_CC, tx_type=expense, category=Other`; "PAYMENT THANK YOU - Livin" rows -> `wallet=MANDIRI, to_wallet=MANDIRI_CC, tx_type=transfer, category=Transfer`; other CR-marked rows (merchant credits/refunds, not payments) -> `wallet=MANDIRI_CC, tx_type=income, category=Other`. A charge that Mandiri immediately reverses same-day via a "Convert to IPP <merchant>" credit of the identical amount is converting the purchase to an installment plan — both the charge and its reversal are dropped (net zero), since the real debt bills later as separate `NNN/NNN` installment rows; counting the original charge too would double it.
  - BBL: no explicit debit/credit marker in the statement text — direction is inferred from whether the running balance rose or fell between consecutive rows (seeded from the statement's own "B/F" opening-balance line). A row named exactly "SALARY" gets `category=Salary`; everything else is `category=Other`. **BBL is also populated live via the Android notification listener**, so most of a given month is typically already recorded (with friendlier item text like "BTS" or "WD Vira" instead of the statement's generic "PMT FOR GOODS")  — use `--reconcile` (optionally with `--insert`) instead of plain `--insert`, which diffs parsed rows against the DB by (date, amount, direction) and only touches the gap. The DB side of that diff includes transfers recorded from the OTHER wallet's side (`to_wallet` matches too, compared on `to_amount` when the transfer converts currency — confirmed 2026-08: a 5,500 THB BBL transfer arrived as the Mandiri statement's 2,944,700 IDR credit, DB row 1719), and direction is part of the key because an amount-only key pairs a credit with a same-day, same-size debit (02/08 on the Mandiri statement: a 1,000,000 credit from BCA and a 1,000,000 card payment out). Confirmed 2026-07: the notification listener does not capture incoming-transfer or salary notifications at all, so `--reconcile`'s findings are almost always income rows ("TRF FR OTH BK", "SALARY") that are otherwise invisible; it also reports (never deletes) any DB rows with no matching statement line, which may be real notification-capture duplicates worth a manual look.
- Older statement imports (`scripts/import_bca_cc.py`, `scripts/reimport_mandiri_jan2026.py`) were hand-transcribed one-off scripts, not reusable — don't run them again as-is.

## Sub-File Load Map

| Task                                       | Load first                                      |
| ------------------------------------------ | ----------------------------------------------- |
| Understand a gate or add a new one         | `governance/docs/conventions.md`                |
| Understand the harness pattern itself      | `factory-blueprint.md`                          |
| Wire up a new feature end-to-end           | `tasks/task.json`, then this file               |
| Audit what happened last session           | `governance/worklog.jsonl`, `governance/gate-log.jsonl` |
| Resume after an interruption               | `bash governance/session_resume.sh`             |
| Parse/import a bank statement PDF (BCA CC/savings, Mandiri CC) | `scripts/parse_bank_statement.mjs` (see Recurring Data-Import Scripts above) |
