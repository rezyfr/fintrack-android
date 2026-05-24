# Supabase Backend Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Google Sheets as the cloud data store with Supabase (PostgreSQL), removing all Google Auth dependency from the Android app.

**Architecture:** Room continues as the local offline queue. SupabaseSyncerImpl replaces SheetsSyncerImpl behind the existing SheetsSyncer interface — TransactionRepository and SyncWorker are untouched. MonthlyOverviewFetcherImpl is stubbed to return failure (Dashboard shows cached data; will be replaced in Sub-project 3). Google Auth is removed entirely.

**Tech Stack:** Supabase REST API, OkHttp (already in project), Hilt DI (@Binds), Kotlin coroutines, Python 3 (migration script)

---

### Task 1: BuildConfig setup — add Supabase keys

**Files:**
- Modify: `local.properties` (not committed to git)
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add placeholder keys to local.properties**

Append to `local.properties` (already gitignored):
```
SUPABASE_URL=https://placeholder.supabase.co
SUPABASE_ANON_KEY=placeholder-anon-key
```

- [ ] **Step 2: Add BuildConfig fields**

In `app/build.gradle.kts`, inside `defaultConfig { }`, **add** these two lines (keep `SPREADSHEET_ID` for now — still referenced by `MonthlyOverviewFetcherImpl` until Task 6):
```kotlin
buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts local.properties
git commit -m "chore: add Supabase BuildConfig fields (placeholder keys)"
```

---

### Task 2: Extend SheetsRow + update callers

`SheetsRow` currently has 5 fields. Supabase needs `merchant` and `channel` too. Two files construct `SheetsRow` and must both be updated: `TransactionRepository` and `AddViewModel`.

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/model/SheetsRow.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/add/AddViewModel.kt`
- Modify: `app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt`

- [ ] **Step 1: Write failing test**

Add this test to `app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt`, inside the `TransactionRepositoryTest` class:

```kotlin
@Test
fun `syncTransaction passes merchant and channel from entity`() = runTest {
    val capturedRow = slot<SheetsRow>()
    coEvery { transactionDao.getById(1L) } returns existingEntity
    coEvery { transactionDao.updateStatus(any(), any()) } just Runs
    coEvery { sheetsSyncer.sync(capture(capturedRow)) } returns Result.success(Unit)

    repository.syncTransaction(1L)

    assertEquals("GRAB", capturedRow.captured.merchant)
    assertEquals("PromptPay", capturedRow.captured.channel)
    assertNull(capturedRow.captured.note)
}
```

The test uses the existing `existingEntity` (merchant="GRAB", channel="PromptPay") defined in `@Before`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.TransactionRepositoryTest.syncTransaction passes merchant and channel from entity"`
Expected: FAIL — compile error because `SheetsRow` has no `merchant`/`channel`/`note`

- [ ] **Step 3: Update SheetsRow**

Replace `app/src/main/java/com/fidriyanto/banktracker/data/model/SheetsRow.kt` entirely:

```kotlin
package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class SheetsRow(
    val tab: SheetTab,
    val date: LocalDate,
    val merchant: String,
    val item: String,
    val amount: Double,
    val category: String,
    val channel: String,
    val note: String? = null
)
```

- [ ] **Step 4: Update TransactionRepository.syncTransaction and insertManual**

In `app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt`, replace the `syncTransaction` method:

```kotlin
suspend fun syncTransaction(id: Long): Result<Unit> {
    val entity = transactionDao.getById(id)
        ?: return Result.failure(Exception("Transaction not found"))

    transactionDao.updateStatus(id, TransactionStatus.PENDING_SYNC)
    val row = SheetsRow(
        tab = entity.tab,
        date = LocalDate.parse(entity.dateIso),
        merchant = entity.merchant,
        item = entity.item,
        amount = entity.amount,
        category = entity.category,
        channel = entity.channel
    )
    return sheetsSyncer.sync(row).also { result ->
        val newStatus = if (result.isSuccess) TransactionStatus.SYNCED else TransactionStatus.SYNC_FAILED
        transactionDao.updateStatus(id, newStatus)
    }
}
```

Replace the `insertManual` method:

```kotlin
suspend fun insertManual(row: SheetsRow): Result<Unit> {
    val entity = TransactionEntity(
        merchant = row.merchant,
        item = row.item,
        amount = row.amount,
        category = row.category,
        dateIso = row.date.toString(),
        channel = row.channel,
        referenceNo = "",
        tab = row.tab,
        status = TransactionStatus.PENDING_SYNC
    )
    val id = transactionDao.insert(entity)
    return syncTransaction(id)
}
```

- [ ] **Step 5: Update AddViewModel**

In `app/src/main/java/com/fidriyanto/banktracker/ui/add/AddViewModel.kt`, replace line 48:

Old:
```kotlin
val row = SheetsRow(tab, s.date, s.description, amount, s.category)
```

New (named args to survive future field additions):
```kotlin
val row = SheetsRow(
    tab = tab,
    date = s.date,
    merchant = s.description,
    item = s.description,
    amount = amount,
    category = s.category,
    channel = "Manual"
)
```

Also update the success message on line 52 from `"Synced to Sheets!"` to `"Saved and syncing!"`.

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.TransactionRepositoryTest.syncTransaction passes merchant and channel from entity"`
Expected: PASS

- [ ] **Step 7: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests pass

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/model/SheetsRow.kt \
        app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt \
        app/src/main/java/com/fidriyanto/banktracker/ui/add/AddViewModel.kt \
        app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt
git commit -m "feat: add merchant/channel/note to SheetsRow; populate in syncTransaction and AddViewModel"
```

---

### Task 3: Create SupabaseSyncerImpl

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/sheets/SupabaseSyncerImpl.kt`

The JSON field names match the Supabase `transactions` table columns exactly (`tab`, `date`, `merchant`, `item`, `amount`, `category`, `channel`, `note`). The `tab` value is the Kotlin enum `.name` (EXPENSES, IDR_EXPENSES, INCOME, IDR_INCOME).

- [ ] **Step 1: Create SupabaseSyncerImpl**

```kotlin
package com.fidriyanto.banktracker.sheets

import android.util.Log
import com.fidriyanto.banktracker.BuildConfig
import com.fidriyanto.banktracker.data.model.SheetsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class SupabaseSyncerImpl @Inject constructor(
    private val httpClient: OkHttpClient
) : SheetsSyncer {

    override suspend fun sync(row: SheetsRow): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("tab", row.tab.name)
            put("date", row.date.toString())
            put("merchant", row.merchant)
            put("item", row.item)
            put("amount", row.amount)
            put("category", row.category)
            put("channel", row.channel)
            if (row.note != null) put("note", row.note)
        }.toString()

        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/rest/v1/transactions")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        Log.d("SupabaseSyncer", "POST transactions: $body")
        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("SupabaseSyncer", "status=${response.code} body=$responseBody")
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Supabase error: HTTP ${response.code} — $responseBody"))
        } catch (e: Exception) {
            Log.e("SupabaseSyncer", "sync failed", e)
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/sheets/SupabaseSyncerImpl.kt
git commit -m "feat: add SupabaseSyncerImpl posting transactions to Supabase REST API"
```

---

### Task 4: Create BudgetRepository

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/data/repository/BudgetRepository.kt`

The Supabase `budgets` table uses snake_case column names (`food_drink`, `health_wellbeing`). The `MonthlyBudgetEntity` fields are camelCase (`foodDrink`, `healthWellbeing`) — the mapping is explicit in both directions.

- [ ] **Step 1: Create BudgetRepository**

```kotlin
package com.fidriyanto.banktracker.data.repository

import android.util.Log
import com.fidriyanto.banktracker.BuildConfig
import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val baseUrl get() = "${BuildConfig.SUPABASE_URL}/rest/v1/budgets"
    private val apiKey get() = BuildConfig.SUPABASE_ANON_KEY

    suspend fun get(): Result<List<MonthlyBudgetEntity>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl?select=*")
            .addHeader("apikey", apiKey)
            .build()
        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Supabase error: HTTP ${response.code}"))
            }
            val arr = JSONArray(body)
            val budgets = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                MonthlyBudgetEntity(
                    currency = obj.getString("currency"),
                    bills = obj.getDouble("bills"),
                    subscriptions = obj.getDouble("subscriptions"),
                    entertainment = obj.getDouble("entertainment"),
                    foodDrink = obj.getDouble("food_drink"),
                    groceries = obj.getDouble("groceries"),
                    healthWellbeing = obj.getDouble("health_wellbeing"),
                    other = obj.getDouble("other"),
                    shopping = obj.getDouble("shopping"),
                    transport = obj.getDouble("transport"),
                    travel = obj.getDouble("travel"),
                    business = obj.getDouble("business"),
                    gifts = obj.getDouble("gifts")
                )
            }
            Result.success(budgets)
        } catch (e: Exception) {
            Log.e("BudgetRepository", "get failed", e)
            Result.failure(e)
        }
    }

    suspend fun upsert(budget: MonthlyBudgetEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("currency", budget.currency)
            put("bills", budget.bills)
            put("subscriptions", budget.subscriptions)
            put("entertainment", budget.entertainment)
            put("food_drink", budget.foodDrink)
            put("groceries", budget.groceries)
            put("health_wellbeing", budget.healthWellbeing)
            put("other", budget.other)
            put("shopping", budget.shopping)
            put("transport", budget.transport)
            put("travel", budget.travel)
            put("business", budget.business)
            put("gifts", budget.gifts)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl?on_conflict=currency")
            .addHeader("apikey", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Supabase error: HTTP ${response.code} — $responseBody"))
        } catch (e: Exception) {
            Log.e("BudgetRepository", "upsert failed", e)
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/repository/BudgetRepository.kt
git commit -m "feat: add BudgetRepository for Supabase budgets table (get + upsert)"
```

---

### Task 5: Swap DI — bind SupabaseSyncerImpl, delete SheetsSyncerImpl

Both changes must land in the same commit or the build breaks between them.

**Files:**
- Delete: `app/src/main/java/com/fidriyanto/banktracker/sheets/SheetsSyncerImpl.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/di/SheetsModule.kt`

- [ ] **Step 1: Update SheetsModule**

Replace `app/src/main/java/com/fidriyanto/banktracker/di/SheetsModule.kt` entirely:

```kotlin
package com.fidriyanto.banktracker.di

import com.fidriyanto.banktracker.sheets.SheetsSyncer
import com.fidriyanto.banktracker.sheets.SupabaseSyncerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SheetsModule {
    @Binds @Singleton
    abstract fun bindSheetsSyncer(impl: SupabaseSyncerImpl): SheetsSyncer
}
```

- [ ] **Step 2: Delete SheetsSyncerImpl**

```bash
git rm app/src/main/java/com/fidriyanto/banktracker/sheets/SheetsSyncerImpl.kt
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/di/SheetsModule.kt
git commit -m "feat: swap SheetsSyncer DI binding to SupabaseSyncerImpl; delete SheetsSyncerImpl"
```

---

### Task 6: Remove Google auth

All edits and deletions in this task must land in one commit — deleting `GoogleAuthManager.kt` breaks the build until all references to it are removed.

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/sheets/MonthlyOverviewFetcherImpl.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/settings/SettingsScreen.kt`
- Delete: `app/src/main/java/com/fidriyanto/banktracker/auth/GoogleAuthManager.kt`
- Delete: `app/src/main/java/com/fidriyanto/banktracker/auth/GoogleAuthManagerImpl.kt`
- Delete: `app/src/main/java/com/fidriyanto/banktracker/di/AuthModule.kt`

- [ ] **Step 1: Stub MonthlyOverviewFetcherImpl**

Replace `app/src/main/java/com/fidriyanto/banktracker/sheets/MonthlyOverviewFetcherImpl.kt` entirely (removes GoogleAuthManager + SPREADSHEET_ID references):

```kotlin
package com.fidriyanto.banktracker.sheets

import okhttp3.OkHttpClient
import javax.inject.Inject

class MonthlyOverviewFetcherImpl @Inject constructor(
    private val httpClient: OkHttpClient
) : MonthlyOverviewFetcher {
    override suspend fun fetch(): Result<MonthlyOverviewFetcher.FetchResult> =
        Result.failure(Exception("Dashboard data not available — will be replaced in Sub-project 3"))
}
```

- [ ] **Step 2: Update SettingsViewModel**

Replace `app/src/main/java/com/fidriyanto/banktracker/ui/settings/SettingsViewModel.kt` entirely:

```kotlin
package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val claudeApiKey: String = "",
    val isListenerActive: Boolean = false,
    val isSyncing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SecurePrefs,
    private val repository: TransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = SettingsState(
            claudeApiKey = prefs.claudeApiKey,
            isListenerActive = true
        )
    }

    fun saveClaudeKey(key: String) { prefs.claudeApiKey = key; refresh() }

    fun retryPendingSyncs() = viewModelScope.launch {
        _state.value = _state.value.copy(isSyncing = true)
        repository.retryFailedSyncs()
        _state.value = _state.value.copy(isSyncing = false)
    }
}
```

- [ ] **Step 3: Update SettingsScreen**

Replace `app/src/main/java/com/fidriyanto/banktracker/ui/settings/SettingsScreen.kt` entirely (removes Google Account card, signInLauncher, consentLauncher, and LaunchedEffect):

```kotlin
package com.fidriyanto.banktracker.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.ui.theme.Accent
import com.fidriyanto.banktracker.ui.theme.Destructive

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var claudeKeyInput by remember(state.claudeApiKey) { mutableStateOf(state.claudeApiKey) }
    var showKey by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Claude API Key", fontWeight = FontWeight.SemiBold, color = Color.White)
                OutlinedTextField(
                    value = claudeKeyInput,
                    onValueChange = { claudeKeyInput = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "Hide" else "Show", fontSize = 12.sp)
                        }
                    }
                )
                Button(onClick = { viewModel.saveClaudeKey(claudeKeyInput) }) { Text("Save") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notification Listener", fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dotColor = if (state.isListenerActive) Accent else Destructive
                    Text("●", color = dotColor, fontSize = 18.sp)
                    Text(if (state.isListenerActive) "Listening" else "Inactive", color = Color.White)
                }
                if (!state.isListenerActive) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text("Enable Access") }
                }
            }
        }

        Button(
            onClick = { viewModel.retryPendingSyncs() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSyncing
        ) {
            if (state.isSyncing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
            else Text("Retry Pending Syncs")
        }
    }
}
```

- [ ] **Step 4: Delete auth files**

```bash
git rm app/src/main/java/com/fidriyanto/banktracker/auth/GoogleAuthManager.kt
git rm app/src/main/java/com/fidriyanto/banktracker/auth/GoogleAuthManagerImpl.kt
git rm app/src/main/java/com/fidriyanto/banktracker/di/AuthModule.kt
```

- [ ] **Step 5: Verify build — no GoogleAuthManager references**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

If there are unresolved references, grep for remaining usages:
```bash
grep -r "GoogleAuthManager" app/src/main/java --include="*.kt"
```
Expected: no output

- [ ] **Step 6: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests pass

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/sheets/MonthlyOverviewFetcherImpl.kt \
        app/src/main/java/com/fidriyanto/banktracker/ui/settings/SettingsViewModel.kt \
        app/src/main/java/com/fidriyanto/banktracker/ui/settings/SettingsScreen.kt
git commit -m "feat: remove Google auth; stub MonthlyOverviewFetcher; clean Settings UI"
```

---

### Task 7: Dependency cleanup

Remove Google and jsoup dependencies. These are only used by files already deleted.

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Update build.gradle.kts**

In `defaultConfig { }`, remove:
```kotlin
buildConfigField("String", "SPREADSHEET_ID", "\"${localProps.getProperty("SPREADSHEET_ID", "")}\"")
```

In `dependencies { }`, remove these three lines:
```kotlin
implementation(libs.jsoup)
implementation(libs.google.signin)
implementation(libs.google.api.client.android)
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — confirm no remaining references to removed libraries by checking for compile errors

- [ ] **Step 3: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: remove Google auth + jsoup dependencies; remove SPREADSHEET_ID BuildConfig"
```

---

### Task 8: Supabase SQL schema file

**Files:**
- Create: `supabase/schema.sql`

- [ ] **Step 1: Create schema file**

```bash
mkdir -p supabase
```

Write `supabase/schema.sql`:

```sql
-- Run this in the Supabase SQL editor to create the required tables.

create table transactions (
    id          bigserial primary key,
    merchant    text not null,
    item        text not null,
    amount      numeric not null,
    category    text not null,
    date        date not null,
    channel     text not null,
    tab         text not null,
    note        text,
    created_at  timestamptz default now()
);

create table budgets (
    currency         text primary key,
    bills            numeric not null,
    subscriptions    numeric not null,
    entertainment    numeric not null,
    food_drink       numeric not null,
    groceries        numeric not null,
    health_wellbeing numeric not null,
    other            numeric not null,
    shopping         numeric not null,
    transport        numeric not null,
    travel           numeric not null,
    business         numeric not null,
    gifts            numeric not null
);
```

- [ ] **Step 2: Commit**

```bash
git add supabase/schema.sql
git commit -m "docs: add Supabase schema SQL for transactions and budgets tables"
```

---

### Task 9: Python migration script

Reads all rows from Google Sheets, posts them to Supabase. One-time use.

**Files:**
- Create: `scripts/migrate_sheets_to_supabase.py`

- [ ] **Step 1: Create scripts directory**

```bash
mkdir -p scripts
```

- [ ] **Step 2: Write script**

Write `scripts/migrate_sheets_to_supabase.py`:

```python
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
    """Parse D/M/YYYY or DD/MM/YYYY into ISO 8601 YYYY-MM-DD."""
    raw = raw.strip()
    parts = raw.split("/")
    if len(parts) == 3:
        try:
            d, m, y = int(parts[0]), int(parts[1]), int(parts[2])
            return datetime.date(y, m, d).isoformat()
        except ValueError:
            pass
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
```

- [ ] **Step 3: Commit**

```bash
git add scripts/migrate_sheets_to_supabase.py
git commit -m "feat: add Python migration script to move Sheets history to Supabase"
```

---

### Task 10: Final verification

- [ ] **Step 1: Clean build**

Run: `./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: All unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests pass

- [ ] **Step 3: Install on device**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 4: Verify Settings screen**

Open Settings — confirm three sections visible: Claude API Key, Notification Listener, Retry Pending Syncs button. No Google Account section.

- [ ] **Step 5: Tap Retry Pending Syncs**

All on-device Room transactions will attempt to POST to Supabase. With placeholder keys they will fail with HTTP error and stay SYNC_FAILED — this is expected. Configure real `SUPABASE_URL` + `SUPABASE_ANON_KEY` in `local.properties`, rebuild and install, then tap Retry again to push them.

- [ ] **Step 6: Commit any fixes**

```bash
git add -p
git commit -m "fix: final cleanup after Supabase migration"
```
