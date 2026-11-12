package com.upisoundbox.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class DayAggregate(
    val dateString: String,
    val totalPaise: Long,
    val count: Int
)

data class AppBreakdown(
    val appPackage: String,
    val totalPaise: Long,
    val count: Int
)

data class HourlyAggregate(
    val hourOfDay: Int,
    val totalPaise: Long,
    val count: Int
)

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Update
    suspend fun update(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE upiRef = :upiRef LIMIT 1")
    suspend fun getByUpiRef(upiRef: String): PaymentEntity?

    @Query("SELECT SUM(amountPaise) FROM payments WHERE receivedAt >= :startOfDayTimestamp")
    fun getTodayTotal(startOfDayTimestamp: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM payments WHERE receivedAt >= :startOfDayTimestamp")
    fun getTodayCount(startOfDayTimestamp: Long): Flow<Int>

    @Query("SELECT SUM(amountPaise) FROM payments WHERE receivedAt >= :startOfMonthTimestamp")
    fun getMonthlyTotal(startOfMonthTimestamp: Long): Flow<Long?>

    @Query("SELECT SUM(amountPaise) FROM payments WHERE receivedAt >= :startOfYesterday AND receivedAt < :startOfToday")
    fun getYesterdayTotal(startOfYesterday: Long, startOfToday: Long): Flow<Long?>

    @Query("SELECT * FROM payments ORDER BY receivedAt DESC LIMIT 1")
    fun getLastPayment(): Flow<PaymentEntity?>

    @Query("SELECT * FROM payments ORDER BY receivedAt DESC LIMIT :limit")
    fun getRecentPayments(limit: Int = 50): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY receivedAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("""
        SELECT * FROM payments 
        WHERE (:query IS NULL OR :query = '' OR payer LIKE '%' || :query || '%' OR upiRef LIKE '%' || :query || '%')
          AND (:dateFrom IS NULL OR receivedAt >= :dateFrom)
          AND (:dateTo IS NULL OR receivedAt <= :dateTo)
          AND (:minPaise IS NULL OR amountPaise >= :minPaise)
          AND (:maxPaise IS NULL OR amountPaise <= :maxPaise)
        ORDER BY receivedAt DESC
    """)
    fun getFilteredPayments(
        query: String?,
        dateFrom: Long?,
        dateTo: Long?,
        minPaise: Long?,
        maxPaise: Long?
    ): Flow<List<PaymentEntity>>

    @Query("""
        SELECT strftime('%Y-%m-%d', receivedAt / 1000, 'unixepoch', 'localtime') AS dateString,
               SUM(amountPaise) AS totalPaise,
               COUNT(*) AS count
        FROM payments
        WHERE receivedAt >= :sinceTimestamp
        GROUP BY dateString
        ORDER BY dateString ASC
    """)
    suspend fun getDailyAggregates(sinceTimestamp: Long): List<DayAggregate>

    @Query("""
        SELECT appPackage,
               SUM(amountPaise) AS totalPaise,
               COUNT(*) AS count
        FROM payments
        WHERE receivedAt >= :sinceTimestamp
        GROUP BY appPackage
        ORDER BY totalPaise DESC
    """)
    suspend fun getAppBreakdown(sinceTimestamp: Long): List<AppBreakdown>

    @Query("""
        SELECT CAST(strftime('%H', receivedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hourOfDay,
               SUM(amountPaise) AS totalPaise,
               COUNT(*) AS count
        FROM payments
        WHERE receivedAt >= :sinceTimestamp
        GROUP BY hourOfDay
        ORDER BY hourOfDay ASC
    """)
    suspend fun getHourlyBreakdown(sinceTimestamp: Long): List<HourlyAggregate>

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}

