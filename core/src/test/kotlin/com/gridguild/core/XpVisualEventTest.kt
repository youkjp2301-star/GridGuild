package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class XpVisualEventTest {
    private val initial=GameState(districtId="sasang")
    private val award=PointEvent("a","a","sasang",120,1L)
    @Test fun persistedAwardHasAccurateIndependentCredits() {
        val old=initial.copy(history=listOf(award.copy(id="old",personalCredit=false)))
        val next=old.copy(history=old.history+award)
        assertEquals(XpVisualEvent("a",120,120,120,240),XpVisualEvent.between(old,next))
        assertNull(XpVisualEvent.between(next,next))
    }
    @Test fun resetsAndCreditFlagChangesNeverReplayRewards() {
        val old=initial.copy(history=listOf(award))
        assertNull(XpVisualEvent.between(old,initial))
        assertNull(XpVisualEvent.between(old,old.copy(history=listOf(award.copy(cityCredit=false)))))
        assertNull(XpVisualEvent.between(old,old.copy(districtId="dongnae")))
    }
    @Test fun consecutiveEqualAwardsRemainDistinct() {
        val one=initial.copy(history=listOf(award))
        val two=one.copy(history=one.history+award.copy(id="b"))
        assertNotEquals(XpVisualEvent.between(initial,one)?.id,XpVisualEvent.between(one,two)?.id)
        assertEquals(120,XpVisualEvent.between(one,two)?.cityBefore)
    }
}
