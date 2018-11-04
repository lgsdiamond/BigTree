package com.lgsdiamond.bigtree

import com.lgsdiamond.bigtree.amway.*
import com.lgsdiamond.bigtree.tree.AmNode

class PlanningFragment : SimulationFragment() {
    override val barTitle = "나의 네트워크 계획"
    override val dbTableName = "PLANNING"

    init {
        currentMonth = AmMonth(2018, 9)
    }

    override fun makeDefaultNetwork(): MutableList<AmNode> {
        val aMemberNodes: MutableList<AmNode> = ArrayList()

        val self = AmNode(ABO(gMainActivity.registeredUserName))
        self.addMemberNode(AmNode(ABO("파트너 A")))
        self.addMemberNode(AmNode(ABO("파트너 B")))
        self.addMemberNode(AmNode(ABO("파트너 C")))
        self.addMemberNode(AmNode(OnMember("회원 A")))
        self.addMemberNode(AmNode(OffMember("소비자 A")))

        aMemberNodes.add(self)
        self.resetBase()

        return aMemberNodes
    }
}