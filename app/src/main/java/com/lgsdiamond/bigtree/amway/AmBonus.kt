package com.lgsdiamond.bigtree.amway

class AmBonus(private val abo: ABO) {

    val first = FirstBonus(abo)
    val leadership = LeadershipBonus(abo)
    val ruby = RubyBonus(abo)
    val monthlyDepth = MonthlyDepthBonus(abo)
    val emerald = EmeraldBonus(abo)
    val diamond = DiamondBonus(abo)
    val diamondPlus = DiamondPlusBonus(abo)
    val oneTime = OneTimeBonus(abo)
    val faa = FAABonus(abo)

    private val bonuses: List<Bonus> = listOf(first, leadership, ruby, monthlyDepth, emerald, diamond, diamondPlus, oneTime, faa)
    val total: Float
        get() = bonuses.sumByDouble { it.amount.toDouble() }.toFloat()
    var baseTotal: Float = 0f        // for tracking change
    val totalChange: Float
        get() = total - baseTotal

    fun recordAmount(record: MonthlyRecord) {
        record.firstBonus = first.amount
        record.leadershipBonus = leadership.amount
        record.rubyBonus = ruby.amount
        record.monthlyDepthBonus = monthlyDepth.amount
        record.emeraldBonus = emerald.amount
        record.diamondBonus = diamond.amount
        record.diamondPlusBonus = diamondPlus.amount
        record.oneTimeBonus = oneTime.amount
        record.faaBonus = faa.amount
    }

    override fun toString(): String {
        var bonusString = ""
        bonuses.forEach { bonusString = it.addBonusText(bonusString) }
        return bonusString
    }

    fun resetBase() {
        baseTotal = total
    }

    companion object {

        fun getFirstBonusRate(pv: Float): Float = AmSystem.firstBonusRates.first { (pv >= it.pv) }.rate
    }

    abstract class Bonus(val abo: ABO) {
        abstract val title: String
        abstract val isQualified: Boolean
        abstract val amount: Float

        fun addBonusText(bonusString: String): String {
            if (amount == 0f) return bonusString
            return if (bonusString.isEmpty()) "[$title]${amount.to1000Won()}"
            else "$bonusString, [$title]${amount.to1000Won()}"
        }
    }

    class FirstBonus(abo: ABO) : Bonus(abo) {
        override val title = "후원수당"

        // 모든 ABO는 후원수당 수혜 자격이 있음.
        override val isQualified: Boolean
            get() = true

        override val amount: Float
            get() {
                if (!isQualified) return 0f

                var finalBonus = groupBonus
                for (partner in abo.aboPartners) {
                    finalBonus -= partner.bonus.first.groupBonus    // 하위 ABO 그룹 보너스를 먼저 제공
                }
                return finalBonus           // already based on BV(in groupBonus)
            }

        private val groupBonus: Float
            get() = abo.pv.groupBV * abo.firstBonusRate    // based on BV
    }

    class LeadershipBonus(abo: ABO) : Bonus(abo) {
        override val title = "리더십"

        // 1개 독립라인+개인그룹 400PV 이상 또한 2개라인 이상이면 리더십 보너스 자격이 있음
        override val isQualified: Boolean
            get() {
                val indList = abo.independentPartners
                return !(indList.isEmpty() ||
                        ((indList.size == 1) && (abo.pv.personalGroup < AmSystem.LEADERSHIP_BONUS_PARTIAL_PV)))
            }

        // 순수한 개인그룹 BV의6%는 PassUp
        private val purePassUpBV: Float
            get() = abo.pv.personalGroupBV * AmSystem.LEADERSHIP_BONUS_RATE // based on BV

        private val passUpBV: Float
            get() {
                var pass = 0f
                // 1개 독립라인+개인그룹 400PV 이상 또한 2개라인 이상이면 리더십 보너스 자격이 있음
                // 이경우, 최소보장액 60만원, 또는 순수한 개인 PassUp액 중 큰 액수를 PassUp(BV)
                if (isQualified) {
                    pass = Math.max(purePassUpBV, AmSystem.LEADERSHIP_BONUS_MINIMUM_ASSURE)
                } else {
                    // 자격이 없으면 하위 독립라인의 PassUp을 합계하여 상위로 PassUp
                    // 또한 본인의 순수한 PassUp 금액도 역시 상위로 PassUp
                    for (sub in abo.independentPartners) {  // it should be size 1
                        pass += sub.bonus.leadership.passUpBV
                    }
                    pass += purePassUpBV
                }
                return pass
            }

        override val amount: Float
            get() {
                if (!isQualified) return 0f

                // 1개 독립라인+개인그룹 400PV 이상 또한 2개라인 이상이면 리더십 보너스 자격이 있음
                // 각 독립라인의 PassUp(BV)을 합산

                var bonusBV = abo.independentPartners.sumByDouble { it.bonus.leadership.passUpBV.toDouble() }.toFloat()

                // 개인그룹 1_000PV 이상이면, 리더십 보너스 전체 수령
                if (abo.pv.personalGroup >= AmSystem.LEADERSHIP_BONUS_FULL_PV) return bonusBV

                // 개인그룹 1_000PV 미만이면, PassUp 60만원 차액 공제후 수령
                val less = AmSystem.LEADERSHIP_BONUS_MINIMUM_ASSURE - purePassUpBV
                if (less > 0) bonusBV -= less

                return bonusBV        // already based on BV(in purePassUp)
            }
    }

    class RubyBonus(abo: ABO) : Bonus(abo) {
        override val title = "루비"

        // 개인그룹 PV가 2,000만 이상 달성한 경우(PT뿐 아니라, SP, GP도 지급)
        override val isQualified: Boolean
            get() = (abo.pv.personalGroup >= AmSystem.RUBY_BONUS_FULL_PV)

        override val amount: Float
            get() {
                if (!isQualified) return 0f

                var bonusBV = 0f
                for (member in abo.supportMembers) {
                    if ((member !is ABO) || (!member.isIndependent)) {
                        if ((member is ABO) && (member.isQualifiedPIN(PinTitle.PT)))
                            continue    // 21% 미만이더라도 유자격 PT의 bv는 제외
                        bonusBV += member.pv.groupBV
                    }
                }
                bonusBV += abo.pv.personalBV

                return bonusBV * AmSystem.RUBY_BONUS_RATE
            }
    }

    class MonthlyDepthBonus(abo: ABO) : Bonus(abo) {
        override val title = "MD"

        // 21% 보너스 수준을 달성한 그룹을 3개 이상 후원하고 있는 실버프로듀서 이상의 ABO
        override val isQualified: Boolean
            get() = (abo.independentPartners.size >= 3)

        // 두번째 단계에 있는 그룹 이하부터 처음으로 MD 보너스 수혜자 자격을 갖춘 그룹과
        // 그가 직접 또는 대리로 후원하는 21% 수준에 있는 첫번째 그룹까지의 BV를 포함한 총 BV의 1%
        private val passUp: Float
            get() {
                if (!abo.isIndependent) return 0f

                var pass = abo.pv.personalGroupBV * AmSystem.MD_BONUS_RATE

                pass += if (abo.bonus.monthlyDepth.isQualified) {
                    abo.independentPartners.sumByDouble {
                        Math.max(it.pv.personalGroupBV, AmSystem.MD_BONUS_FULL_BV).toDouble() * AmSystem.MD_BONUS_RATE
                    }.toFloat()
                } else {
                    abo.independentPartners.sumByDouble { it.bonus.monthlyDepth.passUp.toDouble() }.toFloat()
                }

                return pass
            }

        override val amount: Float
            get() {
                if (!isQualified) return 0f

                var bonus = 0f
                for (ind in abo.independentPartners) {          // at least 3
                    for (sub in ind.independentPartners) {
                        bonus += sub.bonus.monthlyDepth.passUp
                    }
                }

                // check my first downline's personalGroupBV to assure the Minimum MD passUp
                for (firstDownline in abo.independentPartners) {
                    if (firstDownline.pv.personalGroupBV < AmSystem.MD_BONUS_FULL_BV) {
                        bonus -= (AmSystem.MD_BONUS_FULL_BV - firstDownline.pv.personalGroupBV) *
                                AmSystem.MD_BONUS_RATE
                    }
                }

                return bonus
            }
    }

    class EmeraldBonus(abo: ABO) : Bonus(abo) {
        override val title = "에머랄드"

        override val isQualified: Boolean
            get() = false

        override val amount: Float
            get() = 0f
    }

    class DiamondBonus(abo: ABO) : Bonus(abo) {
        override val title = "다이아몬드"

        override val isQualified: Boolean
            get() = false

        override val amount: Float
            get() = 0f
    }

    class DiamondPlusBonus(abo: ABO) : Bonus(abo) {
        override val title = "다이아몬드+"

        override val isQualified: Boolean
            get() = false

        override val amount: Float
            get() = 0f
    }

    class OneTimeBonus(abo: ABO) : Bonus(abo) {
        override val title = "일시불"

        override val isQualified: Boolean
            get() = false

        override val amount: Float
            get() = 0f
    }

    class FAABonus(abo: ABO) : Bonus(abo) {
        override val title = "FAA"

        override val isQualified: Boolean
            get() = false

        override val amount: Float
            get() = 0f
    }
}