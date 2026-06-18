package com.fidriyanto.banktracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.fidriyanto.banktracker.BuildConfig
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseBudgetService
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseInstallmentService
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseOverviewService
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseTransactionService
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

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "banktracker.db")
            .addMigrations(
                AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .build()

    @Provides fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
    @Provides fun provideProcessedRefDao(db: AppDatabase) = db.processedRefDao()
    @Provides fun provideMonthlyOverviewDao(db: AppDatabase) = db.monthlyOverviewDao()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton @Named("supabase")
    fun provideSupabaseOkHttpClient(base: OkHttpClient): OkHttpClient = base.newBuilder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .build()
            )
        }
        .build()

    @Provides @Singleton
    fun provideTransactionService(@Named("supabase") client: OkHttpClient): SupabaseTransactionService =
        Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseTransactionService::class.java)

    @Provides @Singleton
    fun provideOverviewService(@Named("supabase") client: OkHttpClient): SupabaseOverviewService =
        Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseOverviewService::class.java)

    @Provides @Singleton
    fun provideBudgetService(@Named("supabase") client: OkHttpClient): SupabaseBudgetService =
        Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseBudgetService::class.java)

    @Provides @Singleton
    fun provideInstallmentService(@Named("supabase") client: OkHttpClient): SupabaseInstallmentService =
        Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseInstallmentService::class.java)

    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore
}
