# Merchant Autocomplete — Design Spec

**Date:** 2026-05-31
**Capability:** `merchant-suggestion`
**Story:** `merchant-autocomplete`

## Problem

When adding or editing a transaction the merchant field is a free-text input. Users who record the same merchants repeatedly (LINE MAN, Grab, 7-Eleven) must retype the name each time. There is no autocomplete.

## Goal

Show a dropdown of up to 10 previously used merchant names when the field is focused; filter the list as the user types. Both the web app and the Android app must support this.

## Scope

In scope: Add form, Edit form/bottom-sheet on both platforms.
Out of scope: server-side merchant directory, fuzzy matching, cross-device sync.

---

## Storage

### Web

- Key: `fintrack_merchant_history` in `localStorage`
- Value: JSON array of strings, newest first, max 10 entries, deduplicated case-insensitively
- Access point: `web/src/hooks/useMerchantHistory.js` — exports `[history, addToHistory]`
- `addToHistory(merchant)`: trim whitespace, skip empty, prepend, remove duplicates (case-insensitive), slice to 10, write back

### Android

- Jetpack DataStore Preferences (no Room schema change, no migration)
- Key: `StringPreferencesKey("merchant_history")` — JSON array of strings
- Pyramid path: `MerchantHistoryDataSourceImpl` (DataStore) → `MerchantHistoryRepositoryImpl` → `SaveMerchantUseCase` + `GetRecentMerchantsUseCase`
- Same dedup / cap invariants as web

---

## UI / Interaction

**Shared behaviour (both platforms):**

| Trigger | Result |
|---------|--------|
| Focus merchant field | Show dropdown with all stored merchants (up to 10) |
| Type characters | Filter list to case-insensitive substring matches |
| Select suggestion | Fill field with selected value; close dropdown |
| No matches / empty history | Dropdown hidden |
| Successful save | Append merchant to history |
| Failed save | History unchanged |

### Web — `MerchantInput` component

- Lives at `web/src/components/MerchantInput.jsx`
- Props: `value`, `onChange`, `onMerchantSaved` (called by parent on successful submit)
- Renders the existing `<input>` plus an absolutely-positioned suggestion list
- Blur hides dropdown after 100 ms delay (allows click on suggestion to register first)
- Both `AddTransactionForm` and `EditTransactionModal` replace their bare merchant `<input>` with `<MerchantInput>`

### Android — description field dropdown

- `AddViewModel` gains `merchantSuggestions: StateFlow<List<String>>`, derived by filtering stored history against the current `description` value; updated on every keystroke
- A named top-level composable `MerchantSuggestionDropdown(suggestions, onSelect)` renders a `DropdownMenu` anchored below the description field; used in both `AddScreen` and `EditTransactionBottomSheet`
- `EditTransactionBottomSheet` (existing composable) follows the same pattern, injecting the same use cases
- On successful submit in both screens, `SaveMerchantUseCase` is called with the submitted merchant value

---

## Manifest

### Capability — `merchant-suggestion`

```json
{
  "id": "merchant-suggestion",
  "title": "Merchant Suggestion",
  "status": "Approved",
  "owners": ["frotylatz@gmail.com"],
  "stories": ["merchant-autocomplete"],
  "flows": ["merchant-autocomplete-flow"]
}
```

### Story — `merchant-autocomplete`

```json
{
  "id": "merchant-autocomplete",
  "capability": "merchant-suggestion",
  "role": "user",
  "want": "to see suggestions of previously used merchants when filling the merchant field",
  "soThat": "I can quickly reuse common merchants without retyping them",
  "acceptance": [
    "focusing the merchant field shows a dropdown of up to 10 previously saved merchants",
    "typing filters the list to merchants containing the typed string (case-insensitive)",
    "selecting a suggestion fills the merchant field and closes the dropdown",
    "if no history exists or no suggestions match the current input, no dropdown is shown",
    "a merchant name is saved to history only when a transaction is successfully submitted",
    "history is deduplicated — the same merchant appears at most once, ordered most-recent first",
    "history is capped at 10 entries; adding an 11th drops the oldest"
  ]
}
```

### Flow — `merchant-autocomplete-flow`

```json
{
  "id": "merchant-autocomplete-flow",
  "steps": [
    { "actor": "user", "action": "focuses the merchant field on the Add or Edit form" },
    {
      "actor": "app",
      "action": "renders the suggestion dropdown populated from local history",
      "story": "merchant-autocomplete",
      "noNavigation": true,
      "triggerElement": "MerchantSuggestionDropdown"
    },
    { "actor": "user", "action": "types characters or taps a suggestion" },
    {
      "actor": "app",
      "action": "filters list to substring matches, or fills the field and dismisses the dropdown",
      "story": "merchant-autocomplete",
      "noNavigation": true
    }
  ]
}
```

---

## Testing

### Web

- `useMerchantHistory` unit tests: dedup, cap at 10, order, empty initial state
- `MerchantInput` component tests: dropdown on focus, filters on type, selection fills field, hidden when empty
- Existing `AddTransactionForm` and `EditTransactionModal` tests remain green

### Android

- `SaveMerchantUseCase` / `GetRecentMerchantsUseCase` unit tests: same invariants
- `AddViewModel` test: `merchantSuggestions` emits filtered list as description changes; save calls use case on success
- DataStore replaced with fake (in-memory) in all unit tests

---

## Acceptance Criteria (task.json)

```
ac-1  focusing the merchant field shows a dropdown of up to 10 previously saved merchants
ac-2  typing filters the list to merchants containing the typed string (case-insensitive)
ac-3  selecting a suggestion fills the merchant field and closes the dropdown
ac-4  if no history exists or no suggestions match the current input, no dropdown is shown
ac-5  a merchant name is saved to history only when a transaction is successfully submitted
ac-6  history is deduplicated — the same merchant appears at most once, ordered most-recent first
ac-7  history is capped at 10 entries; adding an 11th drops the oldest
```
