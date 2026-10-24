package com.divine.gita.domain

import com.divine.gita.domain.usecase.DailySlokaCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySlokaCalculatorTest {

    @Test fun `first of January is the first sloka`() =
        assertEquals(0, DailySlokaCalculator.indexFor(dayOfYear = 1, totalSlokas = 701))

    @Test fun `each day advances by one`() {
        (1..365).forEach { day -> assertEquals(day - 1, DailySlokaCalculator.indexFor(day, 701)) }
    }

    @Test fun `wraps around when there are fewer slokas than days`() {
        assertEquals(0, DailySlokaCalculator.indexFor(101, 100))
        assertEquals(65, DailySlokaCalculator.indexFor(366, 100))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects empty library`() { DailySlokaCalculator.indexFor(10, 0) }
}
