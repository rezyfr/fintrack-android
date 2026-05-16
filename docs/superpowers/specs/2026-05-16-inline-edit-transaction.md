# Inline Edit Transaction — Design Spec

**Date:** 2026-05-16

## Goal

Allow the user to edit a `PENDING_EDIT` transaction directly from the Feed screen before it is synced to Google Sheets.

## Interaction

Tapping a `PENDING_EDIT` card expands it in-place. Two fields become editable: `item` (description) and `category`. Two action buttons appear below:

- **Confirm & Sync** — saves the edits and immediately syncs the row to Sheets. Status transitions `PENDING_EDIT → PENDING_SYNC → SYNCED` (or `SYNC_FAILED` on error).
- **Dismiss** — collapses the card without saving any changes.

Tapping an already-expanded card does not collapse it (the Dismiss button handles that). Cards with any other status (`SYNCED`, `PENDING_SYNC`, `SYNC_FAILED`) are not expandable.

## Components

### `TransactionCard`

- Add `onConfirm: (item: String, category: String) -> Unit` and `onDismiss: () -> Unit` callbacks.
- Add `var expanded by remember { mutableStateOf(false) }` local state.
- When `entity.status == PENDING_EDIT`, the card becomes clickable (`onClick = { expanded = !expanded }`).
- When `expanded`, render below the existing row:
  - `OutlinedTextField` for `item` (label: "Description")
  - `OutlinedTextField` for `category` (label: "Category")
  - Row with `TextButton("Dismiss")` and `Button("Confirm & Sync")`

### `FeedViewModel`

Add:
```kotlin
fun updateAndSync(id: Long, item: String, category: String) =
    viewModelScope.launch { repository.updateAndSync(id, item, category) }
```

### `TransactionRepository`

Add:
```kotlin
suspend fun updateAndSync(id: Long, item: String, category: String) {
    val entity = transactionDao.getById(id) ?: return
    transactionDao.update(entity.copy(item = item, category = category))
    syncTransaction(id)
}
```

No DAO changes needed — `update(entity)` already exists.

## Data Flow

```
User taps card → expanded = true
User edits fields → local String state
User taps "Confirm & Sync" → FeedViewModel.updateAndSync(id, item, category)
  → repository.updateAndSync → dao.update → syncTransaction
  → status: PENDING_EDIT → PENDING_SYNC → SYNCED / SYNC_FAILED
User taps "Dismiss" → expanded = false (no DB write)
```

## Out of Scope

- Editing `amount`, `tab`, `date` — read-only for now.
- Bulk edit or swipe-to-dismiss.
- Undo after Confirm.
