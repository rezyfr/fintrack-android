# Inline Edit Transaction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users tap a `PENDING_EDIT` transaction card in the Feed to expand it inline, edit the description and category, then confirm and sync to Google Sheets.

**Architecture:** `TransactionCard` gains local expanded state and edit fields; `FeedViewModel` adds `updateAndSync`; `TransactionRepository` adds `updateAndSync` that writes the edits then calls the existing `syncTransaction`.

**Tech Stack:** Kotlin · Jetpack Compose · Hilt · Room · existing `TransactionRepository`/`SheetsSyncer` pipeline

---

## File Map

```
Modify: app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt
Modify: app/src/main/java/com/fidriyanto/banktracker/ui/feed/FeedViewModel.kt
Modify: app/src/main/java/com/fidriyanto/banktracker/ui/feed/TransactionCard.kt
Create: app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt
```

---

## Task 1: Repository — `updateAndSync`

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt`
- Create: `app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt`

- [ ] **Step 1: Add required test dependencies to `app/build.gradle.kts`**

Open `app/build.gradle.kts` and confirm these are already present in `dependencies {}`. If not, add them:

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.10")
```

Run:
```
./gradlew :app:dependencies --configuration testDebugRuntimeClasspath | grep -E "mockk|coroutines-test"
```
Expected: both libraries listed.

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt`:

```kotlin
package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.*
import com.fidriyanto.banktracker.data.model.*
import com.fidriyanto.banktracker.categorization.CategoryResolver
import com.fidriyanto.banktracker.email.EmailFetcher
import com.fidriyanto.banktracker.email.EmailParser
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TransactionRepositoryTest {

    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val processedRefDao = mockk<ProcessedRefDao>(relaxed = true)
    private val emailFetcher = mockk<EmailFetcher>()
    private val emailParser = mockk<EmailParser>()
    private val categoryResolver = mockk<CategoryResolver>()
    private val sheetsSyncer = mockk<SheetsSyncer>()
    private val prefs = mockk<SecurePrefs>()

    private lateinit var repository: TransactionRepository

    private val existingEntity = TransactionEntity(
        id = 1L,
        merchant = "GRAB",
        item = "GRAB",
        amount = 250.0,
        category = "Transport",
        dateIso = "2026-05-16",
        channel = "PromptPay",
        referenceNo = "REF001",
        tab = SheetTab.EXPENSES,
        status = TransactionStatus.PENDING_EDIT
    )

    @Before
    fun setUp() {
        repository = TransactionRepository(
            transactionDao, processedRefDao,
            emailFetcher, emailParser, categoryResolver, sheetsSyncer, prefs
        )
    }

    @Test
    fun `updateAndSync updates item and category then syncs`() = runTest {
        coEvery { transactionDao.getById(1L) } returns existingEntity
        coEvery { sheetsSyncer.sync(any()) } returns Result.success(Unit)

        repository.updateAndSync(1L, "Grab Food", "Food & Drink")

        coVerify {
            transactionDao.update(match {
                it.item == "Grab Food" && it.category == "Food & Drink" && it.id == 1L
            })
        }
        coVerify { sheetsSyncer.sync(any()) }
    }

    @Test
    fun `updateAndSync does nothing when entity not found`() = runTest {
        coEvery { transactionDao.getById(99L) } returns null

        repository.updateAndSync(99L, "Edit", "Category")

        coVerify(exactly = 0) { transactionDao.update(any()) }
        coVerify(exactly = 0) { sheetsSyncer.sync(any()) }
    }
}
```

- [ ] **Step 3: Run test to confirm it fails**

```
./gradlew :app:testDebugUnitTest --tests "*.TransactionRepositoryTest" 2>&1 | tail -20
```
Expected: compilation error — `updateAndSync` does not exist yet.

- [ ] **Step 4: Add `updateAndSync` to `TransactionRepository`**

Open `app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt` and add after `insertManual`:

```kotlin
suspend fun updateAndSync(id: Long, item: String, category: String) {
    val entity = transactionDao.getById(id) ?: return
    transactionDao.update(entity.copy(item = item, category = category))
    syncTransaction(id)
}
```

- [ ] **Step 5: Run test to confirm it passes**

```
./gradlew :app:testDebugUnitTest --tests "*.TransactionRepositoryTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`, both tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt
git add app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt
git commit -m "feat: add updateAndSync to TransactionRepository"
```

---

## Task 2: ViewModel — expose `updateAndSync`

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/feed/FeedViewModel.kt`

- [ ] **Step 1: Add `updateAndSync` to `FeedViewModel`**

Open `app/src/main/java/com/fidriyanto/banktracker/ui/feed/FeedViewModel.kt`. The file currently contains:

```kotlin
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun retry(id: Long) = viewModelScope.launch { repository.syncTransaction(id) }
}
```

Replace with:

```kotlin
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun retry(id: Long) = viewModelScope.launch { repository.syncTransaction(id) }

    fun updateAndSync(id: Long, item: String, category: String) =
        viewModelScope.launch { repository.updateAndSync(id, item, category) }
}
```

- [ ] **Step 2: Build to verify no compilation errors**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/feed/FeedViewModel.kt
git commit -m "feat: expose updateAndSync in FeedViewModel"
```

---

## Task 3: UI — inline edit in `TransactionCard`

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/feed/TransactionCard.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/feed/FeedScreen.kt`

- [ ] **Step 1: Update `TransactionCard` signature and add inline edit UI**

Replace the full contents of `app/src/main/java/com/fidriyanto/banktracker/ui/feed/TransactionCard.kt` with:

```kotlin
package com.fidriyanto.banktracker.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TransactionCard(
    entity: TransactionEntity,
    onRetry: () -> Unit,
    onConfirm: (item: String, category: String) -> Unit
) {
    val borderColor = when (entity.status) {
        TransactionStatus.PENDING_EDIT -> Primary
        TransactionStatus.PENDING_SYNC -> Warning
        TransactionStatus.SYNC_FAILED -> Destructive
        TransactionStatus.SYNCED -> Color.Transparent
    }
    val badgeText = when (entity.status) {
        TransactionStatus.PENDING_EDIT -> "Pending"
        TransactionStatus.SYNCED -> "Synced"
        TransactionStatus.PENDING_SYNC -> "Queued"
        TransactionStatus.SYNC_FAILED -> "Failed"
    }
    val badgeColor = when (entity.status) {
        TransactionStatus.PENDING_EDIT -> Secondary
        TransactionStatus.SYNCED -> Accent
        TransactionStatus.PENDING_SYNC -> Warning
        TransactionStatus.SYNC_FAILED -> Destructive
    }
    val date = runCatching { LocalDate.parse(entity.dateIso) }.getOrNull()
    val dateStr = date?.format(DateTimeFormatter.ofPattern("d MMM")) ?: ""
    val amountStr = if (entity.amount % 1.0 == 0.0) entity.amount.toInt().toString() else entity.amount.toString()

    var expanded by remember { mutableStateOf(false) }
    var itemInput by remember(entity.id) { mutableStateOf(entity.item) }
    var categoryInput by remember(entity.id) { mutableStateOf(entity.category) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (borderColor != Color.Transparent) 1.dp else 0.dp, borderColor, RoundedCornerShape(12.dp))
            .then(
                if (entity.status == TransactionStatus.PENDING_EDIT)
                    Modifier.clickable { expanded = !expanded }
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entity.item, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                Text("${entity.category} · $dateStr", fontSize = 12.sp, color = MutedText)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("-฿$amountStr", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmountRed)
                Text(badgeText, fontSize = 11.sp, color = badgeColor)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                OutlinedTextField(
                    value = itemInput,
                    onValueChange = { itemInput = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = false }) {
                        Text("Dismiss", color = MutedText)
                    }
                    Button(
                        onClick = {
                            onConfirm(itemInput, categoryInput)
                            expanded = false
                        },
                        enabled = itemInput.isNotBlank() && categoryInput.isNotBlank()
                    ) {
                        Text("Confirm & Sync")
                    }
                }
            }
        }

        if (entity.status == TransactionStatus.SYNC_FAILED) {
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text("Retry", color = Secondary, fontSize = 12.sp)
            }
        }
    }
}
```

- [ ] **Step 2: Update `FeedScreen` to pass `onConfirm`**

Open `app/src/main/java/com/fidriyanto/banktracker/ui/feed/FeedScreen.kt`. Find the `TransactionCard` call and update it:

```kotlin
items(transactions, key = { it.id }) { entity ->
    TransactionCard(
        entity = entity,
        onRetry = { viewModel.retry(entity.id) },
        onConfirm = { item, category -> viewModel.updateAndSync(entity.id, item, category) }
    )
}
```

- [ ] **Step 3: Build and install**

```
./gradlew installDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL` and installed on device.

- [ ] **Step 4: Manual verification**

1. Open the app → Feed screen
2. Tap a `PENDING_EDIT` (Pending) card — it should expand showing Description and Category fields pre-filled
3. Edit the description or category
4. Tap **Confirm & Sync** — card should collapse, status badge should change to Queued then Synced
5. Tap **Dismiss** on another pending card — it should collapse with no changes

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/feed/TransactionCard.kt
git add app/src/main/java/com/fidriyanto/banktracker/ui/feed/FeedScreen.kt
git commit -m "feat: inline edit for PENDING_EDIT transactions in Feed"
```
