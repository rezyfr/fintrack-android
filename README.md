# FinTrack

An Android app that automatically captures bank transactions from email notifications, categorizes them using AI, and syncs to Google Sheets.

## How it works

FinTrack listens for bank email notifications, parses the transaction details, uses Claude AI to categorize merchants, and syncs everything to a connected Google Sheets spreadsheet — with no manual data entry required.

## Features

- **Automatic capture** — reads bank email notifications via `NotificationListenerService`
- **AI categorization** — uses Claude Haiku to intelligently label merchants and transaction types
- **Google Sheets sync** — pushes transactions to a spreadsheet for easy review and analysis
- **Offline-first** — stores transactions locally in Room; syncs when connected
- **Inline editing** — edit pending transactions directly in the feed before they sync
- **Google Sign-In** — secure OAuth 2.0 authentication with encrypted token storage

## Tech stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt |
| Database | Room |
| Background work | WorkManager |
| Auth | Google Sign-In + EncryptedSharedPreferences |
| Networking | OkHttp |
| HTML parsing | JSoup |
| AI | Claude API (Haiku) |
| Sheets | Google Sheets API v4 |

## Setup

### Prerequisites

- Android Studio Hedgehog or later
- A Google Cloud project with the **Sheets API** and **Gmail API** enabled
- An Anthropic API key

### Configuration

1. Clone the repo
   ```
   git clone https://github.com/rezyfr/fintrack-android.git
   ```

2. Add your API keys to `local.properties`:
   ```
   ANTHROPIC_API_KEY=your_key_here
   GOOGLE_CLIENT_ID=your_client_id_here
   ```

3. Place your `google-services.json` in `app/`.

4. Build and run on a device running Android 8.0+ (API 26).

### Permissions

The app requires **Notification access** to read bank email notifications. Grant it via *Settings → Notifications → Notification access* after first launch.

## Architecture

```
ui/          — Compose screens and ViewModels (Feed, Add, Settings)
service/     — BankNotificationService (NotificationListenerService)
email/       — EmailFetcher: parses raw notification content
sheets/      — SheetsSyncer: Google Sheets API integration
auth/        — GoogleAuthManager: OAuth token lifecycle
data/        — Room database, DAOs, TransactionRepository
worker/      — WorkManager tasks for background sync
```

## License

MIT
