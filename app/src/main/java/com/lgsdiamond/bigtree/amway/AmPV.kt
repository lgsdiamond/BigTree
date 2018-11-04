package com.lgsdiamond.bigtree.amway

open class AmPV(private val member: AmMember, var personal: Float) {
    private val sponsor: ABO?
        get() = member.sponsor

    var group: Float = personal         // real-time update
    var baseGroup: Float = 0f           // for tracking change
    val groupChange: Float
        get() = group - baseGroup

    val personalBV: Float
        get() = pv2bv(personal)
    val groupBV: Float
        get() = pv2bv(group)

    fun resetPersonal(pv: Float = 0f) {
        require(pv >= 0f)
        val change = pv - personal
        if (change >= 0) {
            addPersonal(change)
        } else {
            subtractPersonal(-change)
        }
    }

    fun addPersonal(pv: Float) {
        require(pv >= 0f)
        personal += pv
        addGroup(pv)
    }

    fun subtractPersonal(pv: Float): Float {       // return the amount effective
        require(pv >= 0f)
        val newPV = if (pv >= personal) personal else pv
        personal -= newPV
        addGroup(-newPV)

        return newPV
    }

    fun addGroup(pv: Float) {
        group += pv
        sponsor?.pv?.addGroup(pv)
    }

    fun addGroupToSponsor() {
        sponsor?.pv?.addGroup(group)
    }

    fun subtractGroupFromSponsor() {
        sponsor?.pv?.addGroup(-group)
    }

    fun resetBase() {
        baseGroup = group
    }

    override fun toString(): String {
        return "[개인]$personal"
    }

    companion object {
        fun pv2bv(pv: Float) = pv * AmSystem.PV_BV_RATIO
    }
}

class ABOPV(private val abo: ABO, personalPV: Float) : AmPV(abo, personalPV) {

    val independent: Float
        get() = abo.independentPartners.sumByDouble { it.pv.group.toDouble() }.toFloat()
    val personalGroup: Float
        get() = group - independent

    val independentBV: Float
        get() = pv2bv(independent)
    val personalGroupBV: Float
        get() = pv2bv(personalGroup)

    override fun toString(): String {
        return "[개인]${personal.to1000Won()}, [개인그룹]${personalGroup.to1000Won()}, " +
                "[그룹]${group.to1000Won()}"
    }
}