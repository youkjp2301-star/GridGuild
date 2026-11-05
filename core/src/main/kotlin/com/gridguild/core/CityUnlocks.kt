package com.gridguild.core

/** Current and earlier cities may be visited. Locked artwork is never a preview target. */
object CityUnlocks {
    fun canView(level: Int, unlockedThrough: Int) = unlockedThrough in 1..12 && level in 1..unlockedThrough
    fun selection(requested: Int?, cityXp: Int): Int {
        val current=CityLevels.level(cityXp)
        return requested?.takeIf{canView(it,current)} ?: current
    }
    fun remainingXp(cityXp: Int): Int {
        val current=CityLevels.level(cityXp)
        return if(current==12)0 else (CityLevels.thresholds[current]-cityXp).coerceAtLeast(0)
    }
}
