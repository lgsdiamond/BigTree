package com.lgsdiamond.bigtree.amway

enum class PinTitle(val fullName: String) { NONE("No-PIN"),
    SP("Silver Producer"), SPS("Silver Producer Sponsor"), GP("Gold Producer"),
    PT("Platinum"), F_PT("Founders Platinum"),
    RB("Ruby"), F_RB("Founders Ruby"),
    SA("Sapphire"), F_SA("Founders Sapphire"),
    EM("Emerald"), F_EM("Founders Emerald"),
    DIA("Diamond"), F_DIA("Founders Diamond"),
    EDC("Executive Diamond"), F_EDC("Founders Executive Diamond"),
    DDC("Double Diamond"), F_DDC("Founders Double Diamond"),
    TDC("Triple Diamond"), F_TDC("Founders Triple Diamond"),
    CR("Crown"), F_CR("Founders Crown"),
    CA("Crown Ambassador"), F_CA("Founders Crown Ambassador");
}

class MemberPin(val abo: ABO) {
    var current: PinTitle = PinTitle.NONE
    var firstSP: AmMonth? = null
    var fitstPT: AmMonth? = null
    val record: ArrayList<MonthlyRecord>
        get() = abo.monthlyRecords

    fun isQualified(pin: PinTitle, month: AmMonth): Boolean {
        return when (pin) {
            PinTitle.NONE -> {
                true
            }
            PinTitle.SP -> {
                when {
                    // 1) 1개월간 개인 그룹에서 1,000만 PV 이상을 달성한 경우
                    (abo.pv.personalGroup >= AmSystem.SP_PV_MINIMUM) -> true

                    // 2) 21%보너스 수준을 달성한 그룹 1개 이상을 직접 또는 대리로 후원하고,
                    // 개인 그룹에서 400만 PV 이상 달성한 경우, 미만은 SPS
                    ((abo.independentPartners.size == 1) && (abo.pv.personalGroup >= AmSystem.SP_PV_CONDITION)) -> true

                    // 3) 21% 보너스 수준을 달성한 그룹 2개 이상을 직접 또는 대리로 후원한 경우
                    (abo.independentPartners.size >= 2) -> true
                    else -> false
                }
            }
            PinTitle.SPS -> {
                when {
                    // 2) 21%보너스 수준을 달성한 그룹 1개 이상을 직접 또는 대리로 후원하고, 개인 그룹에서 400만 PV 미만은 SPS
                    ((abo.independentPartners.size == 1) && (abo.pv.personalGroup < AmSystem.SP_PV_CONDITION)) -> true
                    else -> false
                }
            }
            PinTitle.GP -> {
                // 1) 유자격 실버 프로듀서의 조건을 1년 이내에 3개월 달성한 경우 골드 프로듀서로 인정됩니다.
                // 단, 이 3개월은 반드시 연속적일 필요는 없습니다.
                val (count, _) = countPINWithin12Month(PinTitle.SP, month)
                when {
                    (count >= 3) -> true
                    else -> false
                }
                // 2) 골드 프로듀서 재달성 조건 : 한 회계연도 이상 골드 프로듀서 자격을 유지하지 못하다가 골드 프로듀서 자격 재
                // 달성에 도전하는 경우 회계연도 내에 3번째 SP 유자격 월에 GP 자격이 재 달성됩니다. 두 회계연도에 걸쳐서 3달
                // 이상의 SP 유자격 월을 달성한 경우에도 한 회계연도 내에 3달 이상의 SP 유자격 월을 달성해야 합니다.
            }
            PinTitle.PT -> {
                // (1) 유자격 실버 프로듀서의 자격을 달성한 달로부터 1년 이내에
                // (2) 유자격 실버 프로듀서 자격 조건을 6개월 이상 달성하고
                // (3) 이 6개월 가운데 최소 3개월은 연속적으로 달성한 경우 플래티늄으로 인정됩니다.
                val (count, consequence) = countPINWithinPF(PinTitle.SP, month)
                when {
                    ((count >= 6) && (consequence >= 3)) -> true
                }
                false
            }
            PinTitle.F_PT -> {
                /* 1회계연도 내에 유자격 SP 조건을 12개월 동안 달성한 ABO */
                false
            }
            PinTitle.RB -> {
                /* 1개월간 개인그룹 PV를 2,000만 이상 달성한 유자격 플래티늄.
                유자격 플래티늄 미만인 경우, 루비실적 달성 후 동일회계연도내에 플래티늄 자격을 달성해야 하며
                플래티늄을 달성한 달에 플래티늄과 루비가 동시에 인정됩니다. */

                false
            }
            PinTitle.F_RB -> {
                /* 1회계연도 내에 루비 자격 조건을 12개월 동안 달성한 ABO */

                false
            }
            PinTitle.SA -> {
                /* 하기의 사파이어 유자격 월 조건을 회계연도 중에 6개월 이상 달성할 경우
                ① 두개의 21% 보너스 수준을 달성한 레그와 400만 PV 이상의 개인그룹실적(Award PV)를 달성하였거나
                (이때 두 개의 21% 보너스 수준을 달성한 레그는 매월 동일할 필요는 없습니다.)
                ② 두개의 21% 보너스 수준을 달성한 레그 이외에 추가로 한 개의 21% 보너스 수준의  레그를 달성하였을 경우 */

                false
            }
            PinTitle.F_SA -> {
                /* 1회계연도 내에 사파이어 자격 조건을 12개월 동안 달성한 ABO */

                false
            }
            PinTitle.EM -> {
                /* 1회계연도 내에 6개월 이상 유자격 월을 달성하고 있는 그룹을 3개 이상 직접, 대리 또는 국제후원하고 있는 ABO */

                false
            }
            PinTitle.F_EM -> {
                /* 1회계연도 내에 12개월 동안 유자격 월을 달성하고 있는 그룹을 3개 이상 직접, 대리 또는 국제후원하고 있는 ABO */

                false
            }
            PinTitle.DIA -> {
                /* 1회계연도 내에 6개월 이상 유자격 월을 달성하고 있는 그룹을 6개 이상 직접,
                대리 또는 국제후원하고 있는 ABO */

                false
            }
            PinTitle.F_DIA -> {
                /* 1회계연도 내에 12개월 동안 유자격 월을 달성하고 있는 그룹을 6개 이상 직접, 대리 또는 국제후원하고 있거나,
                FAA 점수를 8점 이상 달성한 다이아몬드(단, FAA 점수에 의한 파운더스 다이아몬드 달성은
                다이아몬드 보너스 수혜자에 한하여 인정) */

                false
            }
            PinTitle.EDC -> {
                /* 1회계연도 내에 6개월 이상 유자격 월을 달성하고 있는 그룹을 9개 이상 직접, 대리 또는 국제후원하고 있거나,
                FAA 점수를 10점 이상 달성한 다이아몬드 */

                false
            }
            PinTitle.F_EDC -> {
                /* 1회계연도 내에 12개월 동안 유자격 월을 달성하고 있는 그룹을 9개 이상 직접, 대리 또는 국제후원하고 있거나,
                FAA 점수를 12점 이상 달성한 다이아몬드 */

                false
            }
            PinTitle.DDC -> {
                /* 1회계연도 내에 6개월 이상 유자격 월을 달성하고 있는 그룹을 12개 이상 직접, 대리 또는 국제후원하고 있거나,
                FAA 점수를 14점 이상 달성한 다이아몬드 */

                false
            }
            PinTitle.F_DDC -> {
                /* 1회계연도 내에 12개월 동안 유자격 월을 달성하고 있는 그룹을 12개 이상 직접, 대리 또는 국제후원하고 있거나,
                FAA 점수를 16점 이상 달성한 다이아몬드 */

                false
            }
            PinTitle.TDC -> {
                /* 1회계연도 내에 6개월 이상 유자격 월을 달성하고 있는 그룹을 15개 이상 직접, 대리 또는 국제후원하고 있거나,
                FAA 점수를 18점 이상 달성한 다이아몬드 */

                false
            }
            PinTitle.F_TDC -> {
                /* 1회계연도 내에 12개월 동안 유자격 월을 달성하고 있는 그룹을 15개 이상 직접, 대리 또는 국제후원하고 있거나,
                FAA 점수를 20점 이상 달성한 다이아몬드 */

                false
            }
            PinTitle.CR -> {
                /* 1회계연도 내에 6개월 이상 유자격 월을 달성하고 있는 그룹을 18개 이상 직접, 대리 또는 국제후원하고 있거나,
                FAA 점수를 22점 이상 달성한 다이아몬드 */

                false
            }
            PinTitle.F_CR -> {
                false
            }
            PinTitle.CA -> {
                false
            }
            PinTitle.F_CA -> {
                false
            }
        }
    }

    data class CountWithConsequence(val count: Int, val consequence: Int)

    fun evaluate(month: AmMonth) {
        val titles = PinTitle.values()
        titles.size
        for (i in (titles.size - 1) downTo 0) {
            if (isQualified(titles[i], month)) {
                current = titles[i]
                break
            }
        }
    }

    fun countPINWithinPF(qPIN: PinTitle, curMonth: AmMonth): CountWithConsequence {
        // 예) 2018년 2월 기준 회계년도 이내 = 2017년 9월 ~ 2018년 2월
        return countPINWithin(qPIN, curMonth.startPF, curMonth)
    }

    fun countPINWithin12Month(qPIN: PinTitle, curMonth: AmMonth): CountWithConsequence {
        // 예) 2018년 2월 기준 1년 이내 = 2017년 3월 ~ 2018년 2월
        // test
        return countPINWithin(qPIN, AmMonth(curMonth.year - 1, curMonth.month + 1), curMonth)
    }

    fun countPINWithin(qPIN: PinTitle, startMonth: AmMonth, curMonth: AmMonth): CountWithConsequence {

        var countMonth = 0
        for (month in startMonth..curMonth) {
            countMonth += month.month
        }

        if (record.isEmpty()) return CountWithConsequence(0, 0)

        var startRecord: MonthlyRecord? = null
        var startIndex = -1
        for ((index, record) in record.withIndex()) {
            if (record.month >= startMonth) {
                startRecord = record
                startIndex = index
                break
            }
        }
        if (startRecord == null) return CountWithConsequence(0, 0)

        var count = 0
        var consequence = 0
        for (i in startIndex..(record.size - 1)) {
            if (record[i].pin == qPIN) {
                count++             // 횟수 증가
                consequence++       // 연속횟수 증가
            } else {
                consequence = 0     // 연속횟수 reset
            }
        }

        return CountWithConsequence(count, consequence)
    }
}