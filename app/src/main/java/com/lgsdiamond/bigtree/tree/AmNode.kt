package com.lgsdiamond.bigtree.tree

import android.content.ContentValues
import com.lgsdiamond.bigtree.amway.*
import java.util.*

// WideTreeNodeObserver

interface WideTreeNodeObserver {
    fun notifyDataChanged(node: AmNode)
    fun notifyNodeAdded(node: AmNode, parent: AmNode)
    fun notifyNodeRemoved(node: AmNode, parent: AmNode)
}

// subclass of TreeNode for Amway
class AmNode(member: AmMember) : TreeNode<AmMember>(member) {
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0
    var treeLevel: Int = 0
    var nodeCount = 1
    private val mTreeNodeObservers = ArrayList<WideTreeNodeObserver>()

    init {
        member.node = this@AmNode
    }

    val isIndependent: Boolean
        get() = ((content is ABO) && (content as ABO).isIndependent)

    fun addMemberNode(node: AmNode): AmNode {
        addChild(node)
        val member = node.content
        member.pv.addGroupToSponsor()

        notifyParentNodeCountChanged()

        for (observer in treeNodeObservers) {
            observer.notifyNodeAdded(node, this)
        }
        return this@AmNode
    }

    fun measureLevel(curLevel: Int = 0): Int {
        return if (parent == null) {
            curLevel
        } else {
            (parent as AmNode).measureLevel(curLevel + 1)
        }
    }

    fun resetBase() {
        content.resetBase()
        children.forEach { (it as AmNode).resetBase() }
    }

    // adding
    fun addNetwork3() {
        // 642 Model - First Step
        for (i in 1..3) {
            addMemberNode(AmNode(ABO("A-$i")))
        }
    }

    fun addNetwork6() {
        // 642 Model - First Step
        for (i in 1..6) {
            addMemberNode(AmNode(ABO("A-$i")))
        }
    }

    fun addNetwork64() {
        // 642 Model - Second Step
        for (i in 1..6) {
            val member = AmNode(ABO("A-$i"))
            addMemberNode(member)
            for (j in 1..4) {
                member.addMemberNode(AmNode(ABO("B-$i$j")))
            }
        }
    }

    fun addNetwork642() {
        // 642 Model - Third Step
        for (i in 1..6) {
            val member = AmNode(ABO("A-$i"))
            addMemberNode(member)
            for (j in 1..4) {
                val subMember = AmNode(ABO("B-$i$j"))
                member.addMemberNode(subMember)
                for (k in 1..2) {
                    subMember.addMemberNode(AmNode(ABO("C-$i$j$k")))
                }
            }
        }
    }

    fun containsSponsorNode(sponsorNode: AmNode): Boolean {
        if (this == sponsorNode) return true
        if (parent == null) return false
        return if (sponsorNode == parent) true else (parent as AmNode).containsSponsorNode(sponsorNode)
    }

    // for DB use
    fun node2values(): ContentValues {
        val nodeValues = ContentValues()
        nodeValues.put(DBKeysMembers.KEY_MEMBER_CLASS.key, content.memberClass.ordinal)
        nodeValues.put(DBKeysMembers.KEY_ID_STAMP.key, content.getIDStamp())
        nodeValues.put(DBKeysMembers.KEY_MEMBER_NAME.key, content.name)
        nodeValues.put(DBKeysMembers.KEY_PV_PERSONAL.key, content.pv.personal)
        val sponsorID = content.sponsor?.getIDStamp() ?: 0L
        nodeValues.put(DBKeysMembers.KEY_SPONSOR_ID.key, sponsorID)
        return nodeValues
    }

    fun addUpContentValues(valuesArray: ArrayList<ContentValues>) {
        valuesArray.add(node2values())
        children.forEach { (it as AmNode).addUpContentValues(valuesArray) }
    }

    companion object {
        var focused: AmNode? = null

        data class MemberWithSponsorID(val member: AmMember, val sponsorID: Long)

        fun values2member(values: ContentValues): MemberWithSponsorID {
            val memberClass = AmMemberClass.values()[
                    values.getAsInteger(DBKeysMembers.KEY_MEMBER_CLASS.key)]
            val name = values.getAsString(DBKeysMembers.KEY_MEMBER_NAME.key)
            val pvPersonal = values.getAsFloat(DBKeysMembers.KEY_PV_PERSONAL.key)
            val member = when (memberClass) {
                AmMemberClass.ABO -> ABO(name, pvPersonal)
                AmMemberClass.ON_MEMBER -> OnMember(name, pvPersonal)
                else -> OffMember(name, pvPersonal)
            }
            member.setIDStamp(values.getAsLong(DBKeysMembers.KEY_ID_STAMP.key))
            val sponsorID = values.getAsLong(DBKeysMembers.KEY_SPONSOR_ID.key)
            return MemberWithSponsorID(member, sponsorID)
        }
    }

    var member: AmMember
        get() = content
        set(data) {
            content = data

            for (observer in treeNodeObservers) {
                observer.notifyDataChanged(this)
            }
        }

    private val treeNodeObservers: ArrayList<WideTreeNodeObserver>
        get() {
            var observers = mTreeNodeObservers
            if (observers.isEmpty() && parent != null) {
                observers = (parent as AmNode).treeNodeObservers
            }
            return observers
        }

    internal fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    val hasChildren: Boolean
        get() = children.isNotEmpty()
    val hasParent: Boolean
        get() = (parent != null)

    private fun notifyParentNodeCountChanged() {
        if (parent != null) {
            (parent as AmNode).notifyParentNodeCountChanged()
        } else {
            calculateNodeCount()
        }
    }

    private fun calculateNodeCount(): Int {
        var size = 1

        for (child in children) {
            size += (child as AmNode).calculateNodeCount()
        }

        nodeCount = size
        return nodeCount
    }

    fun addMemberNodes(vararg children: AmNode) {
        addMemberNodes(Arrays.asList(*children))
    }

    fun addMemberNodes(children: List<AmNode>) {
        for (child in children) {
            addMemberNode(child)
        }
    }

    fun removeMemberNode(child: AmNode) {
        child.parent = null
        children.remove(child)

        notifyParentNodeCountChanged()

        for (observer in treeNodeObservers) {
            observer.notifyNodeRemoved(child, this)
        }
    }

    internal fun addTreeNodeObserver(observer: WideTreeNodeObserver) {
        mTreeNodeObservers.add(observer)
    }

    internal fun removeTreeNodeObserver(observer: WideTreeNodeObserver) {
        mTreeNodeObservers.remove(observer)
    }

    override fun toString(): String {
        var indent = "\t"
        for (i in 0 until y / 10) {
            indent += indent
        }
        return "\n" + indent + "TreeNode{" +
                " member=" + content +
                ", mX=" + x +
                ", mY=" + y +
                ", mChildren=" + children +
                '}'.toString()
    }

    fun isFirstChild(node: AmNode): Boolean {
        return children.indexOf(node) == 0
    }
}