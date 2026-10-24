# Custom rules

A rule sets the category, the subcategory and the description of a transaction when the transaction matches the rule.

## Conditions

All conditions are optional, but a rule needs at least one. A rule with no condition matches nothing. A rule matches when all of its conditions match. Detail is the number of conditions.

| Condition | Options |
|---|---|
| Text | contains, starts with, equals (ignores case) |
| Amount | from, to (one side may stay empty) |
| Time of day | from, to in 24 hour time. The range may cross midnight. |
| Date | day or a range of days (for example 2 to 5, or 28 to 3 across the end of the month), month, year (each optional), weekdays |
| Account | one or more accounts |
| Type | expense, income, transfer, investment |

## Actions

| Action | Behavior |
|---|---|
| Category | Required unless the rule ignores. |
| Subcategory | Optional. It belongs to the chosen category. |
| Description | Optional. It may hold `{merchant}` and `{amount}`. |
| Ignore | Drops a captured bank notification. It replaces the other actions. |

## Which rule wins

1. Take every enabled rule that matches.
2. For each field, take the value from the rule with the highest detail.
3. A tie goes to the rule higher in the list.
4. A less detailed rule fills the fields a more detailed rule leaves empty.
5. The Rules page warns about two rules with the same detail that can match the same transaction and set different results.

## Where rules run

| Place | Code |
|---|---|
| Captured bank notification | `BankNotificationService.capture` calls `ApplyRulesUseCase.forCapture` |
| Add form | `AddViewModel.submit` fills the category only when the user left the default one. A typed description stays. |
| Saved transactions | The Saved transactions page (`PastRulesScreen`) on the last step of the editor. It lists the changes, asks for confirmation, applies them and offers Undo. |

## Storage and sync

| Part | Behavior |
|---|---|
| Phone | The rules are one JSON text in the DataStore (`RuleDataSourceImpl`, key `rules_json`). The phone also keeps the time of the last edit and a flag for an edit the server has not received. |
| Server | The whole list is one document on the FinTrack server (`GET` and `PUT` on `/v1/rules`). The server stores it in the table `rules_doc`. |
| Save | The phone saves first, then sends the list. A failed send leaves the edit marked, and the next refresh sends it. |
| Refresh | The app compares the two copies at start and when the Rules page opens. The copy with the later edit wins. |
| First sync | Rules that were made before sync existed are sent once. |
| Two phones | Edits are compared as a whole list. If two phones edit while offline, the later edit replaces the other one. |

## Not built yet

- The web app.
- A date range that spans months, such as 5 Dec to 10 Jan.
- Drag to reorder. The list has arrows.

## Suggested rules

The Rules page has a Suggest rules button. It reads the saved transactions of the last 12 months that are still in Other (`SuggestRulesUseCase`, `RuleMiner`).

| Pattern | What the finder needs |
|---|---|
| Amount | The same amount on the same account at least 4 times |
| Monthly payment | The same amount on the same day of the month, in at least 3 months |
| Text | The same text at least 4 times, with one amount in at least 60 percent of its rows. A text with many different amounts is too vague and is skipped. |

- A transaction that an existing rule already sorts does not count. Transfers do not count.
- When transactions with the same amount already have a category and at least 80 percent agree, that category is chosen for the user. The card says how many agree.
- Each card shows the rule in words, how many transactions it would sort and why it was found.
- The user picks a category and taps Add rule, taps Edit first to open the editor with the rule filled in, or taps Not now.
- After Add rule, the card offers Apply to saved transactions.

## Saved transactions

| Rule | Behavior |
|---|---|
| Period | The user picks the last 30 days, 3 months or 12 months. The default is 3 months. |
| Time of day | A rule with a time of day is not tried on saved transactions, because they have no time. The page says so. |
| Ignore | An ignore rule does not change saved transactions. |
| Transfers | Transfers stay out unless the rule has the type Transfer. |
| Change | The rule sets the category, the subcategory and, if it has one, the description. Rows that already match are not listed. |
| Undo | Right after Apply, Undo puts the listed rows back to their old values. |
