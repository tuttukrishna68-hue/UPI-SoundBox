package com.upisoundbox.core.data.repository

import com.upisoundbox.core.data.db.AppBreakdown
import com.upisoundbox.core.data.db.DayAggregate
import com.upisoundbox.core.data.db.HourlyAggregate
import com.upisoundbox.core.data.db.PaymentDao
import com.upisoundbox.core.data.db.PaymentEntity
import com.upisoundbox.core.parser.PaymentEvent
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

interface PaymentRepository {
    fun getTodayTotal(): Flow<Long?>
    fun getTodayCount(): Flow<Int>
    fun getMonthlyTotal(): Flow<Long?>
    fun getYesterdayTotal(): Flow<Long?>
    fun getLastPayment(): Flow<PaymentEntity?>
    fun getRecentPayments(limit: Int = 50): Flow<List<PaymentEntity>>
    fun getAllPayments(): Flow<List<PaymentEntity>>
    suspend fun getPaymentById(id: String): PaymentEntity?
    suspend fun getPaymentByUpiRef(upiRef: String): PaymentEntity?
    fun getFilteredPayments(
        query: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        minPaise: Long? = null,
        maxPaise: Long? = null
    ): Flow<List<PaymentEntity>>
    suspend fun recordPayment(event: PaymentEvent, announced: Boolean = true, rawSnippet: String? = null): Long
    suspend fun getDailyAggregates(sinceTimestamp: Long): List<DayAggregate>
    suspend fun getAppBreakdown(sinceTimestamp: Long): List<AppBreakdown>
    suspend fun getHourlyBreakdown(sinceTimestamp: Long): List<HourlyAggregate>
}

class PaymentRepositoryImpl(
    private val paymentDao: PaymentDao
) : PaymentRepository {

    private fun getStartOfDayTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getStartOfMonthTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getStartOfYesterdayTimestamp(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    override fun getTodayTotal(): Flow<Long?> = paymentDao.getTodayTotal(getStartOfDayTimestamp())

    override fun getTodayCount(): Flow<Int> = paymentDao.getTodayCount(getStartOfDayTimestamp())

    override fun getMonthlyTotal(): Flow<Long?> = paymentDao.getMonthlyTotal(getStartOfMonthTimestamp())

    override fun getYesterdayTotal(): Flow<Long?> =
        paymentDao.getYesterdayTotal(getStartOfYesterdayTimestamp(), getStartOfDayTimestamp())

    override fun getLastPayment(): Flow<PaymentEntity?> = paymentDao.getLastPayment()

    override fun getRecentPayments(limit: Int): Flow<List<PaymentEntity>> = paymentDao.getRecentPayments(limit)

    override fun getAllPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    override suspend fun getPaymentById(id: String): PaymentEntity? = paymentDao.getById(id)

    override suspend fun getPaymentByUpiRef(upiRef: String): PaymentEntity? = paymentDao.getByUpiRef(upiRef)

    override fun getFilteredPayments(
        query: String?,
        dateFrom: Long?,
        dateTo: Long?,
        minPaise: Long?,
        maxPaise: Long?
    ): Flow<List<PaymentEntity>> = paymentDao.getFilteredPayments(query, dateFrom, dateTo, minPaise, maxPaise)

    override suspend fun recordPayment(event: PaymentEvent, announced: Boolean, rawSnippet: String?): Long {
        val entity = PaymentEntity.fromEvent(event, announced = announced, rawSnippet = rawSnippet)
        return paymentDao.insert(entity)
    }

    override suspend fun getDailyAggregates(sinceTimestamp: Long): List<DayAggregate> =
        paymentDao.getDailyAggregates(sinceTimestamp)

    override suspend fun getAppBreakdown(sinceTimestamp: Long): List<AppBreakdown> =
        paymentDao.getAppBreakdown(sinceTimestamp)

    override suspend fun getHourlyBreakdown(sinceTimestamp: Long): List<HourlyAggregate> =
        paymentDao.getHourlyBreakdown(sinceTimestamp)
}

