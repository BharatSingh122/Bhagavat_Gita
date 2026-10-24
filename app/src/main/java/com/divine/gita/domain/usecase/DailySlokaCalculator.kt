package com.divine.gita.domain.usecase

/**
 * Deterministic daily rotation, no server and no hard-coded dates.
 *
 *     index = (DAY_OF_YEAR - 1) % totalSlokas
 *
 * DAY_OF_YEAR is 1-based (1..365/366), so 1 January maps to index 0 (the first sloka),
 * and every calendar day moves to the next sloka in canonical chapter/verse order.
 */
object DailySlokaCalculator {
    fun indexFor(dayOfYear: Int, totalSlokas: Int): Int {
        require(totalSlokas > 0) { "totalSlokas must be > 0" }
        require(dayOfYear in 1..366) { "dayOfYear must be in 1..366, was $dayOfYear" }
        return (dayOfYear - 1) % totalSlokas
    }
}
