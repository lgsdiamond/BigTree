package com.lgsdiamond.bigtree.amway

import java.util.*

// 회계년도는 9월에 시작
enum class PFMonth(val month: Int) {
    M01(9), M02(10), M03(11), M04(12),
    M05(1), M06(2), M07(3), M08(4),
    M09(5), M10(6), M11(7), M12(8);
}

class AmMonthRange(override val start: AmMonth, override val endInclusive: AmMonth, private val stepMonth: Int = 1)
    : Iterable<AmMonth>, ClosedRange<AmMonth> {
    override fun iterator(): Iterator<AmMonth> {
        return AmMonthRangeIterator(start, endInclusive, stepMonth)
    }
}

class AmMonthRangeIterator(val start: AmMonth,
                           private val endInclusive: AmMonth, private val stepMonth: Int) : Iterator<AmMonth> {
    private var current = start

    override fun next(): AmMonth {
        val next = AmMonth(current.year, current.month)
        val newCurrent = start + stepMonth
        current.year = newCurrent.year
        current.month = newCurrent.month

        return next
    }

    override fun hasNext(): Boolean {
        if (current > endInclusive) return false
        return true
    }
}

// hear year is four digit integer number based on 2000 = PF00, 2001=PF01, etc.
data class AmMonth(var year: Int, var month: Int) : Comparable<AmMonth> {
    private val pfYear: String
        get() = if (month >= 9) "PF${(year - 2000 + 1)}" else "PF${(year - 2000)}"
    private val pfMonth: String
        get() = PFMonth.values()[if (month >= 9) (month - 9) else (month + 3)].toString()

    override fun toString(): String = "${year}년 ${month}월 ($pfYear$pfMonth)"

    val totalMonth: Int
        get() = year * 12 + month

    val startPF: AmMonth
        get() = AmMonth(if (month <= 8) (year - 1) else year, 9)
    val endPF: AmMonth
        get() = AmMonth(if (month >= 9) (year + 1) else year, 8)

    operator fun inc(): AmMonth = this + 1
    operator fun dec(): AmMonth = this - 1
    operator fun plus(amount: Int): AmMonth = AmMonth(this.year, this.month + amount)
    operator fun minus(amount: Int): AmMonth = AmMonth(this.year, this.month - amount)

    override fun equals(other: Any?) = (other is AmMonth) && (year == other.year) && (month == other.month)
    override fun compareTo(other: AmMonth): Int = totalMonth - other.totalMonth
    override fun hashCode() = Objects.hash(year, month)
    operator fun rangeTo(other: AmMonth) = AmMonthRange(this, other)

    init {
        if (month !in 1..12) {
            val total = totalMonth
            year = total / 12
            month = total % 12
            if (month == 0) {
                year--
                month = 12
            }
        }
    }
}

class AmSystem {
    companion object {

        // PV to BV ration
        const val PV_BV_RATIO: Float = 1.0f

        // First Bonus rules
        const val FIRST_BONUS_MAX_RATE = 0.21f

        class PVRate(val pv: Float, val rate: Float)

        val firstBonusRates = arrayOf(
                PVRate(1_000.0f, FIRST_BONUS_MAX_RATE),
                PVRate(680.0f, 0.18f),
                PVRate(400.0f, 0.15f),
                PVRate(240.0f, 0.12f),
                PVRate(120.0f, 0.09f),
                PVRate(60.0f, 0.06f),
                PVRate(20.0f, 0.03f),
                PVRate(-0.1f, 0.00f)
        )

        // Leadership Bonus rules
        const val LEADERSHIP_BONUS_RATE = 0.06f             // bonus rate for full-leg BV
        const val LEADERSHIP_BONUS_MINIMUM_ASSURE = 60.0f   // 최소 60만원 보장
        const val LEADERSHIP_BONUS_FULL_PV = 1_000.0f       // L.Bonus 전체 자격기준(PV)
        const val LEADERSHIP_BONUS_PARTIAL_PV = 400.0f      // L.Bonus 일부 자격기준(PV) with 1 Full leg

        // Ruby Bonus rules
        const val RUBY_BONUS_FULL_PV = 2_000.0f             // 개인그룹 PV가 2천만 이상
        const val RUBY_BONUS_RATE = 0.02f                   // 개인그룹 BV의 2%

        // Monthly-Depth(MD) Bonus rules
        const val MD_BONUS_FULL_BV = 1_000.0f               // 개인그룹 BV가 PV가 1천만 이상
        const val MD_BONUS_RATE = 0.01f                     // 개인그룹 BV의 1%

        // For PIN - SP
        const val SP_PV_MINIMUM = 1_000.0f                  // SP의 최저 개인그룹 PV
        const val SP_PV_CONDITION = 400.0f                  // SP의 최저 개인그룹 PV with 1 21% group
    }
}