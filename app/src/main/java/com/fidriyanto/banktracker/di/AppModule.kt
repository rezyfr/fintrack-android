package com.fidriyanto.banktracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.fidriyanto.banktracker.BuildConfig
import com.fidriyanto.banktracker.data.datasource.remote.AiModelDownloadService
import com.fidriyanto.banktracker.data.datasource.remote.AppSettingsApiService
import com.fidriyanto.banktracker.data.datasource.remote.RuleApiService
import com.fidriyanto.banktracker.data.datasource.remote.WalletBalanceApiService
import com.fidriyanto.banktracker.data.datasource.remote.BudgetLineApiService
import com.fidriyanto.banktracker.data.datasource.remote.CardBillingApiService
import com.fidriyanto.banktracker.data.datasource.remote.InstallmentApiService
import com.fidriyanto.banktracker.data.datasource.remote.TransactionApiService
import com.fidriyanto.banktracker.data.datasource.remote.AccountApiService
import com.fidriyanto.banktracker.data.datasource.remote.CategoryApiService
import com.fidriyanto.banktracker.data.datasource.remote.ApiServerInterceptor
import com.fidriyanto.banktracker.data.datasource.remote.ServerProbeApiService
import com.fidriyanto.banktracker.data.model.BuiltInServer
import com.fidriyanto.banktracker.data.db.AppDatabase
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "fintrack_prefs")

// The API services are built with this address. ApiServerInterceptor sends each call to the server the
// user chose, so the address and the token can change at run time.
private const val API_PLACEHOLDER_URL = "http://fintrack.invalid/"

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "banktracker.db")
            .addMigrations(
                AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16, AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19
            )
            .build()

    @Provides fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
    @Provides fun provideProcessedRefDao(db: AppDatabase) = db.processedRefDao()
    @Provides fun provideAccountDao(db: AppDatabase) = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase) = db.categoryDao()
    @Provides fun providePendingInstallmentStepDao(db: AppDatabase) = db.pendingInstallmentStepDao()
    @Provides fun provideAutoAddedDao(db: AppDatabase) = db.autoAddedDao()
    @Provides fun provideLedgerDao(db: AppDatabase) = db.ledgerDao()

    // The server address and token from local.properties. An empty address means the user picks the
    // storage in setup.
    @Provides @Singleton
    fun provideBuiltInServer(): BuiltInServer = BuiltInServer(BuildConfig.API_BASE_URL, BuildConfig.API_TOKEN)

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton @Named("api")
    fun provideApiOkHttpClient(base: OkHttpClient, server: ApiServerInterceptor): OkHttpClient = base.newBuilder()
        .addInterceptor(server)
        .build()

    // ac: setup-storage-choice - tests a server the user typed. It uses the plain client, so the call
    // goes to the typed address and not to the stored one.
    @Provides @Singleton
    fun provideServerProbeService(base: OkHttpClient): ServerProbeApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(base)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServerProbeApiService::class.java)

    @Provides @Singleton
    fun provideTransactionService(@Named("api") client: OkHttpClient): TransactionApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TransactionApiService::class.java)

    @Provides @Singleton
    fun provideInstallmentService(@Named("api") client: OkHttpClient): InstallmentApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InstallmentApiService::class.java)

    @Provides @Singleton
    fun provideCardBillingService(@Named("api") client: OkHttpClient): CardBillingApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CardBillingApiService::class.java)

    @Provides @Singleton
    fun provideBudgetLineService(@Named("api") client: OkHttpClient): BudgetLineApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BudgetLineApiService::class.java)

    @Provides @Singleton
    fun provideAccountService(@Named("api") client: OkHttpClient): AccountApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AccountApiService::class.java)

    @Provides @Singleton
    fun provideWalletBalanceService(@Named("api") client: OkHttpClient): WalletBalanceApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WalletBalanceApiService::class.java)

    @Provides @Singleton
    fun provideAppSettingsService(@Named("api") client: OkHttpClient): AppSettingsApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AppSettingsApiService::class.java)

    @Provides @Singleton
    fun provideRuleService(@Named("api") client: OkHttpClient): RuleApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RuleApiService::class.java)

    @Provides @Singleton
    fun provideCategoryService(@Named("api") client: OkHttpClient): CategoryApiService =
        Retrofit.Builder()
            .baseUrl(API_PLACEHOLDER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CategoryApiService::class.java)

    // The model file is large, so reads wait up to two minutes for the next bytes.
    @Provides @Singleton
    fun provideAiModelDownloadService(base: OkHttpClient): AiModelDownloadService =
        Retrofit.Builder()
            .baseUrl("https://huggingface.co/")
            .client(base.newBuilder().readTimeout(2, TimeUnit.MINUTES).build())
            .build()
            .create(AiModelDownloadService::class.java)

    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore
}
