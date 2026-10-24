package com.divine.gita.domain.usecase

import com.divine.gita.domain.model.DailySloka
import com.divine.gita.domain.repository.GitaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.Calendar

/** Abstraction over "now" so the rotation can be unit-tested. */
fun interface CalendarProvider {
    fun now(): Calendar
}

class GetDailySlokaUseCase(
    private val repository: GitaRepository,
    private val calendarProvider: CalendarProvider,
) {
    /** Day-of-year for "today"; the ViewModel re-invokes the use case when this changes. */
    fun currentDayOfYear(): Int = calendarProvider.now().get(Calendar.DAY_OF_YEAR)

    /**
     * Emits today's sloka and keeps emitting when it changes in Room (e.g. favourite toggled).
     */
    suspend operator fun invoke(): Flow<DailySloka> {
        val calendar = calendarProvider.now()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val total = repository.slokaCount()
        if (total == 0) return emptyFlow()
        val index = DailySlokaCalculator.indexFor(dayOfYear, total)
        return repository.observeSlokaAtIndex(index)
            .filterNotNull()
            .map { DailySloka(sloka = it, dayOfYear = dayOfYear, dateMillis = calendar.timeInMillis) }
    }
}
