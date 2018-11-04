package com.lgsdiamond.bigtree.amway

import com.lgsdiamond.bigtree.R
import com.lgsdiamond.bigtree.tree.AmNode
import com.lgsdiamond.bigtree.tree.LayoutItemType
import com.lgsdiamond.bigtree.tree.TreeNode
import kotlin.math.roundToLong


// extensions
fun Float.to1000Won(): String {
    return (((this * 10.0f).roundToLong().toFloat()) / 10.0f).toString()
}

enum class AmMemberClass { ABO, ON_MEMBER, OFF_MEMBER }

abstract class AmMember(var name: String) : LayoutItemType {
    abstract val memberClass: AmMemberClass
    abstract val pv: AmPV
    lateinit var node: AmNode
    abstract fun toTitle(): CharSequence
    abstract fun toDesc(): CharSequence

    private var idStamp: Long = contStampID++
    fun getIDStamp() = idStamp
    fun setIDStamp(stamp: Long) {
        idStamp = stamp
    }

    val sponsor: ABO?
        get() = (if (node.parent == null) null else node.parent!!.content as ABO)
    val isTopSponsor: Boolean
        get() = (sponsor == null)

    open fun resetBase() {
        pv.resetBase()
    }

    fun findSponsorByID(id: Long): AmMember? {
        if (idStamp == id) return this
        return sponsor?.findSponsorByID(id)
    }

    companion object {
        private var counterABO = 0
        private var counterOnMember = 0
        private var counterOffMember = 0

        private var contStampID: Long = System.currentTimeMillis()

        const val DEFAULT_PV: Float = 20.0f

        val newABO: ABO
            get() = ABO("abo${++counterABO}", DEFAULT_PV)
        val newOnMember: OnMember
            get() = OnMember("onMember${++counterOnMember}", DEFAULT_PV)
        val newOffMember: OffMember
            get() = OffMember("offMember${++counterOffMember}", DEFAULT_PV)
    }
}

class OffMember(name: String, ownPV: Float = DEFAULT_PV) : AmMember(name) {
    override val memberClass = AmMemberClass.OFF_MEMBER

    override val pv: AmPV = AmPV(this@OffMember, ownPV)
    override val layoutId: Int = R.layout.item_offmember

    override fun toString(): String = "OffMember[$name]"
    override fun toTitle(): CharSequence = "[$name]"
    override fun toDesc(): CharSequence = "[$name], PV=${pv.personal}"
}

// OnMember has an unique online id
open class OnMember(name: String, ownPV: Float = DEFAULT_PV) : AmMember(name) {
    override val memberClass = AmMemberClass.ON_MEMBER

    override val pv: AmPV = AmPV(this@OnMember, ownPV)
    override val layoutId: Int = R.layout.item_onmember

    override fun toString(): String = "OnMember[$name]"
    override fun toTitle(): CharSequence = "[$name]"
    override fun toDesc(): CharSequence = "[$name], PV=${pv.personal}"
}

// ABO is an OnMember who can have partner(ABO), OnMembers, OffMembers
class ABO(name: String, ownPV: Float = DEFAULT_PV) : OnMember(name, ownPV) {
    override val memberClass = AmMemberClass.ABO

    override val pv: ABOPV = ABOPV(this@ABO, ownPV)
    var pin: MemberPin = MemberPin(this@ABO)
    val pinTitle: String
        get() = if (pin.current == PinTitle.NONE) "${(firstBonusRate * 100.0f).toInt()}%" else pin.current.toString()

    val monthlyRecords = ArrayList<MonthlyRecord>()

    val bonus = AmBonus(this@ABO)

    override val layoutId: Int = R.layout.item_abo

    override fun toString(): String = "[$name]"

    override fun toTitle(): CharSequence {
        val firstBonus = bonus.first.amount
        val totalBonus = bonus.total

        var title = "[$name], B=${firstBonus.to1000Won()}"
        if (totalBonus != firstBonus) title += "/${totalBonus.to1000Won()}"

        return title
    }

    override fun toDesc(): CharSequence {
        val count = countIndependent
        return if (count > 0) {
            "PV=${pv.personalGroup.to1000Won()}/${pv.personal.to1000Won()}, 독립$count(${pv.independent.to1000Won()})"
        } else {
            "PV=${pv.personalGroup.to1000Won()}/${pv.personal.to1000Won()}"
        }
    }

    val firstBonusRate: Float
        get() = AmBonus.getFirstBonusRate(pv.group)

    override fun resetBase() {
        super.resetBase()
        bonus.resetBase()
    }

    fun finalizeMonth(month: AmMonth?) {

        if (month == null) return

        // remove all future record
        val prevRecord = monthlyRecords.firstOrNull {
            (it.month == month)
        }

        if (prevRecord != null) {
            val index = monthlyRecords.indexOf(prevRecord)
            while (monthlyRecords.size > index)
                monthlyRecords.removeAt(monthlyRecords.size - 1)
        }

        // make and add new record at last
        val record = MonthlyRecord(month)
        record.groupPV = pv.group
        record.personalPV = pv.personal
        record.personalGroupPV = pv.personalGroup
        pin.evaluate(month)

        bonus.recordAmount(record)

        monthlyRecords.add(record)
    }

    val supportMembers: ArrayList<AmMember>
        get() {
            val members = ArrayList<AmMember>()
            for (child in node.children) {
                members.add((child.content as AmMember))
            }
            return members
        }

    val isIndependent: Boolean
        get() = (firstBonusRate >= AmSystem.FIRST_BONUS_MAX_RATE)
    private val independentList: List<TreeNode<*>>
        get() = node.children.filter { (it as AmNode).isIndependent }
    private val countIndependent: Int
        get() = independentList.size

    val aboPartners: List<ABO>
        get() {
            val aboList = mutableListOf<ABO>()
            node.children.forEach {
                if (it.content is ABO) aboList.add(it.content as ABO)
            }
            return aboList
        }

    val independentPartners: List<ABO>
        get() {
            val aboList = mutableListOf<ABO>()
            aboPartners.forEach {
                if (it.isIndependent) aboList.add(it)
            }
            return aboList
        }

    val isPureIndependent: Boolean
        get() = isIndependent && independentPartners.isEmpty()

    val pureIndependentDownline: ArrayList<ABO>
        get() {
            val aboList = ArrayList<ABO>()
            for (sub in independentPartners) {
                if (sub.independentPartners.isEmpty()) {
                    aboList.add(sub)
                } else {
                    aboList.addAll(sub.pureIndependentDownline)
                }
            }
            return aboList
        }

    fun isQualifiedPIN(pin: PinTitle): Boolean {
        return false        // TODO: needs more work
    }

    fun isUplineSponsor(leader: ABO): Boolean {
        if (sponsor == null) return false
        return if (sponsor == leader) true else sponsor!!.isUplineSponsor(leader)
    }

    fun buildLOS(topLeader: ABO): ArrayList<ABO>? {
        val los = ArrayList<ABO>()
        los.add(this)

        var mySponsor: ABO? = sponsor
        while (mySponsor != null) {
            los.add(mySponsor)
            if (mySponsor == topLeader) return los
            mySponsor = mySponsor.sponsor
        }
        return null
    }
}