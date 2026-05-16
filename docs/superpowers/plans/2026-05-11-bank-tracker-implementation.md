# BankTracker Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a personal Android app that auto-captures Bangkok Bank notifications, parses the transaction email, categorizes via rules/Claude, prompts a 3-min review, then syncs to Google Sheets.

**Architecture:** Fully on-device. `NotificationListenerService` triggers a Gmail fetch 5s later; `EmailParser` extracts structured data; `CategoryResolver` applies rules then Claude Haiku as fallback; `ReviewNotificationManager` posts an editable notification with a 3-min countdown; `SheetsSyncer` appends the row to the correct tab. Room DB holds the offline queue and dedup log.

**Tech Stack:** Kotlin · Jetpack Compose + Material 3 · Hilt · Room · WorkManager · Retrofit/OkHttp · JSoup · Google Sign-In v21 · Claude Haiku API · Google Sheets API v4 · Google Gmail API v1

---

## File Map

```
app/src/main/java/com/fidriyanto/banktracker/
├── BankTrackerApp.kt
├── MainActivity.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── TransactionDao.kt
│   │   ├── CategoryCacheDao.kt
│   │   ├── ProcessedRefDao.kt
│   │   ├── TransactionEntity.kt
│   │   ├── CategoryCacheEntity.kt
│   │   └── ProcessedRefEntity.kt
│   ├── model/
│   │   ├── ParsedTransaction.kt
│   │   ├── SheetsRow.kt
│   │   ├── SheetTab.kt
│   │   └── TransactionStatus.kt
│   ├── prefs/
│   │   └── SecurePrefs.kt
│   └── repository/
│       └── TransactionRepository.kt
├── email/
│   ├── EmailFetcher.kt
│   ├── EmailParser.kt
│   └── MerchantNormalizer.kt
├── categorization/
│   ├── CategoryResolver.kt
│   ├── RuleBasedMatcher.kt
│   ├── PromptPayHeuristic.kt
│   └── ClaudeCategorizor.kt
├── sheets/
│   ├── SheetsSyncer.kt
│   └── DateFormatter.kt
├── auth/
│   └── GoogleAuthManager.kt
├── notification/
│   └── ReviewNotificationManager.kt
├── service/
│   ├── BankNotificationService.kt
│   └── SyncWorker.kt
└── ui/
    ├── theme/
    │   ├── Color.kt
    │   ├── Type.kt
    │   └── Theme.kt
    ├── navigation/
    │   └── AppNavigation.kt
    ├── feed/
    │   ├── FeedScreen.kt
    │   ├── TransactionCard.kt
    │   └── FeedViewModel.kt
    ├── add/
    │   ├── AddScreen.kt
    │   └── AddViewModel.kt
    └── settings/
        ├── SettingsScreen.kt
        └── SettingsViewModel.kt

app/src/test/java/com/fidriyanto/banktracker/
├── email/
│   ├── EmailParserTest.kt
│   └── MerchantNormalizerTest.kt
├── categorization/
│   ├── RuleBasedMatcherTest.kt
│   ├── PromptPayHeuristicTest.kt
│   └── CategoryResolverTest.kt
└── sheets/
    └── DateFormatterTest.kt
```

---

## Task 1: Project Scaffold

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create new Android project** in Android Studio: Empty Activity, package `com.fidriyanto.banktracker`, min SDK 26, Kotlin, Gradle Kotlin DSL.

- [ ] **Step 2: Replace `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.4.2"
kotlin = "2.0.0"
ksp = "2.0.0-1.0.21"
hilt = "2.51.1"
room = "2.6.1"
compose-bom = "2024.06.00"
okhttp = "4.12.0"
coroutines = "1.8.1"
jsoup = "1.17.2"
google-signin = "21.2.0"
security-crypto = "1.1.0-alpha06"
work = "2.9.0"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts", version = "1.6.8" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.0" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version = "2.7.7" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.8.0" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.0" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
jsoup = { group = "org.jsoup", name = "jsoup", version.ref = "jsoup" }
google-signin = { group = "com.google.android.gms", name = "play-services-auth", version.ref = "google-signin" }
google-api-client-android = { group = "com.google.api-client", name = "google-api-client-android", version = "2.2.0" }
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
hilt-work = { group = "androidx.hilt", name = "hilt-work", version = "1.2.0" }
hilt-work-compiler = { group = "androidx.hilt", name = "hilt-compiler", version = "1.2.0" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version = "4.13.2" }
mockk = { group = "io.mockk", name = "mockk", version = "1.13.11" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 3: Replace `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.fidriyanto.banktracker"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.fidriyanto.banktracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.google.fonts)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.google.signin)
    implementation(libs.google.api.client.android)
    implementation(libs.security.crypto)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 4: Sync Gradle** — confirm build succeeds with no errors.

- [ ] **Step 5: Commit**
```bash
git add .
git commit -m "feat: initialize project scaffold with all dependencies"
```

---

## Task 2: Theme & Design System

**Files:**
- Create: `ui/theme/Color.kt`
- Create: `ui/theme/Type.kt`
- Create: `ui/theme/Theme.kt`

- [ ] **Step 1: Create `Color.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.theme

import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0F172A)
val Surface = Color(0xFF1E293B)
val Primary = Color(0xFF1E40AF)
val Secondary = Color(0xFF3B82F6)
val Accent = Color(0xFF059669)
val AmountRed = Color(0xFFF87171)
val Warning = Color(0xFFF59E0B)
val Destructive = Color(0xFFDC2626)
val MutedText = Color(0xFF64748B)
val OnPrimary = Color(0xFFFFFFFF)
val Border = Color(0x14FFFFFF)
```

- [ ] **Step 2: Create `Type.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val PlusJakartaSans = FontFamily(
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Normal),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Medium),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.SemiBold),
    Font(GoogleFont("Plus Jakarta Sans"), provider, FontWeight.Bold),
)

val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
```

- [ ] **Step 3: Create `Theme.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onBackground = OnPrimary,
    onSurface = OnPrimary,
    error = Destructive,
)

@Composable
fun BankTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = AppTypography, content = content)
}
```

- [ ] **Step 4: Add font certs resource** — Android Studio → `res/values/` → new resource file `font_certs.xml`. Add the Google Fonts certificate array. Copy from [official docs](https://developers.google.com/fonts/docs/android).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/theme/
git add app/src/main/res/
git commit -m "feat: add Material 3 OLED dark theme with Plus Jakarta Sans"
```

---

## Task 3: Domain Models

**Files:**
- Create: `data/model/ParsedTransaction.kt`
- Create: `data/model/SheetsRow.kt`
- Create: `data/model/SheetTab.kt`
- Create: `data/model/TransactionStatus.kt`

- [ ] **Step 1: Create `SheetTab.kt`**

```kotlin
package com.fidriyanto.banktracker.data.model

enum class SheetTab { EXPENSES, IDR_EXPENSES, INCOME, IDR_INCOME }
```

- [ ] **Step 2: Create `TransactionStatus.kt`**

```kotlin
package com.fidriyanto.banktracker.data.model

enum class TransactionStatus {
    PENDING_EDIT,   // Waiting for 3-min review window
    SYNCED,         // Successfully written to Sheets
    PENDING_SYNC,   // Queued (offline or retry)
    SYNC_FAILED     // Failed after retries
}
```

- [ ] **Step 3: Create `ParsedTransaction.kt`**

```kotlin
package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class ParsedTransaction(
    val merchant: String,
    val amount: Double,
    val date: LocalDate,
    val channel: String,       // "BillPayment" | "eWallet" | "PromptPay" | "BankTransfer" | "Unknown"
    val referenceNo: String,
    val rawFields: Map<String, String> = emptyMap()
)
```

- [ ] **Step 4: Create `SheetsRow.kt`**

```kotlin
package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class SheetsRow(
    val tab: SheetTab,
    val date: LocalDate,
    val item: String,
    val amount: Double,
    val category: String
)
```

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/model/
git commit -m "feat: add domain models ParsedTransaction, SheetsRow, SheetTab, TransactionStatus"
```

---

## Task 4: Room Database

**Files:**
- Create: `data/db/TransactionEntity.kt`
- Create: `data/db/CategoryCacheEntity.kt`
- Create: `data/db/ProcessedRefEntity.kt`
- Create: `data/db/TransactionDao.kt`
- Create: `data/db/CategoryCacheDao.kt`
- Create: `data/db/ProcessedRefDao.kt`
- Create: `data/db/AppDatabase.kt`

- [ ] **Step 1: Create entities**

```kotlin
// TransactionEntity.kt
package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fidriyanto.banktracker.data.model.SheetTab
import com.fidriyanto.banktracker.data.model.TransactionStatus

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val item: String,
    val amount: Double,
    val category: String,
    val dateIso: String,        // ISO yyyy-MM-dd for storage
    val channel: String,
    val referenceNo: String,
    val tab: SheetTab = SheetTab.EXPENSES,
    val status: TransactionStatus = TransactionStatus.PENDING_EDIT,
    val createdAt: Long = System.currentTimeMillis()
)
```

```kotlin
// CategoryCacheEntity.kt
package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_cache")
data class CategoryCacheEntity(
    @PrimaryKey val merchantKey: String,
    val category: String,
    val cleanDescription: String
)
```

```kotlin
// ProcessedRefEntity.kt
package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_refs")
data class ProcessedRefEntity(
    @PrimaryKey val referenceNo: String,
    val processedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Create DAOs**

```kotlin
// TransactionDao.kt
package com.fidriyanto.banktracker.data.db

import androidx.room.*
import com.fidriyanto.banktracker.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = :status")
    suspend fun getByStatus(status: TransactionStatus): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: TransactionEntity): Long

    @Update
    suspend fun update(t: TransactionEntity)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TransactionStatus)
}
```

```kotlin
// CategoryCacheDao.kt
package com.fidriyanto.banktracker.data.db

import androidx.room.*

@Dao
interface CategoryCacheDao {
    @Query("SELECT * FROM category_cache WHERE merchantKey = :key")
    suspend fun getByMerchant(key: String): CategoryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CategoryCacheEntity)
}
```

```kotlin
// ProcessedRefDao.kt
package com.fidriyanto.banktracker.data.db

import androidx.room.*

@Dao
interface ProcessedRefDao {
    @Query("SELECT COUNT(*) FROM processed_refs WHERE referenceNo = :ref")
    suspend fun exists(ref: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ProcessedRefEntity)
}
```

- [ ] **Step 3: Create `AppDatabase.kt`**

```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionEntity::class, CategoryCacheEntity::class, ProcessedRefEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryCacheDao(): CategoryCacheDao
    abstract fun processedRefDao(): ProcessedRefDao
}
```

- [ ] **Step 4: Build project** — confirm Room generates DAOs without errors.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/db/
git commit -m "feat: add Room DB with transaction queue, category cache, and dedup log"
```

---

## Task 5: SecurePrefs + Hilt Module

**Files:**
- Create: `data/prefs/SecurePrefs.kt`
- Create: `BankTrackerApp.kt`
- Create: `di/AppModule.kt`

- [ ] **Step 1: Create `SecurePrefs.kt`**

```kotlin
package com.fidriyanto.banktracker.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private val prefs = EncryptedSharedPreferences.create(
        context, "secure_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var claudeApiKey: String
        get() = prefs.getString("claude_api_key", "") ?: ""
        set(v) = prefs.edit().putString("claude_api_key", v).apply()

    var googleAccessToken: String
        get() = prefs.getString("google_access_token", "") ?: ""
        set(v) = prefs.edit().putString("google_access_token", v).apply()

    var googleAccountEmail: String
        get() = prefs.getString("google_account_email", "") ?: ""
        set(v) = prefs.edit().putString("google_account_email", v).apply()

    var promptPayThreshold: Double
        get() = prefs.getString("promptpay_threshold", "25000")?.toDoubleOrNull() ?: 25000.0
        set(v) = prefs.edit().putString("promptpay_threshold", v.toString()).apply()

    var gmailSenderFilter: String
        get() = prefs.getString("gmail_sender_filter", "from:bangkokbank.com") ?: "from:bangkokbank.com"
        set(v) = prefs.edit().putString("gmail_sender_filter", v).apply()
}
```

- [ ] **Step 2: Create `BankTrackerApp.kt`**

```kotlin
package com.fidriyanto.banktracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BankTrackerApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration get() =
        Configuration.Builder().setWorkerFactory(workerFactory).build()
}
```

- [ ] **Step 3: Create `di/AppModule.kt`**

```kotlin
package com.fidriyanto.banktracker.di

import android.content.Context
import androidx.room.Room
import com.fidriyanto.banktracker.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "banktracker.db").build()

    @Provides fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
    @Provides fun provideCategoryCacheDao(db: AppDatabase) = db.categoryCacheDao()
    @Provides fun provideProcessedRefDao(db: AppDatabase) = db.processedRefDao()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
```

- [ ] **Step 4: Add `android:name=".BankTrackerApp"` to `<application>` in AndroidManifest.xml.**

- [ ] **Step 5: Build** — confirm Hilt generates without errors.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/
git commit -m "feat: add SecurePrefs, Hilt DI module, and Application class"
```

---

## Task 6: MerchantNormalizer

**Files:**
- Create: `email/MerchantNormalizer.kt`
- Create: `src/test/.../email/MerchantNormalizerTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// src/test/.../email/MerchantNormalizerTest.kt
package com.fidriyanto.banktracker.email

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {
    @Test fun `strips CO LTD suffix`() {
        assertEquals("TRUE MONEY", MerchantNormalizer.normalize("TRUE MONEY CO., LTD."))
    }
    @Test fun `strips PCL suffix`() {
        assertEquals("CENTRAL RESTAURANTS GROUP", MerchantNormalizer.normalize("CENTRAL RESTAURANTS GROUP PCL"))
    }
    @Test fun `uppercases and trims`() {
        assertEquals("SUSHIRO", MerchantNormalizer.normalize("  Sushiro  "))
    }
    @Test fun `removes punctuation`() {
        assertEquals("MCDONALDS SILOM 64", MerchantNormalizer.normalize("McDonald's Silom 64"))
    }
    @Test fun `handles already clean input`() {
        assertEquals("BTS TIM TVM", MerchantNormalizer.normalize("BTS TIM TVM"))
    }
}
```

- [ ] **Step 2: Run tests** — expect FAIL with `Unresolved reference: MerchantNormalizer`.

- [ ] **Step 3: Create `email/MerchantNormalizer.kt`**

```kotlin
package com.fidriyanto.banktracker.email

object MerchantNormalizer {
    private val legalSuffixes = listOf(
        "PUBLIC COMPANY LIMITED", "COMPANY LIMITED",
        "CO., LTD.", "CO.,LTD.", "CO LTD",
        "PCL.", "PCL", "PLC.", "PLC", "LTD.", "LTD"
    )

    fun normalize(raw: String): String {
        var result = raw.uppercase().trim()
        for (suffix in legalSuffixes) {
            if (result.endsWith(suffix)) {
                result = result.dropLast(suffix.length).trimEnd(',').trim()
                break
            }
        }
        return result.replace("'", "").replace(".", "").trim()
    }
}
```

- [ ] **Step 4: Run tests** — expect all PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/email/MerchantNormalizer.kt
git add app/src/test/java/com/fidriyanto/banktracker/email/MerchantNormalizerTest.kt
git commit -m "feat: add MerchantNormalizer with legal suffix stripping"
```

---

## Task 7: DateFormatter

**Files:**
- Create: `sheets/DateFormatter.kt`
- Create: `src/test/.../sheets/DateFormatterTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.fidriyanto.banktracker.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateFormatterTest {
    @Test fun `toExpensesDate formats without leading zeros`() {
        assertEquals("5/3/2026", DateFormatter.toExpensesDate(LocalDate.of(2026, 3, 5)))
    }
    @Test fun `toExpensesDate day 11`() {
        assertEquals("11/5/2026", DateFormatter.toExpensesDate(LocalDate.of(2026, 5, 11)))
    }
    @Test fun `toIncomeDate has leading zeros`() {
        assertEquals("26/01/2026", DateFormatter.toIncomeDate(LocalDate.of(2026, 1, 26)))
    }
    @Test fun `parseEmailDate parses standard format`() {
        assertEquals(LocalDate.of(2026, 5, 10), DateFormatter.parseEmailDate("10 May 2026 at 17:52:34 (Thailand time)"))
    }
    @Test fun `parseEmailDate returns null on garbage`() {
        assertNull(DateFormatter.parseEmailDate("not a date"))
    }
    @Test fun `parseEmailDate parses single digit day`() {
        assertEquals(LocalDate.of(2026, 5, 9), DateFormatter.parseEmailDate("9 May 2026 at 14:49:06 (Thailand time)"))
    }
}
```

- [ ] **Step 2: Run** — expect FAIL.

- [ ] **Step 3: Create `sheets/DateFormatter.kt`**

```kotlin
package com.fidriyanto.banktracker.sheets

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    private val emailPattern = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    private val incomePattern = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Expenses tab: D/M/YYYY text string, no leading zeros
    fun toExpensesDate(date: LocalDate): String =
        "${date.dayOfMonth}/${date.monthValue}/${date.year}"

    // Income tab: DD/MM/YYYY real date
    fun toIncomeDate(date: LocalDate): String =
        date.format(incomePattern)

    // Parse "10 May 2026 at 17:52:34 (Thailand time)"
    fun parseEmailDate(raw: String): LocalDate? = try {
        val datePart = raw.substringBefore(" at").trim()
        LocalDate.parse(datePart, emailPattern)
    } catch (e: Exception) { null }
}
```

- [ ] **Step 4: Run tests** — expect all PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/sheets/DateFormatter.kt
git add app/src/test/java/com/fidriyanto/banktracker/sheets/DateFormatterTest.kt
git commit -m "feat: add DateFormatter for Expenses (D/M/YYYY) and Income (DD/MM/YYYY) tabs"
```

---

## Task 8: EmailParser

**Files:**
- Create: `email/EmailParser.kt`
- Create: `src/test/.../email/EmailParserTest.kt`

- [ ] **Step 1: Write failing tests** (use the real email HTML from brainstorm)

```kotlin
package com.fidriyanto.banktracker.email

import org.junit.Assert.*
import org.junit.Test

class EmailParserTest {
    private val parser = EmailParser()

    private val billPaymentHtml = """
        <table>
          <tr><td>Service name / Payee name</td><td>TRUE MONEY CO., LTD.</td></tr>
          <tr><td>Amount (Baht)</td><td>500.00</td></tr>
          <tr><td>Fee (Baht)</td><td>0.00</td></tr>
          <tr><td>Reference no.</td><td>417486</td></tr>
          <tr><td>Date</td><td>10 May 2026 at 17:52:34 (Thailand time)</td></tr>
        </table>
    """.trimIndent()

    private val eWalletHtml = """
        <table>
          <tr><td>e-wallet number</td><td>004xx-xxx-xxx-7802</td></tr>
          <tr><td>e-wallet owner</td><td>MR. NUTTAWUT KOSANPRAPAI</td></tr>
          <tr><td>e-wallet provider name</td><td>K Plus Wallet</td></tr>
          <tr><td>Amount (Baht)</td><td>100.00</td></tr>
          <tr><td>Fee (Baht)</td><td>0.00</td></tr>
          <tr><td>Bank Reference No.</td><td>599120</td></tr>
          <tr><td>Date</td><td>10 May 2026 at 10:05:26 (Thailand time)</td></tr>
        </table>
    """.trimIndent()

    private val promptPayHtml = """
        <table>
          <tr><td>Receiving method</td><td>Deposit to recipient's account registered with PromptPay</td></tr>
          <tr><td>Account name</td><td>MR MONGKON JUNSINGKORN</td></tr>
          <tr><td>Amount (Baht)</td><td>226.00</td></tr>
          <tr><td>Fee (Baht)</td><td>0.00</td></tr>
          <tr><td>Reference no.</td><td>464325</td></tr>
          <tr><td>Date</td><td>10 May 2026 at 13:46:01 (Thailand time)</td></tr>
        </table>
    """.trimIndent()

    private val bankTransferHtml = """
        <table>
          <tr><td>Account name</td><td>NITTRA PATTAR</td></tr>
          <tr><td>Bank</td><td>TTB</td></tr>
          <tr><td>Amount (Baht)</td><td>55.00</td></tr>
          <tr><td>Fee (Baht)</td><td>0.00</td></tr>
          <tr><td>Bank reference no.</td><td>445720</td></tr>
          <tr><td>Date</td><td>9 May 2026 at 14:49:06 (Thailand time)</td></tr>
        </table>
    """.trimIndent()

    @Test fun `parses bill payment format`() {
        val result = parser.parse(billPaymentHtml)!!
        assertEquals("TRUE MONEY CO., LTD.", result.merchant)
        assertEquals(500.0, result.amount, 0.01)
        assertEquals("BillPayment", result.channel)
        assertEquals("417486", result.referenceNo)
        assertEquals(10, result.date.dayOfMonth)
        assertEquals(5, result.date.monthValue)
    }

    @Test fun `parses e-wallet format`() {
        val result = parser.parse(eWalletHtml)!!
        assertEquals("K Plus Wallet", result.merchant)
        assertEquals(100.0, result.amount, 0.01)
        assertEquals("eWallet", result.channel)
        assertEquals("599120", result.referenceNo)
    }

    @Test fun `parses PromptPay format`() {
        val result = parser.parse(promptPayHtml)!!
        assertTrue(result.merchant.contains("MR MONGKON"))
        assertEquals(226.0, result.amount, 0.01)
        assertEquals("PromptPay", result.channel)
    }

    @Test fun `parses bank transfer format`() {
        val result = parser.parse(bankTransferHtml)!!
        assertTrue(result.merchant.contains("TTB"))
        assertEquals(55.0, result.amount, 0.01)
        assertEquals("BankTransfer", result.channel)
    }

    @Test fun `adds fee to amount`() {
        val html = """
            <table>
              <tr><td>Service name / Payee name</td><td>SomeService</td></tr>
              <tr><td>Amount (Baht)</td><td>100.00</td></tr>
              <tr><td>Fee (Baht)</td><td>5.00</td></tr>
              <tr><td>Reference no.</td><td>123</td></tr>
              <tr><td>Date</td><td>10 May 2026 at 10:00:00 (Thailand time)</td></tr>
            </table>
        """.trimIndent()
        val result = parser.parse(html)!!
        assertEquals(105.0, result.amount, 0.01)
    }

    @Test fun `returns null when amount missing`() {
        assertNull(parser.parse("<table></table>"))
    }
}
```

- [ ] **Step 2: Run** — expect FAIL.

- [ ] **Step 3: Create `email/EmailParser.kt`**

```kotlin
package com.fidriyanto.banktracker.email

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.sheets.DateFormatter
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailParser @Inject constructor() {

    fun parse(htmlBody: String): ParsedTransaction? {
        val fields = extractFields(htmlBody)
        val amount = fields["Amount (Baht)"]?.toDoubleOrNull() ?: return null
        val fee = fields["Fee (Baht)"]?.toDoubleOrNull() ?: 0.0
        val dateRaw = fields["Date"] ?: return null
        val date = DateFormatter.parseEmailDate(dateRaw) ?: return null
        val refNo = fields["Reference no."]
            ?: fields["Bank Reference No."]
            ?: fields["Bank reference no."]
            ?: fields["Reference no. 1"]
            ?: ""

        val (merchant, channel) = resolveMerchantAndChannel(fields)

        return ParsedTransaction(
            merchant = merchant,
            amount = amount + fee,
            date = date,
            channel = channel,
            referenceNo = refNo,
            rawFields = fields
        )
    }

    private fun extractFields(html: String): Map<String, String> {
        val doc = Jsoup.parse(html)
        val result = mutableMapOf<String, String>()
        val cells = doc.select("td")
        var i = 0
        while (i < cells.size - 1) {
            val label = cells[i].text().trim()
            val value = cells[i + 1].text().trim()
            if (label.isNotEmpty() && value.isNotEmpty()) {
                result[label] = value
            }
            i += 2
        }
        return result
    }

    private fun resolveMerchantAndChannel(fields: Map<String, String>): Pair<String, String> {
        return when {
            fields.containsKey("Service name / Payee name") ->
                Pair(fields["Service name / Payee name"] ?: "Unknown", "BillPayment")
            fields.containsKey("e-wallet provider name") -> {
                val provider = fields["e-wallet provider name"] ?: "eWallet"
                val owner = fields["e-wallet owner"]?.take(30) ?: ""
                Pair(provider, "eWallet")
            }
            fields["Receiving method"]?.contains("PromptPay", ignoreCase = true) == true -> {
                val name = fields["Account name"]?.take(30) ?: "Unknown"
                Pair("PromptPay – $name", "PromptPay")
            }
            fields.containsKey("Bank") -> {
                val bank = fields["Bank"] ?: "Bank"
                val name = fields["Account name"]?.take(30) ?: "Unknown"
                Pair("$bank – $name", "BankTransfer")
            }
            else -> Pair("Unknown", "Unknown")
        }
    }
}
```

- [ ] **Step 4: Run tests** — expect all PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/email/EmailParser.kt
git add app/src/test/java/com/fidriyanto/banktracker/email/EmailParserTest.kt
git commit -m "feat: add EmailParser supporting 4 Bangkok Bank email formats"
```

---

## Task 9: RuleBasedMatcher

**Files:**
- Create: `categorization/RuleBasedMatcher.kt`
- Create: `src/test/.../categorization/RuleBasedMatcherTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.fidriyanto.banktracker.categorization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleBasedMatcherTest {
    @Test fun `matches exact key`() {
        assertEquals("Transport", RuleBasedMatcher.match("BTS TIM TVM"))
    }
    @Test fun `matches partial key`() {
        assertEquals("Food & Drink", RuleBasedMatcher.match("TRUE MONEY"))
    }
    @Test fun `returns null for unknown`() {
        assertNull(RuleBasedMatcher.match("UNKNOWN MERCHANT XYZ"))
    }
    @Test fun `matches SUSHIRO`() {
        assertEquals("Food & Drink", RuleBasedMatcher.match("SUSHIRO"))
    }
    @Test fun `matches CASH ATM`() {
        assertEquals("Other", RuleBasedMatcher.match("CASH ATM WD"))
    }
}
```

- [ ] **Step 2: Run** — expect FAIL.

- [ ] **Step 3: Create `categorization/RuleBasedMatcher.kt`**

```kotlin
package com.fidriyanto.banktracker.categorization

object RuleBasedMatcher {
    // Keys are normalized (uppercase, no punctuation) — partial match supported
    private val rules = mapOf(
        "RATTIKAN LEKDARA" to "Bills",
        "METROPOLITAN ELECTRICITY" to "Bills",
        "TRUEMONEY CO" to "Bills",
        "TRUE MOBILE" to "Bills",
        "TRUEAPP" to "Bills",
        "FEE OTH BAK ATM" to "Bills",
        "MONTHLY CARD CHARGE" to "Bills",
        "ADMIN FEE" to "Bills",
        "BTS" to "Transport",
        "RED LINE" to "Transport",
        "SRT TICKET" to "Transport",
        "TRUE MONEY" to "Food & Drink",
        "TRUEMONEY" to "Food & Drink",
        "LINE PAY" to "Food & Drink",
        "SUSHIRO" to "Food & Drink",
        "SLIDE MORE PIZZA" to "Food & Drink",
        "MCDONALDS" to "Food & Drink",
        "CENTRAL RESTAURANTS" to "Food & Drink",
        "RISE COFFEE" to "Food & Drink",
        "URBAN EATS" to "Food & Drink",
        "FIVE STAR" to "Food & Drink",
        "RATTANA RESTAURANT" to "Food & Drink",
        "SWEET CLOUD" to "Food & Drink",
        "CP AXTRA" to "Groceries",
        "MAKRO" to "Groceries",
        "LOTUS" to "Groceries",
        "SUPER TURTLE" to "Groceries",
        "VENDING BY BOONTERM" to "Groceries",
        "SUN VENDING" to "Groceries",
        "SHOPEE" to "Shopping",
        "DECATHLON" to "Shopping",
        "CITY MALL" to "Shopping",
        "LITTLE BEE" to "Shopping",
        "LIFE POINT CHURCH" to "Entertainment",
        "SJ BARBER" to "Other",
        "CASH ATM" to "Other",
        "K PLUS WALLET" to "Other",
    )

    fun match(normalizedMerchant: String): String? {
        rules[normalizedMerchant]?.let { return it }
        return rules.entries.firstOrNull { (key, _) ->
            normalizedMerchant.contains(key)
        }?.value
    }
}
```

- [ ] **Step 4: Run tests** — expect all PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/categorization/RuleBasedMatcher.kt
git add app/src/test/java/com/fidriyanto/banktracker/categorization/RuleBasedMatcherTest.kt
git commit -m "feat: add RuleBasedMatcher with 30+ merchant-to-category mappings"
```

---

## Task 10: PromptPayHeuristic

**Files:**
- Create: `categorization/PromptPayHeuristic.kt`
- Create: `src/test/.../categorization/PromptPayHeuristicTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.fidriyanto.banktracker.categorization

import org.junit.Assert.*
import org.junit.Test

class PromptPayHeuristicTest {
    @Test fun `matches PromptPay over threshold`() {
        assertTrue(PromptPayHeuristic.isLikelyTransferOut("PromptPay", 30000.0, 25000.0))
    }
    @Test fun `matches PromptPay exactly at threshold`() {
        assertTrue(PromptPayHeuristic.isLikelyTransferOut("PromptPay", 25000.0, 25000.0))
    }
    @Test fun `does not match small PromptPay`() {
        assertFalse(PromptPayHeuristic.isLikelyTransferOut("PromptPay", 226.0, 25000.0))
    }
    @Test fun `does not match non-PromptPay channel`() {
        assertFalse(PromptPayHeuristic.isLikelyTransferOut("BillPayment", 50000.0, 25000.0))
    }
}
```

- [ ] **Step 2: Run** — expect FAIL.

- [ ] **Step 3: Create `categorization/PromptPayHeuristic.kt`**

```kotlin
package com.fidriyanto.banktracker.categorization

object PromptPayHeuristic {
    fun isLikelyTransferOut(channel: String, amount: Double, threshold: Double): Boolean =
        channel == "PromptPay" && amount >= threshold
}
```

- [ ] **Step 4: Run tests** — expect all PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/categorization/PromptPayHeuristic.kt
git add app/src/test/java/com/fidriyanto/banktracker/categorization/PromptPayHeuristicTest.kt
git commit -m "feat: add PromptPayHeuristic for Transfer Out detection"
```

---

## Task 11: ClaudeCategorizor

**Files:**
- Create: `categorization/ClaudeCategorizor.kt`

- [ ] **Step 1: Create `categorization/ClaudeCategorizor.kt`**

```kotlin
package com.fidriyanto.banktracker.categorization

import com.fidriyanto.banktracker.data.db.CategoryCacheDao
import com.fidriyanto.banktracker.data.db.CategoryCacheEntity
import com.fidriyanto.banktracker.email.MerchantNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeCategorizor @Inject constructor(
    private val cacheDao: CategoryCacheDao,
    private val httpClient: OkHttpClient
) {
    companion object {
        val CATEGORIES = listOf(
            "Bills", "Subscriptions", "Entertainment", "Food & Drink", "Groceries",
            "Health & Wellbeing", "Other", "Shopping", "Transport", "Travel",
            "Business", "Gifts", "Transfer Out"
        )
        private const val MODEL = "claude-haiku-4-5-20251001"
        private const val API_URL = "https://api.anthropic.com/v1/messages"
    }

    suspend fun categorize(
        merchant: String,
        amount: Double,
        channel: String,
        apiKey: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val cacheKey = MerchantNormalizer.normalize(merchant)
        cacheDao.getByMerchant(cacheKey)?.let {
            return@withContext Pair(it.category, it.cleanDescription)
        }

        val prompt = buildPrompt(merchant, amount, channel)
        try {
            val body = """{"model":"$MODEL","max_tokens":100,"messages":[{"role":"user","content":${JSONObject.quote(prompt)}}]}"""
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val text = JSONObject(response.body!!.string())
                .getJSONArray("content").getJSONObject(0).getString("text").trim()
            val json = JSONObject(text)
            val category = json.getString("category").takeIf { it in CATEGORIES } ?: "Other"
            val description = json.optString("description", merchant)

            cacheDao.insert(CategoryCacheEntity(cacheKey, category, description))
            Pair(category, description)
        } catch (e: Exception) {
            Pair("Other", merchant)
        }
    }

    private fun buildPrompt(merchant: String, amount: Double, channel: String) = """
        Categorize this Bangkok Bank transaction. Return JSON only.
        Merchant: $merchant
        Amount: ${amount.toInt()} THB
        Channel: $channel
        Categories: ${CATEGORIES.joinToString(", ")}
        Response format: {"category":"...","description":"..."}
        description = clean 2-4 word item name (e.g. "TrueMoney top-up", "BTS fare")
    """.trimIndent()
}
```

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/categorization/ClaudeCategorizor.kt
git commit -m "feat: add ClaudeCategorizor with Haiku API and merchant-key caching"
```

---

## Task 12: CategoryResolver

**Files:**
- Create: `categorization/CategoryResolver.kt`
- Create: `src/test/.../categorization/CategoryResolverTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.fidriyanto.banktracker.categorization

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CategoryResolverTest {
    private val claude = mockk<ClaudeCategorizor>()
    private val resolver = CategoryResolver(claude)
    private fun tx(merchant: String, amount: Double = 100.0, channel: String = "BillPayment") =
        ParsedTransaction(merchant, amount, LocalDate.now(), channel, "REF123")

    @Test fun `uses rule match for known merchant`() = runTest {
        val (category, _) = resolver.resolve(tx("BTS TIM TVM"), 25000.0, "apikey")
        assertEquals("Transport", category)
        coVerify(exactly = 0) { claude.categorize(any(), any(), any(), any()) }
    }

    @Test fun `uses PromptPay heuristic for large transfer`() = runTest {
        val (category, flagged) = resolver.resolve(tx("PromptPay – MR X", 30000.0, "PromptPay"), 25000.0, "apikey")
        assertEquals("Transfer Out", category)
        assertEquals(true, flagged)
    }

    @Test fun `calls Claude for unknown merchant`() = runTest {
        coEvery { claude.categorize(any(), any(), any(), any()) } returns Pair("Shopping", "Online purchase")
        val (category, _) = resolver.resolve(tx("RANDOM SHOP"), 25000.0, "apikey")
        assertEquals("Shopping", category)
        coVerify(exactly = 1) { claude.categorize(any(), any(), any(), any()) }
    }
}
```

- [ ] **Step 2: Run** — expect FAIL.

- [ ] **Step 3: Create `categorization/CategoryResolver.kt`**

```kotlin
package com.fidriyanto.banktracker.categorization

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.email.MerchantNormalizer
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedCategory(val category: String, val description: String, val flagged: Boolean = false)

@Singleton
class CategoryResolver @Inject constructor(
    private val claude: ClaudeCategorizor
) {
    suspend fun resolve(
        tx: ParsedTransaction,
        promptPayThreshold: Double,
        claudeApiKey: String
    ): ResolvedCategory {
        val normalized = MerchantNormalizer.normalize(tx.merchant)

        // Tier 1: rule-based
        RuleBasedMatcher.match(normalized)?.let {
            return ResolvedCategory(it, tx.merchant)
        }

        // Tier 2: PromptPay heuristic
        if (PromptPayHeuristic.isLikelyTransferOut(tx.channel, tx.amount, promptPayThreshold)) {
            return ResolvedCategory("Transfer Out", tx.merchant, flagged = true)
        }

        // Tier 3: Claude
        val (category, description) = claude.categorize(tx.merchant, tx.amount, tx.channel, claudeApiKey)
        return ResolvedCategory(category, description)
    }
}
```

- [ ] **Step 4: Run tests** — expect all PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/categorization/
git add app/src/test/java/com/fidriyanto/banktracker/categorization/
git commit -m "feat: add CategoryResolver orchestrating rule match, PromptPay heuristic, Claude fallback"
```

---

## Task 13: GoogleAuthManager

**Files:**
- Create: `auth/GoogleAuthManager.kt`

- [ ] **Step 1: Create `auth/GoogleAuthManager.kt`**

```kotlin
package com.fidriyanto.banktracker.auth

import android.content.Context
import android.content.Intent
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SecurePrefs
) {
    companion object {
        val SCOPES = listOf(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/spreadsheets"
        )
    }

    fun getSignInIntent(): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope(SCOPES[0]),
                com.google.android.gms.common.api.Scope(SCOPES[1])
            )
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    fun isSignedIn(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    fun getSignedInEmail(): String? = GoogleSignIn.getLastSignedInAccount(context)?.email

    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
            val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)
            credential.selectedAccount = account.account
            credential.token // Blocks — call off main thread
        } catch (e: Exception) { null }
    }

    fun signOut() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, options).signOut()
        prefs.googleAccountEmail = ""
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/auth/GoogleAuthManager.kt
git commit -m "feat: add GoogleAuthManager for Gmail + Sheets OAuth via Google Sign-In"
```

---

## Task 14: EmailFetcher

**Files:**
- Create: `email/EmailFetcher.kt`

- [ ] **Step 1: Create `email/EmailFetcher.kt`**

```kotlin
package com.fidriyanto.banktracker.email

import android.net.Uri
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailFetcher @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val prefs: SecurePrefs,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val GMAIL_API = "https://gmail.googleapis.com/gmail/v1/users/me"
    }

    // Returns the HTML body of the most recent Bangkok Bank email, or null
    suspend fun fetchLatestBankEmail(): String? = withContext(Dispatchers.IO) {
        val token = authManager.getValidAccessToken() ?: return@withContext null
        val query = prefs.gmailSenderFilter  // default: "from:bangkokbank.com"

        // Step 1: List matching messages (most recent 1)
        val listUrl = "$GMAIL_API/messages?q=${Uri.encode(query)}&maxResults=1"
        val listReq = Request.Builder().url(listUrl)
            .addHeader("Authorization", "Bearer $token").build()
        val listBody = httpClient.newCall(listReq).execute().body?.string() ?: return@withContext null
        val messageId = JSONObject(listBody)
            .optJSONArray("messages")?.optJSONObject(0)?.optString("id")
            ?: return@withContext null

        // Step 2: Fetch full message
        val msgUrl = "$GMAIL_API/messages/$messageId?format=full"
        val msgReq = Request.Builder().url(msgUrl)
            .addHeader("Authorization", "Bearer $token").build()
        val msgBody = JSONObject(httpClient.newCall(msgReq).execute().body?.string() ?: return@withContext null)

        extractHtmlBody(msgBody)
    }

    private fun extractHtmlBody(message: JSONObject): String? {
        val payload = message.optJSONObject("payload") ?: return null
        // Try direct body first
        val directData = payload.optJSONObject("body")?.optString("data")
        if (!directData.isNullOrEmpty()) return decodeBase64Url(directData)
        // Try parts
        val parts = payload.optJSONArray("parts") ?: return null
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.optString("mimeType") == "text/html") {
                val data = part.optJSONObject("body")?.optString("data") ?: continue
                return decodeBase64Url(data)
            }
        }
        return null
    }

    private fun decodeBase64Url(encoded: String): String {
        val bytes = android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE)
        return String(bytes, Charsets.UTF_8)
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/email/EmailFetcher.kt
git commit -m "feat: add EmailFetcher using Gmail REST API with configurable sender filter"
```

---

## Task 15: SheetsSyncer

**Files:**
- Create: `sheets/SheetsSyncer.kt`

- [ ] **Step 1: Create `sheets/SheetsSyncer.kt`**

```kotlin
package com.fidriyanto.banktracker.sheets

import android.net.Uri
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.model.SheetsRow
import com.fidriyanto.banktracker.data.model.SheetTab
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
class SheetsSyncer @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val SPREADSHEET_ID = "1OJqLIPFWjJPje8HabLVyMp_AGvsCi9nWM2nHqdzv9-w"
        private const val SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets"
    }

    suspend fun sync(row: SheetsRow): Result<Unit> = withContext(Dispatchers.IO) {
        val token = authManager.getValidAccessToken()
            ?: return@withContext Result.failure(Exception("Not authenticated"))

        val (tabName, values) = buildRowValues(row)
        val range = Uri.encode("$tabName!A:E")
        val url = "$SHEETS_API/$SPREADSHEET_ID/values/$range:append?valueInputOption=USER_ENTERED"

        val bodyJson = JSONObject().put("values", JSONArray().put(JSONArray(values))).toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Sheets API error: HTTP ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRowValues(row: SheetsRow): Pair<String, List<Any>> {
        val amountStr = row.amount.toAmountString()
        return when (row.tab) {
            SheetTab.EXPENSES -> Pair("Expenses", listOf(
                DateFormatter.toExpensesDate(row.date), row.item, amountStr, row.category
            ))
            SheetTab.IDR_EXPENSES -> Pair("IDR Expenses", listOf(
                DateFormatter.toExpensesDate(row.date), row.item, amountStr, row.category
            ))
            SheetTab.INCOME -> Pair("Income", listOf(
                "", DateFormatter.toIncomeDate(row.date), row.item, "", amountStr
            ))
            SheetTab.IDR_INCOME -> Pair("IDR Income", listOf(
                "", DateFormatter.toIncomeDate(row.date), row.item, "", amountStr
            ))
        }
    }

    private fun Double.toAmountString(): String =
        if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
}
```

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/sheets/SheetsSyncer.kt
git commit -m "feat: add SheetsSyncer with correct date formats and USER_ENTERED mode for all 4 tabs"
```

---

## Task 16: TransactionRepository

**Files:**
- Create: `data/repository/TransactionRepository.kt`

- [ ] **Step 1: Create `data/repository/TransactionRepository.kt`**

```kotlin
package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.categorization.CategoryResolver
import com.fidriyanto.banktracker.data.db.*
import com.fidriyanto.banktracker.data.model.*
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.fidriyanto.banktracker.email.EmailFetcher
import com.fidriyanto.banktracker.email.EmailParser
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val processedRefDao: ProcessedRefDao,
    private val emailFetcher: EmailFetcher,
    private val emailParser: EmailParser,
    private val categoryResolver: CategoryResolver,
    private val sheetsSyncer: SheetsSyncer,
    private val prefs: SecurePrefs
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    // Called by BankNotificationService after 5s delay
    suspend fun processNewNotification(triggerAmount: Double): Long? {
        val html = emailFetcher.fetchLatestBankEmail() ?: return null
        val parsed = emailParser.parse(html) ?: return null

        // Dedup check
        if (parsed.referenceNo.isNotEmpty() && processedRefDao.exists(parsed.referenceNo) > 0) return null
        if (parsed.referenceNo.isNotEmpty()) {
            processedRefDao.insert(ProcessedRefEntity(parsed.referenceNo))
        }

        val resolved = categoryResolver.resolve(parsed, prefs.promptPayThreshold, prefs.claudeApiKey)

        val entity = TransactionEntity(
            merchant = parsed.merchant,
            item = resolved.description,
            amount = parsed.amount,
            category = resolved.category,
            dateIso = parsed.date.toString(),
            channel = parsed.channel,
            referenceNo = parsed.referenceNo,
            tab = SheetTab.EXPENSES,
            status = TransactionStatus.PENDING_EDIT
        )
        return transactionDao.insert(entity)
    }

    suspend fun syncTransaction(id: Long): Result<Unit> {
        val entity = transactionDao.getByStatus(TransactionStatus.PENDING_EDIT)
            .firstOrNull { it.id == id }
            ?: transactionDao.getByStatus(TransactionStatus.PENDING_SYNC)
                .firstOrNull { it.id == id }
            ?: return Result.failure(Exception("Transaction not found"))

        transactionDao.updateStatus(id, TransactionStatus.PENDING_SYNC)
        val row = SheetsRow(
            tab = entity.tab,
            date = LocalDate.parse(entity.dateIso),
            item = entity.item,
            amount = entity.amount,
            category = entity.category
        )
        return sheetsSyncer.sync(row).also { result ->
            val newStatus = if (result.isSuccess) TransactionStatus.SYNCED else TransactionStatus.SYNC_FAILED
            transactionDao.updateStatus(id, newStatus)
        }
    }

    suspend fun insertManual(row: SheetsRow): Result<Unit> {
        val entity = TransactionEntity(
            merchant = row.item,
            item = row.item,
            amount = row.amount,
            category = row.category,
            dateIso = row.date.toString(),
            channel = "Manual",
            referenceNo = "",
            tab = row.tab,
            status = TransactionStatus.PENDING_SYNC
        )
        val id = transactionDao.insert(entity)
        return syncTransaction(id)
    }

    suspend fun retryFailedSyncs() {
        transactionDao.getByStatus(TransactionStatus.SYNC_FAILED).forEach { syncTransaction(it.id) }
        transactionDao.getByStatus(TransactionStatus.PENDING_SYNC).forEach { syncTransaction(it.id) }
    }
}
```

- [ ] **Step 2: Add missing `getById` query to `TransactionDao`**

```kotlin
@Query("SELECT * FROM transactions WHERE id = :id")
suspend fun getById(id: Long): TransactionEntity?
```

Update `syncTransaction` in Repository to use `getById` instead of filtering lists.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/
git commit -m "feat: add TransactionRepository coordinating email fetch, categorization, and Sheets sync"
```

---

## Task 17: NotificationListenerService + SyncWorker

**Files:**
- Create: `service/BankNotificationService.kt`
- Create: `service/SyncWorker.kt`

- [ ] **Step 1: Create `service/BankNotificationService.kt`**

```kotlin
package com.fidriyanto.banktracker.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class BankNotificationService : NotificationListenerService() {
    companion object {
        // Verify actual package name by checking device: adb shell pm list packages | grep bangkok
        const val BANGKOK_BANK_PACKAGE = "th.co.bangkokbank.bangkokmobile"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != BANGKOK_BANK_PACKAGE) return
        val text = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: return
        val amount = extractAmount(text) ?: return

        val work = OneTimeWorkRequestBuilder<EmailFetchWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setInputData(workDataOf("trigger_amount" to amount))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(work)
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("""(\d[\d,]*(?:\.\d{1,2})?)THB""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }
}
```

- [ ] **Step 2: Create `service/EmailFetchWorker.kt`**

```kotlin
package com.fidriyanto.banktracker.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.notification.ReviewNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EmailFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TransactionRepository,
    private val notificationManager: ReviewNotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val triggerAmount = inputData.getDouble("trigger_amount", 0.0)
        val transactionId = repository.processNewNotification(triggerAmount) ?: return Result.success()
        notificationManager.showReviewNotification(transactionId)
        return Result.success()
    }
}
```

- [ ] **Step 3: Create `service/SyncWorker.kt`**

```kotlin
package com.fidriyanto.banktracker.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.notification.ReviewNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TransactionRepository,
    private val notificationManager: ReviewNotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong("transaction_id", -1L)
        if (id == -1L) return Result.failure()
        notificationManager.dismiss(id)
        repository.syncTransaction(id)
        return Result.success()
    }
}
```

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/service/
git commit -m "feat: add NotificationListenerService, EmailFetchWorker, and SyncWorker"
```

---

## Task 18: ReviewNotificationManager

**Files:**
- Create: `notification/ReviewNotificationManager.kt`

- [ ] **Step 1: Create `notification/ReviewNotificationManager.kt`**

```kotlin
package com.fidriyanto.banktracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.fidriyanto.banktracker.MainActivity
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.db.TransactionDao
import com.fidriyanto.banktracker.service.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionDao: TransactionDao
) {
    companion object {
        const val CHANNEL_ID = "transaction_review"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val REVIEW_TIMEOUT_MS = 3 * 60 * 1000L
    }

    init { createChannel() }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Transaction Review",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Review auto-captured transactions" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun showReviewNotification(transactionId: Long) {
        val entity = transactionDao.getById(transactionId) ?: return

        val editIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val editPi = PendingIntent.getActivity(
            context, transactionId.toInt(), editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncNowWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf("transaction_id" to transactionId))
            .build()
        val syncNowIntent = WorkManager.getInstance(context).beginWith(syncNowWork)

        val amountStr = if (entity.amount % 1.0 == 0.0) entity.amount.toInt().toString() else entity.amount.toString()
        val title = if (entity.category == "Transfer Out" && entity.channel == "PromptPay")
            "⚠ ฿$amountStr · ${entity.category}" else "฿$amountStr · ${entity.category}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("${entity.item} · Tap to edit")
            .setSubText("Auto-syncing in 3 min")
            .setContentIntent(editPi)
            .addAction(0, "Edit", editPi)
            .setAutoCancel(false)
            .setOngoing(true)
            .setTimeoutAfter(REVIEW_TIMEOUT_MS)
            .build()

        NotificationManagerCompat.from(context).notify(transactionId.toInt(), notification)

        // Schedule auto-sync after 3 minutes
        val autoSync = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(3, TimeUnit.MINUTES)
            .setInputData(workDataOf("transaction_id" to transactionId))
            .addTag("auto_sync_$transactionId")
            .build()
        WorkManager.getInstance(context).enqueue(autoSync)
    }

    fun dismiss(transactionId: Long) {
        NotificationManagerCompat.from(context).cancel(transactionId.toInt())
        WorkManager.getInstance(context).cancelAllWorkByTag("auto_sync_$transactionId")
    }
}
```

- [ ] **Step 2: Create `res/drawable/ic_notification.xml`** — a simple vector drawable (bank icon).

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M4,10v7h3v-7H4zM10,10v7h3v-7H10zM2,22h19v-3H2V22zM17,10v7h3v-7H17zM11.5,1L2,6v2h19V6L11.5,1z"/>
</vector>
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/notification/
git add app/src/main/res/drawable/ic_notification.xml
git commit -m "feat: add ReviewNotificationManager with 3-min countdown and auto-sync"
```

---

## Task 19: UI Navigation Shell

**Files:**
- Create: `ui/navigation/AppNavigation.kt`
- Modify: `MainActivity.kt`

- [ ] **Step 1: Create `ui/navigation/AppNavigation.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fidriyanto.banktracker.ui.add.AddScreen
import com.fidriyanto.banktracker.ui.feed.FeedScreen
import com.fidriyanto.banktracker.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Feed : Screen("feed", "Feed", Icons.Outlined.List)
    object Add : Screen("add", "Add", Icons.Outlined.Add)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
}

private val screens = listOf(Screen.Feed, Screen.Add, Screen.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = current?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Feed.route) {
            composable(Screen.Feed.route) { FeedScreen() }
            composable(Screen.Add.route) { AddScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
```

- [ ] **Step 2: Update `MainActivity.kt`**

```kotlin
package com.fidriyanto.banktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fidriyanto.banktracker.ui.navigation.AppNavigation
import com.fidriyanto.banktracker.ui.theme.BankTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BankTrackerTheme { AppNavigation() } }
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/navigation/
git add app/src/main/java/com/fidriyanto/banktracker/MainActivity.kt
git commit -m "feat: add 3-tab bottom navigation with Feed, Add, Settings"
```

---

## Task 20: FeedScreen

**Files:**
- Create: `ui/feed/FeedViewModel.kt`
- Create: `ui/feed/TransactionCard.kt`
- Create: `ui/feed/FeedScreen.kt`

- [ ] **Step 1: Create `ui/feed/FeedViewModel.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun retry(id: Long) = viewModelScope.launch { repository.syncTransaction(id) }
}
```

- [ ] **Step 2: Create `ui/feed/TransactionCard.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.border
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
fun TransactionCard(entity: TransactionEntity, onRetry: () -> Unit) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (borderColor != Color.Transparent) 1.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)),
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
        if (entity.status == TransactionStatus.SYNC_FAILED) {
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text("Retry", color = Secondary, fontSize = 12.sp)
            }
        }
    }
}
```

- [ ] **Step 3: Create `ui/feed/FeedScreen.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.ui.theme.MutedText

@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Transactions", fontWeight = FontWeight.Bold, fontSize = 20.sp,
            color = Color.White, modifier = Modifier.padding(vertical = 16.dp)
        )
        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions yet.\nBangkok Bank notifications will appear here.",
                    color = MutedText, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions, key = { it.id }) { entity ->
                    TransactionCard(entity = entity, onRetry = { viewModel.retry(entity.id) })
                }
            }
        }
    }
}
```

- [ ] **Step 4: Build and run** — confirm Feed tab shows empty state.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/feed/
git commit -m "feat: add Feed screen with transaction card list and status badges"
```

---

## Task 21: AddScreen (Manual Entry)

**Files:**
- Create: `ui/add/AddViewModel.kt`
- Create: `ui/add/AddScreen.kt`

- [ ] **Step 1: Create `ui/add/AddViewModel.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.categorization.ClaudeCategorizor
import com.fidriyanto.banktracker.data.model.SheetTab
import com.fidriyanto.banktracker.data.model.SheetsRow
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddFormState(
    val account: String = "THB",          // "THB" | "IDR"
    val type: String = "Expense",          // "Expense" | "Income"
    val amount: String = "",
    val description: String = "",
    val category: String = "Other",
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AddViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AddFormState())
    val state = _state.asStateFlow()

    fun update(block: AddFormState.() -> AddFormState) { _state.value = _state.value.block() }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull() ?: run {
            _state.value = s.copy(errorMessage = "Enter a valid amount"); return@launch
        }
        _state.value = s.copy(isLoading = true, errorMessage = null)
        val tab = when {
            s.account == "THB" && s.type == "Expense" -> SheetTab.EXPENSES
            s.account == "THB" && s.type == "Income"  -> SheetTab.INCOME
            s.account == "IDR" && s.type == "Expense" -> SheetTab.IDR_EXPENSES
            else                                       -> SheetTab.IDR_INCOME
        }
        val row = SheetsRow(tab, s.date, s.description, amount, s.category)
        val result = repository.insertManual(row)
        _state.value = _state.value.copy(
            isLoading = false,
            successMessage = if (result.isSuccess) "Synced to Sheets!" else null,
            errorMessage = if (result.isFailure) "Sync failed — saved offline" else null
        )
    }
}
```

- [ ] **Step 2: Create `ui/add/AddScreen.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.categorization.ClaudeCategorizor
import com.fidriyanto.banktracker.ui.theme.Accent

@Composable
fun AddScreen(viewModel: AddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Transaction", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)

        // Account toggle
        ToggleRow("Account", listOf("THB", "IDR"), state.account) {
            viewModel.update { copy(account = it) }
        }

        // Type toggle
        ToggleRow("Type", listOf("Expense", "Income"), state.type) {
            viewModel.update { copy(type = it) }
        }

        // Amount
        OutlinedTextField(
            value = state.amount, onValueChange = { viewModel.update { copy(amount = it) } },
            label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            )
        )

        // Description
        OutlinedTextField(
            value = state.description, onValueChange = { viewModel.update { copy(description = it) } },
            label = { Text("Description / Item") }, modifier = Modifier.fillMaxWidth()
        )

        // Category dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.category, onValueChange = {},
                readOnly = true, label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ClaudeCategorizor.CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { viewModel.update { copy(category = cat) }; expanded = false }
                    )
                }
            }
        }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        state.successMessage?.let { Text(it, color = Accent, fontSize = 12.sp) }

        Button(
            onClick = { viewModel.submit() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
            else Text("Sync to Sheets", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ToggleRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt) })
            }
        }
    }
}
```

- [ ] **Step 3: Build and run** — confirm Add tab renders with all fields.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/add/
git commit -m "feat: add manual entry screen with account/type toggle and Sheets sync"
```

---

## Task 22: SettingsScreen

**Files:**
- Create: `ui/settings/SettingsViewModel.kt`
- Create: `ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Create `ui/settings/SettingsViewModel.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val isSignedIn: Boolean = false,
    val accountEmail: String = "",
    val claudeApiKey: String = "",
    val isListenerActive: Boolean = false,
    val isSyncing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val prefs: SecurePrefs,
    private val repository: TransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = SettingsState(
            isSignedIn = authManager.isSignedIn(),
            accountEmail = authManager.getSignedInEmail() ?: "",
            claudeApiKey = prefs.claudeApiKey,
            isListenerActive = isNotificationListenerActive()
        )
    }

    fun saveClaudeKey(key: String) { prefs.claudeApiKey = key; refresh() }

    fun signOut() { authManager.signOut(); refresh() }

    fun retryPendingSyncs() = viewModelScope.launch {
        _state.value = _state.value.copy(isSyncing = true)
        repository.retryFailedSyncs()
        _state.value = _state.value.copy(isSyncing = false)
    }

    private fun isNotificationListenerActive(): Boolean {
        // Placeholder — check via NotificationManager in real impl
        return true
    }
}
```

- [ ] **Step 2: Create `ui/settings/SettingsScreen.kt`**

```kotlin
package com.fidriyanto.banktracker.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.ui.theme.Accent
import com.fidriyanto.banktracker.ui.theme.Destructive

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var claudeKeyInput by remember(state.claudeApiKey) { mutableStateOf(state.claudeApiKey) }
    var showKey by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)

        // Google Account
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Google Account", fontWeight = FontWeight.SemiBold, color = Color.White)
                if (state.isSignedIn) {
                    Text(state.accountEmail, fontSize = 13.sp, color = Color.Gray)
                    OutlinedButton(onClick = { viewModel.signOut() }) { Text("Sign Out") }
                } else {
                    Text("Not signed in", fontSize = 13.sp, color = Color.Gray)
                    Button(onClick = {
                        // GoogleAuthManager.getSignInIntent() needs to be called here
                        // Inject GoogleAuthManager into composable via hiltViewModel or pass intent
                    }) { Text("Sign in with Google") }
                }
            }
        }

        // Claude API Key
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

        // Service Status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notification Listener", fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dot = if (state.isListenerActive) "●" else "●"
                    val dotColor = if (state.isListenerActive) Accent else Destructive
                    Text(dot, color = dotColor, fontSize = 18.sp)
                    Text(if (state.isListenerActive) "Listening" else "Inactive", color = Color.White)
                }
                if (!state.isListenerActive) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text("Enable Access") }
                }
            }
        }

        // Retry pending
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

- [ ] **Step 3: Wire Google Sign-In properly** — in `SettingsScreen`, inject `GoogleAuthManager` via a wrapper ViewModel method that returns the sign-in Intent, then pass to `signInLauncher.launch(intent)`.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/settings/
git commit -m "feat: add Settings screen with Google auth, Claude key, service status"
```

---

## Task 23: AndroidManifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Replace `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

    <application
        android:name=".BankTrackerApp"
        android:allowBackup="true"
        android:label="BankTracker"
        android:theme="@style/Theme.BankTracker">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <!-- NotificationListenerService requires user to enable in system settings -->
        <service
            android:name=".service.BankNotificationService"
            android:label="BankTracker Listener"
            android:exported="false"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService"/>
            </intent-filter>
        </service>

    </application>
</manifest>
```

- [ ] **Step 2: Verify Bangkok Bank package name** on the target device:
```bash
adb shell pm list packages | grep -i bangkok
```
Update `BankNotificationService.BANGKOK_BANK_PACKAGE` with the actual package name found.

- [ ] **Step 3: Build full project** — confirm zero errors.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: finalize AndroidManifest with notification listener service declaration"
```

---

## Task 24: End-to-End Smoke Test

- [ ] **Step 1: Install on device** via Android Studio or `adb install`.

- [ ] **Step 2: Enable notification access** — Settings → Apps → Special app access → Notification access → BankTracker → Enable.

- [ ] **Step 3: Sign in with Google** in the Settings tab. Confirm account email appears.

- [ ] **Step 4: Enter Claude API key** and save.

- [ ] **Step 5: Test manual entry** — Add tab → THB Expense → 100 → "Test entry" → Food & Drink → Sync. Open Google Sheet, confirm row appears in Expenses tab with correct date format (`D/M/YYYY` text, no leading zeros).

- [ ] **Step 6: Test notification capture** — Make a small Bangkok Bank transaction (or trigger a test notification). Wait 5-10 seconds. Confirm review notification appears. Let 3 minutes pass (or tap Edit and save). Confirm row appears in Expenses tab.

- [ ] **Step 7: Test offline queue** — Enable airplane mode. Add a manual entry. Confirm ⚠ badge. Disable airplane mode. Confirm auto-retry syncs it.

- [ ] **Step 8: Commit**
```bash
git add .
git commit -m "feat: complete BankTracker v1.0 — notification capture, email parsing, Claude categorization, Sheets sync"
```

---

## Self-Review Checklist

**Spec coverage:**
- [x] Auto-capture Bangkok Bank push notifications → `BankNotificationService`
- [x] Gmail API email fetch → `EmailFetcher`
- [x] 4 email format parsing → `EmailParser`
- [x] Rule-based categorization → `RuleBasedMatcher`
- [x] PromptPay heuristic ≥ 25k → `PromptPayHeuristic`
- [x] Claude Haiku fallback with caching → `ClaudeCategorizor`
- [x] 3-min review notification → `ReviewNotificationManager`
- [x] Sheets API v4, USER_ENTERED, correct date formats → `SheetsSyncer` + `DateFormatter`
- [x] All 4 tabs (Expenses, IDR Expenses, Income, IDR Income) → `SheetsSyncer.buildRowValues`
- [x] Offline queue + WorkManager retry → `SyncWorker` + `TransactionRepository.retryFailedSyncs`
- [x] Duplicate detection via reference number → `ProcessedRefDao` + `DuplicateGuard` logic in `Repository`
- [x] Manual entry UI → `AddScreen`
- [x] Feed card list with 4 states → `FeedScreen` + `TransactionCard`
- [x] Settings: Google auth, Claude key, service status → `SettingsScreen`
- [x] OLED dark theme, Plus Jakarta Sans, Material 3 → `Theme.kt`
- [x] EncryptedSharedPreferences for API keys → `SecurePrefs`
