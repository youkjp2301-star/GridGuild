package com.gridguild.core

/** Presentation of an already persisted award. Rendering this never changes the ledger. */
data class XpVisualEvent(val id: String,val personalGain: Int,val cityGain: Int,val cityBefore: Int,val cityTotal: Int) {
    companion object {
        fun between(previous: GameState,next: GameState): XpVisualEvent? {
            if(previous.districtId!=next.districtId || previous.mode!=next.mode)return null
            val known=previous.history.map{it.id}.toSet()
            val added=next.history.filter{it.id !in known && it.xp>0}
            val personal=added.filter{it.personalCredit}.sumOf{it.xp}
            val city=added.filter{it.cityCredit && it.districtId==next.districtId}.sumOf{it.xp}
            if(personal==0 && city==0)return null
            val total=next.cityXp(next.districtId)
            return XpVisualEvent(added.joinToString("|"){it.id},personal,city,total-city,total)
        }
    }
}
