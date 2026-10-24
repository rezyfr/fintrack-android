# Findings from the Maestro run

App bugs found by the flows. F-001 to F-008 are fixed in the app and the flows pass. F-009 is a manual check. F-010 needs a decision.

| ID | Case | Steps | What happened |
|---|---|---|---|
| F-001 | E2E-011 | Add an income of 1,000,000 on Home. Look at Money in. | Money in keeps the old value. It changes only after you open another tab and return. The flow visits Transactions first. |
| F-002 | E2E-018, E2E-019 | The server returns 500 on DELETE or PATCH. Delete or edit a row. | The error message and its Retry button show in a bar behind the floating tab bar. A screenshot shows the dark bar under the toolbar. The user cannot read the message or tap Retry. |
| F-003 | E2E-105 | Open Settings, Manage Categories. Look for Add Category. | The list of 13 categories fills the screen. The Add Category button is pushed off the screen and cannot be reached. |
| F-004 | E2E-109 | Turn the notification access off. Open Settings. | The status says Listening. `SettingsViewModel` sets `isListenerActive = true` and never reads the real state. Inactive and Enable Access never show. |
| F-005 | E2E-145, E2E-110 | Add an expense while the server is unreachable or returns 500. | The Add form stays open with a "Sync failed" message. A second tap on Save adds a duplicate. The row shows Failed with Retry, not Queued as the doc said. |
| F-006 | E2E-110 | Open Settings on a normal phone. Look for Retry Pending Syncs. | The main Settings column does not scroll. Retry Pending Syncs and Mark All as Synced are below the screen and cannot be reached. |
| F-007 | E2E-024 | Link a card row to an installment. Save changes. Open the row again. | The sheet shows Not linked. The server has the link and the step went up, but the row in the list keeps no `installmentId` until the list reloads. |
| F-008 | E2E-144 | Load the Transactions tab. Stop the server. Restart the app. | The banner says it shows cached data. The list is empty. |
| F-009 | E2E-151 | Type a description in the Add form. Rotate to landscape and back. | The form is not checked for the typed text. The flow records screenshots only. Check them by hand. |

Doc rows that were wrong are fixed in `docs/e2e-maestro-cases.md`.

Other facts found while writing the flows:

| Fact | Effect |
|---|---|
| The Transactions list opens on the last 7 days. The chip reads Filters (1) at start. | Older rows show only after Filters, Reset, Apply. |
| A captured notification is saved as synced at once. | No Pending row, no Confirm or Dismiss. E2E-026 is skipped. |
| The Home chart and the Budget chart take drag gestures. | Flows scroll with a swipe that starts below or beside them. |
| A second hideKeyboard inside a sheet presses Back and closes the sheet. | The flows call hideKeyboard once. |

## Found while fixing

| ID | Case | Steps | What happened |
|---|---|---|---|
| F-010 | E2E-140 | Launch the app fresh. Open the Add sheet. Look at the Save button. | On the first open the sheet rests about 130 px too low. The Save button sits under the system buttons of a three-button navigation bar. If the sheet is closed and opened again, or the user drags it, it moves up and Save shows. It is not an inset problem: padding for the navigation bar, and pinning Save below the scroll area, changed nothing, so both edits were reverted. The likely cause is that the sheet is placed before its content reaches its final height. Needs a decision. |
