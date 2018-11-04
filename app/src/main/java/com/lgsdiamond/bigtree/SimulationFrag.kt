package com.lgsdiamond.bigtree

import com.lgsdiamond.bigtree.amway.ABO
import com.lgsdiamond.bigtree.amway.AmMonth
import com.lgsdiamond.bigtree.tree.AmNode

open class SimulationFragment : ExerciseFragment() {
    override val barTitle = "네트워크 시뮬레이션"
    override val dbTableName = "SIMULATION"

    init {
        currentMonth = AmMonth(2018, 9)
    }

    override fun makeDefaultNetwork(): MutableList<AmNode> {
        val aMemberNodes: MutableList<AmNode> = ArrayList()

        val self = AmNode(ABO("나"))
        aMemberNodes.add(self)
        self.resetBase()

        return aMemberNodes
    }
}