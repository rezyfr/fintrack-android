package com.fidriyanto.banktracker.data.db

import androidx.room.*
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.MerchantTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE (:monthPrefix IS NULL OR dateIso LIKE :monthPrefix || '%')
        AND   (:wallet IS NULL OR wallet = :wallet)
        AND   (:txType IS NULL OR txType = :txType)
        ORDER BY dateIso DESC, createdAt DESC
    """)
    fun observeFiltered(monthPrefix: String?, wallet: String?, txType: String?): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = :status")
    suspend fun getByStatus(status: TransactionStatus): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: TransactionEntity): Long

    @Update
    suspend fun update(t: TransactionEntity)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TransactionStatus)

    // ac: delete-transaction-from-feed — hard delete; row is physically removed
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET status = 'SYNCED' WHERE status IN ('SYNC_FAILED', 'PENDING_SYNC')")
    suspend fun markAllSynced()

    @Query("""
        SELECT item AS merchant, SUM(amount) as amount FROM transactions
        WHERE category = :category AND txType = 'expense'
        AND wallet = 'BBL'
        AND dateIso >= :fromDate AND dateIso <= :toDate
        GROUP BY item ORDER BY amount DESC
    """)
    fun observeTransportTotalsTHB(category: String, fromDate: String, toDate: String): Flow<List<MerchantTotal>>

    @Query("""
        SELECT item AS merchant, SUM(amount) as amount FROM transactions
        WHERE category = :category AND txType = 'expense'
        AND wallet IS NOT NULL AND wallet != 'BBL'
        AND dateIso >= :fromDate AND dateIso <= :toDate
        GROUP BY item ORDER BY amount DESC
    """)
    fun observeTransportTotalsIDR(category: String, fromDate: String, toDate: String): Flow<List<MerchantTotal>>
}
