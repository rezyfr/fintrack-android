# Maestro end-to-end test cases

Maestro runs YAML flows against the debug APK on an emulator. This list has 80 cases. It does not hold the YAML files.

Priority: P0 runs on every pull request. P1 runs nightly. P2 runs before a release.
Short names: T(x) is `tapOn: "x"`. V(x) is `assertVisible: "x"`. NV(x) is `assertNotVisible: "x"`.
App id: `com.fidriyanto.banktracker`. Strings come from `app/src/main/res/values/strings.xml`.
Tab names are content descriptions: Home, Transactions, Budget, Accounts. The add button is "Add transaction".

This list was made from `master`. Cases marked [PR9] need the on-device AI branch. Cases marked [PR10] need the first-run setup branch. Check the strings again after the merge.

## 1. First-run setup [PR10]

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-001 | P0 | Setup shows on a clean install | Backend has no accounts. launchApp with clearState | V "Welcome to FinTrack", "Let us go". Then "Where should FinTrack keep your data?" with the server chosen, and "Connect your server", where "Test the connection" must pass. Then the pages "What is your home currency?", "When does your month start?", "Add your accounts". No tab bar. |
| E2E-002 | P0 | One currency | Home currency USD. Add account Cash in USD. T "Continue" until "You are all set", then T "Open FinTrack" | Home opens. No book switch. |
| E2E-003 | P0 | Three currencies | Home USD. Add Main (USD), Euro bank (EUR), Yen wallet (JPY). Type a rate in "Rate: 1 EUR in USD". Continue, then T "Open FinTrack" | Book switch shows Combined, USD, EUR, JPY. A rate field shows for EUR and JPY only. |
| E2E-004 | P0 | At least one account | Fill the currency. Add no account | "Continue" stays disabled on the accounts page. |
| E2E-005 | P1 | Save error | Fake server returns 500 on POST accounts | V "The setup could not be saved. Check your connection and try again." |
| E2E-006 | P1 | Setup is not shown twice | After E2E-002, stopApp, launchApp | Home opens at once. |
| E2E-007 | P2 | Bad currency code | Enter "US" or "12$" | Continue stays disabled on the currency page. |
| E2E-008 | P1 | Pay cycle day | On the cycle page pick day 1, then day 28 | The Home range starts on that day. |
| E2E-009 | P0 | Offline setup | Choose "On this phone". Finish setup. Add an expense | Home opens. The expense shows in Transactions. The fake server gets no POST accounts and no POST transactions. |

## 2. Add, edit and delete

Precondition: seed set `base`, Combined book, Home tab.

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-010 | P0 | Add expense | T "Add transaction". Amount 12500. Pick Food. Description Lunch. T "Save" | Sheet closes. "Lunch" shows under Today. |
| E2E-011 | P0 | Add income | T "Income". Amount 1000000. Description Salary | Row visible. Money in grows. |
| E2E-012 | P0 | Transfer, same currency | T "Transfer". From and To in IDR. Amount 50000 | Row visible. Net total unchanged. |
| E2E-013 | P1 | Transfer, two currencies | From an IDR wallet, To a THB wallet. V "Received (THB)". Fill both amounts | Saved. The row shows in both books. |
| E2E-014 | P0 | Add investment | T "Investment". Amount and description | Row visible. |
| E2E-015 | P0 | Edit | Tap a row. Change the amount. T "Save changes" | Row shows the new amount. The server got a PATCH. |
| E2E-016 | P0 | Delete from the edit sheet | Tap a row. T "Delete". Confirm | Row gone. |
| E2E-017 | P0 | Delete by swipe | Swipe left. Dialog "Delete transaction?". T "Delete", then repeat with "Cancel" | Row gone, then row stays. |
| E2E-018 | P1 | Delete fails | Fake server 500 on DELETE | V "Failed to delete transaction. Please try again." with "Retry". |
| E2E-019 | P1 | Edit fails | Fake server 500 on PATCH | V "Failed to update transaction. Please try again." |
| E2E-020 | P1 | Batch delete | Long press a row. Select two. T "Delete". Confirm | Both rows gone. |
| E2E-021 | P1 | Batch change category | Select rows. Type a category. T "Apply" | Rows show the new category. |
| E2E-022 | P1 | Change the date | T "Date". Pick yesterday | Row under Yesterday. |
| E2E-023 | P1 | Budget row | Add an expense in a category with a budget line | The form shows the budget. The row has no "No budget" mark. |
| E2E-024 | P2 | Link a card transaction to an installment | Card expense, "Link to installment" | The edit sheet shows the link. "Unlink" removes it. |
| E2E-025 | P1 | Close without saving | Fill the amount. T "Close" | No row added. |
| E2E-026 | P2 | Confirm a captured row | Skipped. A captured row is saved as synced at once, so no Pending row exists. | See MANUAL.md. |
| E2E-027 | P1 | Wallet required | Seed `accounts_only`. Delete every account in Manage Accounts. Open the form. Save | V "Choose a wallet." |

## 3. Books

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-030 | P0 | Switch books | Home. T "Combined", then each currency book | Every tab follows the book. A transfer between two currencies shows in both books. |
| E2E-031 | P0 | App opens on Combined | stopApp, launchApp | Combined is selected. |
| E2E-032 | P1 | Book filters the wallet list | Select a book. Open Add | Only wallets of that currency. |
| E2E-033 | P1 | Book stays across tabs | Select a book. Visit all tabs | Same book. |
| E2E-034 | P1 | Three currencies | Seed USD, EUR, JPY accounts | One book per currency. Combined converts each with its own rate. |
| E2E-035 | P1 | Book switch scrolls | Seed five currencies on a narrow screen | Every chip is reachable. |
| E2E-036 | P1 | A removed currency | Delete the last account of a currency while its book is selected | The app falls back to Combined. |

## 4. Home

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-040 | P0 | Overview | launchApp with `base` | V "Welcome back", the cycle range, "Money in", "Money out", "Recent activity". |
| E2E-041 | P1 | Payday text | Seed the date | "Payday in N days" or "Payday today". |
| E2E-042 | P1 | Budget link | scrollUntilVisible "Budget". T "See all budgets" | Budget tab opens. |
| E2E-043 | P1 | Cards due | Seed a card due in 5 days | V "Cards due", "Due in 5d" (a text such as "Due in 5d", the number depends on the seed). |
| E2E-044 | P1 | Spend compare | scrollUntilVisible "Spending vs last cycle" | V "This cycle", "Last cycle". |
| E2E-045 | P1 | Empty cycle | No transactions | V "No activity yet this cycle". |
| E2E-046 | P1 | Combined totals | Seed `three_currency` | Money in and out equal the hand calculation with each rate. |

## 5. Transactions list

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-050 | P0 | Groups | T "Transactions". The list opens on the last 7 days. T "Filters", "Reset", "Apply" to see older rows | Headers Today, Yesterday, Last 7 days, Older. |
| E2E-051 | P1 | Calendar | T "Calendar" | The grid shows. T "List" returns. |
| E2E-052 | P0 | Search and Apply | T "Filters". Search coffee. T "Apply" | Only matching rows from the last 7 days. The chip reads "Filters (2)", because the 7 day range counts as one filter. |
| E2E-053 | P0 | Reset | T "Filters". T "Reset". T "Apply" | All rows return, also older ones. The chip reads "Filters". |
| E2E-054 | P0 | Type filter | T each type, then Apply | Only that type. |
| E2E-055 | P1 | Wallet filter | Pick a wallet. Apply | Only that wallet. |
| E2E-056 | P1 | Category and subcategory | Pick both. Apply | Matching rows. |
| E2E-057 | P1 | No budget | T "No budget". Apply | Only rows with the mark. |
| E2E-058 | P1 | Amount range | T "Advanced". Min 10000, Max 50000. Apply | Rows inside the range. |
| E2E-059 | P1 | Date range | Pick From and To dates | Rows inside the range. |
| E2E-060 | P1 | No match | Search zzzz | V "No transactions found." |
| E2E-061 | P1 | Dismiss without Apply | Type a search. Swipe the sheet down | The list does not change. |

## 6. Budget with the cycle stepper

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-065 | P0 | Summary | T "Budget" tab. The Budget view is the first view | V "Remaining budget", "days left", "You can spend". |
| E2E-066 | P0 | Stepper | T "Previous cycle", then "Next cycle" | Data follows. "Next cycle" is disabled on the current cycle. |
| E2E-067 | P1 | Over budget | Seed spend above the total | V "Over budget by". |
| E2E-068 | P1 | Line detail | T a line | Its transactions show. |
| E2E-069 | P1 | Fixed bills | scrollUntilVisible "Fixed bills" | "Paid" on a paid bill. |
| E2E-070 | P1 | Unbudgeted | scrollUntilVisible "Unbudgeted" | The row shows. |
| E2E-071 | P1 | Empty | A book with no lines | V "No flexible budget lines for this currency". |
| E2E-072 | P1 | Error | Fake server 500 | V "Could not load budget". |
| E2E-073 | P1 | Balance limit | Seed a low balance | V "Limited by your balance of". |
| E2E-074 | P1 | Rates in Combined | Seed `three_currency` with lines in two currencies | The total target converts each line with its currency rate. |

## 7. Insights

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-075 | P0 | Loads | T "Insights" | V "Money in", "Net", "Daily spending". "Spending Breakdown" shows when a book is selected. |
| E2E-076 | P1 | Stepper | T "Previous cycle" | Data and month change. |
| E2E-077 | P1 | Sub-categories | T a category row | Sub-categories show. |
| E2E-078 | P1 | Wallet chips | Select a book. T a wallet chip | Data filtered. Combined has no wallet chips. |
| E2E-079 | P1 | Empty cycle | A cycle with no data | V "No transactions in this cycle." |
| E2E-080 | P1 | Cards | scrollUntilVisible "Largest expenses", "Daily spending", "Last 6 months", "Paid with" | Visible. |
| E2E-081 | P1 | One section per currency | Seed `three_currency`. Select each book | Each book shows only its currency. |

## 8. Cards and installments

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-085 | P0 | Cards | T "Cards" | V "Balance owed", "Minimum payment", "Payment due". |
| E2E-086 | P1 | Due states | Seed due in 3 days, today, overdue | "Due in 3 days", "Due today", "Overdue by 2 days". |
| E2E-087 | P1 | Empty | No card billing rows | V "No card billing records found". "No cards in this book." shows when only the selected book has no card. |
| E2E-088 | P1 | Error | Fake server 500 | V "Could not load cards". |
| E2E-089 | P0 | Installments | T "Installments" | V "Active", "Total monthly". |
| E2E-090 | P1 | Rename | T the edit icon of a plan. New name. T "Save" | Name changes. |
| E2E-091 | P1 | Add a step | T "+1 Step" | The step count grows. |
| E2E-092 | P1 | Exclude | T the switch beside "Include" | The label flips to "Excluded". The total changes. |
| E2E-093 | P1 | Empty | No plans | V "No active installments". |

## 9. Accounts and Settings

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-100 | P0 | Balances | T "Accounts" | One net tile for each currency. Cards show "owed". |
| E2E-101 | P0 | Open and leave Settings | T "Menu". T "Back" | Returns to balances. |
| E2E-102 | P0 | Add account | Manage Accounts. T "Add Account". Name, Currency, a type. Save | The row shows the currency and type. It appears in the Add form. |
| E2E-103 | P1 | Rename | T the edit icon. New name. Save | Name changes. The list order stays. |
| E2E-104 | P1 | Delete | T the delete icon | Row gone. |
| E2E-105 | P1 | Categories | Manage Categories. Add a category and a subcategory | They show in the Add form. |
| E2E-106 | P0 | Currency settings | Change the home book name. Change the cycle day. Set a book name and a rate for another currency. Save | The switch shows the names. The cycle range follows. |
| E2E-107 | P1 | Save is blocked | Cycle day 0 or 29 | "Save" is disabled. |
| E2E-108 | P1 | Dark theme | Toggle "Dark theme" | The screen changes. It survives a restart. |
| E2E-109 | P1 | Listener status | Access off, then on | "Inactive", then "Listening". See FINDINGS F-004. |
| E2E-110 | P1 | Retry pending syncs | Make a row fail (server 500). Server up. T "Retry Pending Syncs" | The Failed badge goes away. See FINDINGS F-006: the button is not reachable. |
| E2E-111 | P1 | Bank notifications [PR10] | Settings, "Bank notifications". Pick an account for BCA. Pick Off for the other bank | The choices stay after a restart. |

## 10. On-device AI [PR9]

The 2.6 GB model is not available in CI. Test the download state only. Use a stub file or a fake engine for the rest.

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-115 | P0 | Not installed | Settings, "On-device AI" | V "Download model (2.6 GB)". Do not tap it in CI. |
| E2E-116 | P0 | Ask needs the model | T "Ask about your spending" | V "Download the model once to ask questions about your spending." |
| E2E-117 | P1 | Download progress | A fake server serves a small file | V the percent text. |
| E2E-118 | P1 | Download fails | Fake server 500 | V "Download stopped", T "Try again". |
| E2E-119 | P1 | Installed | `adb push` a stub to the model path | V "Installed", T "Delete model" returns to the download state. |
| E2E-120 | P1 | No chip without a model | Type "Coffee" in the Add form | NV "Suggested:". |
| E2E-121 | P2 | Chip with a fake engine | Test build with a fake engine | V "Suggested: Food & Drink". Tap fills the category. |
| E2E-122 | P2 | Ask with a fake engine | T an example question | The answer and "Based on:" show. |

## 11. Notification capture

Maestro cannot post a notification. Run adb from the CI script between two flows.
Allow the listener once: `adb shell cmd notification allow_listener com.fidriyanto.banktracker/com.fidriyanto.banktracker.service.BankNotificationService`.
Post: `adb shell cmd notification post -S bigtext -t 'myBCA' tag1 'You spent IDR 45,000 at Food and Beverage'`.
The app ignores its own notifications. The shell package is not the app, so the post is read.

| ID | Pri | Title | Notification | Assert |
|---|---|---|---|---|
| E2E-125 | P0 | BCA expense | title myBCA, text `You spent IDR 45,000 at Food and Beverage` | A review notification shows. The Transactions tab has the row in the account mapped to BCA. |
| E2E-126 | P0 | BCA income | `You received IDR 1,500,000 from ACME at Salary` | Income row "ACME". |
| E2E-127 | P1 | Bangkok Bank | title Bangkok Bank, text `Cafe Nero 120.50THB` | Row "Cafe Nero". |
| E2E-128 | P1 | Ignored | title Weather, text `Rain today` | No new row. |
| E2E-129 | P1 | No amount | title myBCA, text `Your OTP is 123` | No new row. |
| E2E-130 | P1 | Listener off | Disallow the listener, post | No row. |
| E2E-131 | P1 | Bank turned off [PR10] | Set BCA to Off. Post a BCA text | No pattern row. The AI path runs when it is on. |
| E2E-132 | P2 | Duplicate | Post the same text twice | Record the result. |
| E2E-133 | P2 | AI path [PR9] | Enable the setting. Use a fake engine. Post unusual text | A row from the engine. |

## 12. Upgrade and migration

Room is at version 13 on the setup branch.

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-135 | P1 | Upgrade keeps data | `adb install` the old APK. Queue two rows with the server down. `adb install -r` the new APK. launchApp | No crash. The rows sync later. |
| E2E-136 | P2 | Across versions | Install an APK at an older database version, then the new one | Same. Also run the G-DB gate. |
| E2E-137 | P0 | Existing install skips setup [PR10] | Old APK with accounts, then the new APK | Setup does not show. Home keeps its figures and its cycle day. |
| E2E-138 | P1 | Old settings stay | After the upgrade open Currency Settings | Home currency and book names are unchanged. A THB book is named Thailand. |

## 13. Negative and edge cases

| ID | Pri | Title | Steps | Expected |
|---|---|---|---|---|
| E2E-140 | P0 | Amount required | Save with no amount | V "Enter an amount above 0." |
| E2E-141 | P0 | Description required | Amount, no description | V "Add a description." |
| E2E-142 | P1 | Transfer target | Transfer with no To wallet | V "Choose the wallet to transfer to." |
| E2E-143 | P1 | Received amount | Transfer between two currencies with no received amount | V "Enter the amount received." |
| E2E-144 | P1 | Backend down | Stop the fake server. launchApp | Cached data shows. No crash. |
| E2E-145 | P1 | Offline add | Airplane mode on. Add an expense | The form stays open with "Sync failed - saved offline". The row shows "Failed" with Retry. After airplane off and Retry the badge goes away. |
| E2E-146 | P1 | Accounts but no data | Seed accounts only | Every screen shows its empty text. No crash. |
| E2E-147 | P1 | One currency | Seed accounts in one currency | No book switch. Insights and Accounts work. |
| E2E-148 | P1 | Three currencies | Seed USD, EUR, JPY | Four chips. Combined converts with each rate. |
| E2E-149 | P1 | Cycle day 1 | Cycle day 1 | Range "1 Oct - 31 Oct". The stepper moves by month. |
| E2E-150 | P1 | Cycle day 28 | Cycle day 28 in February | Range "28 Jan - 27 Feb". No crash. |
| E2E-151 | P1 | Rotation | Landscape on the Add form | Typed text stays, or document the loss. |
| E2E-152 | P1 | Dark theme | Visit every tab | No crash. Review screenshots. |
| E2E-153 | P2 | Large font | Font scale 1.3 and a long description | Buttons stay tappable. |
| E2E-154 | P2 | Process death | Open the Add sheet. `adb shell am kill`. Relaunch | Home opens. No crash. |
| E2E-155 | P2 | Large list | Seed 2,000 rows | The list scrolls. |

## 14. What Maestro cannot test

| Limit | Why | Instead |
|---|---|---|
| Posting a notification | Maestro has no API for it | adb in the CI script |
| The real model and its answers | 2.6 GB and slow | A stub file and a fake engine. A manual check on a device before a release. |
| System settings (airplane mode, font scale, clock) | System screens differ by image | adb commands in the CI script |
| Server errors and delays | No request control | A fake server with fault switches |
| Charts | A chart is one canvas node | Check labels. The unit tests check the numbers. |
| Pixel layout and colors | No pixel logic | takeScreenshot and a review |
| Keystore, WorkManager timing | Not visible in the UI | Instrumented tests in `src/androidTest` |
| A fake date | The app reads the system clock | `adb shell date`, or a test clock in the code |

## 15. Test data and backend

The app calls the FinTrack server through its REST API. Two options:

1. **A local fake server (pull requests).** The emulator reaches it at `http://10.0.2.2:PORT`. CI writes that URL and a dummy key into `local.properties`, then builds the debug APK. A debug build needs `usesCleartextTraffic` for plain HTTP. I used exactly this setup to test the first-run setup and three currencies by hand.
2. **A dedicated test server with its own test database (nightly and release).** Run the server migrations on an empty database. Reset and seed before each flow. Never use the real server.

| Seed set | Content |
|---|---|
| `empty` | No accounts, no settings. For the first-run setup. |
| `accounts_only` | Accounts and categories, no transactions. |
| `base` | Two currencies, three months of transactions of all types, a transfer between currencies, five budget lines, two card statements, two installments. |
| `one_currency`, `three_currency` | Variants of `base`. |
| `cycle_1`, `cycle_28` | A pay cycle day in `app_settings`. |
| Fault switches | Per route: 500, 401, slow, empty list. |

Seed dates must be relative to today, or the Today and Yesterday groups break. The fake server needs a reset endpoint for the CI script.

## 16. Missing test tags

Enable `testTagsAsResourceId` at the root once, so a `testTag` shows as an id.

| File (under `ui/`) | Composable | Problem | Add |
|---|---|---|---|
| `navigation/AppNavigation.kt` | The add button and the tab items | Hardcoded names. The selected tab text shows twice. | Tags `add_fab`, `tab_home`, `tab_transactions`, `tab_budget`, `tab_accounts`, `sub_budget`, `sub_cards`, `sub_installments`, `sub_insights`. |
| `common/SegmentedToggle.kt`, `CurrencyToggle.kt`, `BookSwitch.kt` | The toggles | No selected state. The labels are user data. | A `selected` semantics and tags `book_combined` and `book_<code>`. |
| `common/CycleStepper.kt` | The stepper | No tag | `cycle_title`, `cycle_prev`, `cycle_next`. |
| `add/TransactionForm.kt` | Amount, description, type, wallet, save | The amount field has only the hint "0". | `tx_amount`, `tx_description`, `tx_type_<name>`, `tx_wallet_row`, `tx_save`, `tx_form_delete`. |
| `feed/FeedScreen.kt`, `TransactionCard.kt` | Filters, search, rows | Hardcoded labels. No id per row. The clear icon has no description. | `feed_filters_button`, `feed_search`, `feed_apply`, `feed_reset`, `tx_row_<id>`, `tx_status_<id>`. |
| `home/HomeScreen.kt` | The hero and the recent rows | No tags | `home_hero`, `home_recent_<id>`. |
| `budgetglance/BudgetGlanceScreen.kt` | The line cards | No tag per line | `budget_line_<id>`. |
| `installments/InstallmentsScreen.kt` | "+1 Step" and exclude | Repeated for each plan | `inst_step_<id>`, `inst_exclude_<id>`. |
| `settings/` | Switches, edit and delete icons | Every row says "Delete". | A description with the row name. Tags `switch_dark`, `currency_save`. |
| `setup/SetupScreen.kt` [PR10] | The fields and the buttons | New | `setup_home_currency`, `setup_account_name`, `setup_add_account`, `setup_finish`. |
| `ask/AskScreen.kt` [PR9] | The input and the send button | New | `ask_input`, `ask_send`, `ai_download`. |

Also move the remaining hardcoded English strings into `strings.xml`, so one set of text works for every flow.

Flows and the runner now live in `.maestro/`. See `.maestro/scripts/run_all.sh`, `.maestro/MANUAL.md` and `.maestro/FINDINGS.md`.

## 17. Folder layout

```
.maestro/
  config.yaml
  env/            local.yaml, ci.yaml
  flows/
    pr/           P0 flows, tag pr
    nightly/      P1 flows
    release/      P2 flows
  subflows/       launch_clean, complete_setup, open_tab, select_book, add_transaction, open_filters
  data/           seed JSON files
  scripts/        seed.sh, reset.sh, post_notification.sh, set_airplane.sh, install_old_apk.sh, fake-server/
  apks/           old release APKs, not committed
```

Name each flow with its id, for example `E2E-010_add_expense.yaml`. Start each flow with `appId` and `tags`.

## 18. Run in CI

1. Build the debug APK with `local.properties` pointing at the fake server.
2. Start an emulator (API 34, x86_64) with animations off.
3. Start the fake server. Run `reset.sh` and `seed.sh base`.
4. Install the APK. Allow the listener and the notification permission.
5. Run `maestro test --include-tags=pr --format junit --output report.xml .maestro/flows`.
6. Upload the report, the screenshots and the logcat, also on failure.

| Trigger | Tags | Notes |
|---|---|---|
| Pull request | pr | Fake server. Reseed before each flow that changes data. |
| Nightly | pr, nightly | Add the upgrade and notification flows. |
| Release | all | Add the test server and the manual checks from section 14. |

Do not share state between flows. Retry a failed flow once and report the flaky ones.

## 19. Open questions

1. Is a debug-only cleartext HTTP setting acceptable for the fake server?
2. Which old release APKs should the upgrade tests keep?
3. Is a duplicate notification (E2E-132) a bug? The deduplication key does not use a reference number.
