package com.lgsdiamond.bigtree.tree

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.NonNull
import android.support.annotation.Nullable
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.util.*

interface LayoutItemType {
    val layoutId: Int
}

open class TreeNode<T : LayoutItemType>(var content: T) : Cloneable {
    var parent: TreeNode<*>? = null
    val children: MutableList<TreeNode<*>> = ArrayList()

    var isExpand: Boolean = false
        private set
    var isLocked: Boolean = false
        private set

    //the tree level
    private var level = UNDEFINE

    val isRoot: Boolean
        get() = (parent == null)

    val isLeaf: Boolean
        get() = children.isEmpty()

    fun getLevel(): Int {
        if (isRoot)
            level = 0
        else if (level == UNDEFINE)
            level = parent!!.getLevel() + 1

        return level
    }

    fun setChildList(newChildList: List<TreeNode<*>>) {
        children.clear()
        for (treeNode in newChildList) {
            addChild(treeNode)
        }
    }

    open fun addChild(node: TreeNode<*>): TreeNode<*> {
        children.add(node)
        node.parent = this
        return this
    }

    fun toggle(): Boolean {
        isExpand = !isExpand
        return isExpand
    }

    private fun collapse() {
        if (isExpand) {
            isExpand = false
        }
    }

    fun collapseAll() {
        collapse()
        children.forEach { it.collapseAll() }
    }

    fun expand() {
        if (!isExpand) {
            isExpand = true
        }
    }

    fun expandAll() {
        expand()
        children.forEach { it.expandAll() }
    }

    fun lock(): TreeNode<T> {
        isLocked = true
        return this
    }

    fun unlock(): TreeNode<T> {
        isLocked = false
        return this
    }

    override fun toString(): String {
        return "TreeNode{" +
                "content=" + this.content +
                ", parent=" + (if (parent == null) "null" else parent!!.content.toString()) +
                ", children=" + (if (children.isEmpty()) "empty" else children.toString()) +
                ", isExpand=" + isExpand +
                '}'.toString()
    }

    override fun clone(): TreeNode<T> {
        val clone = TreeNode(this.content)
        clone.isExpand = this.isExpand
        return clone
    }

    fun makeClone(): TreeNode<T> {
        return clone()
    }

    companion object {
        private const val UNDEFINE = -1
    }
}

class TreeAdapter(nodes: List<TreeNode<*>>, private val binders: List<TreeBinder<*>>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val displayNodes: MutableList<TreeNode<*>> = ArrayList()
    private var padding = 30
    private var onTreeNodeListener: OnTreeNodeListener? = null
    private var toCollapseChild: Boolean = false

    val displayNodesIterator: Iterator<TreeNode<*>>
        get() = displayNodes.iterator()

    init {
        findDisplayNodes(nodes)
    }

    private fun findDisplayNodes(nodes: List<TreeNode<*>>) {
        for (node in nodes) {
            displayNodes.add(node)
            if (!node.isLeaf && node.isExpand)
                findDisplayNodes(node.children as List<TreeNode<*>>)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return displayNodes[position].content.layoutId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false)
        if (binders.size == 1)
            return binders[0].provideViewHolder(v)
        for (viewBinder in binders) {
            if (viewBinder.layoutId == viewType)
                return viewBinder.provideViewHolder(v)
        }
        return binders[0].provideViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        if (!payloads.isEmpty()) {
            val b = payloads[0] as Bundle
            for (key in b.keySet()) {
                when (key) {
                    KEY_IS_EXPAND -> if (onTreeNodeListener != null)
                        onTreeNodeListener!!.onToggle(b.getBoolean(key), holder)
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    private fun toggleItemView(holder: RecyclerView.ViewHolder) {
        val selectedNode = displayNodes[holder.layoutPosition]
        // Prevent multi-click during the short interval.
        try {
            val lastClickTime = holder.itemView.tag as Long
            if (System.currentTimeMillis() - lastClickTime < 500)
                return
        } catch (e: Exception) {
            holder.itemView.tag = System.currentTimeMillis()
        }

        holder.itemView.tag = System.currentTimeMillis()

        if (onTreeNodeListener != null && onTreeNodeListener!!.onClick(selectedNode, holder))
            return
        if (selectedNode.isLeaf)
            return
        // This TreeNode was locked to click.
        if (selectedNode.isLocked) return
        val isExpand = selectedNode.isExpand
        val positionStart = displayNodes.indexOf(selectedNode) + 1
        if (!isExpand) {
            notifyItemRangeInserted(positionStart, addChildNodes(selectedNode, positionStart))
        } else {
            notifyItemRangeRemoved(positionStart, removeChildNodes(selectedNode, true))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.setPadding(displayNodes[position].getLevel() * padding, 3, 3, 3)

        holder.itemView.setOnClickListener {
            toggleItemView(holder)
        }

        for (viewBinder in binders) {
            if (viewBinder.layoutId == displayNodes[position].content.layoutId) {
                val binder = viewBinder as TreeBinder<RecyclerView.ViewHolder>
//                val binder = viewBinder as TreeBinder<RecyclerView.ViewHolder>
                binder.bindView(holder, position, displayNodes[position])
            }
        }
    }

    private fun addChildNodes(pNode: TreeNode<*>, startIndex: Int): Int {
        val nodeChildList = pNode.children
        var addChildCount = 0
        for (treeNode in nodeChildList) {
            displayNodes.add(startIndex + addChildCount++, treeNode)
            if (treeNode.isExpand) {
                addChildCount += addChildNodes(treeNode, startIndex + addChildCount)
            }
        }
        if (!pNode.isExpand)
            pNode.toggle()
        return addChildCount
    }

    private fun removeChildNodes(pNode: TreeNode<*>, shouldToggle: Boolean = true): Int {
        if (pNode.isLeaf)
            return 0
        val nodeChildList = pNode.children
        var removeChildCount = nodeChildList.size
        displayNodes.removeAll(nodeChildList)
        for (child in nodeChildList) {
            if (child.isExpand) {
                if (toCollapseChild)
                    child.toggle()
                removeChildCount += removeChildNodes(child, false)
            }
        }
        if (shouldToggle)
            pNode.toggle()
        return removeChildCount
    }

    override fun getItemCount(): Int {
        return displayNodes.size
    }

    fun setPadding(padding: Int) {
        this.padding = padding
    }

    fun ifCollapseChildWhileCollapseParent(toCollapseChild: Boolean) {
        this.toCollapseChild = toCollapseChild
    }

    fun setOnTreeNodeListener(onTreeNodeListener: OnTreeNodeListener) {
        this.onTreeNodeListener = onTreeNodeListener
    }

    interface OnTreeNodeListener {
        /**
         * called when TreeNodes were clicked.
         *
         * @return weather consume the click event.
         */
        fun onClick(node: TreeNode<*>, holder: RecyclerView.ViewHolder): Boolean

        /**
         * called when TreeNodes were toggle.
         *
         * @param isExpand the status of TreeNodes after being toggled.
         */
        fun onToggle(isExpand: Boolean, holder: RecyclerView.ViewHolder)
    }

    fun refresh(treeNodes: List<TreeNode<*>>) {
        displayNodes.clear()
        findDisplayNodes(treeNodes)
        notifyDataSetChanged()
    }

    private fun notifyDiff(temp: List<TreeNode<*>>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return temp.size
            }

            override fun getNewListSize(): Int {
                return displayNodes.size
            }

            // judge if the same items
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return this@TreeAdapter.areItemsTheSame(temp[oldItemPosition], displayNodes[newItemPosition])
            }

            // if they are the same items, whether the contents has bean changed.
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return this@TreeAdapter.areContentsTheSame(temp[oldItemPosition], displayNodes[newItemPosition])
            }

            @Nullable
            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                return this@TreeAdapter.getChangePayload(temp[oldItemPosition], displayNodes[newItemPosition])
            }
        })
        diffResult.dispatchUpdatesTo(this)
    }

    private fun getChangePayload(oldNode: TreeNode<*>, newNode: TreeNode<*>): Any? {
        val diffBundle = Bundle()
        if (newNode.isExpand != oldNode.isExpand) {
            diffBundle.putBoolean(KEY_IS_EXPAND, newNode.isExpand)
        }
        return if (diffBundle.size() == 0) null else diffBundle
    }

    // For DiffUtil, if they are the same items, whether the contents has bean changed.
    private fun areContentsTheSame(oldNode: TreeNode<*>, newNode: TreeNode<*>): Boolean {
        return (oldNode.content == newNode.content
                && oldNode.isExpand == newNode.isExpand)
    }

    // judge if the same item for DiffUtil
    private fun areItemsTheSame(oldNode: TreeNode<*>, newNode: TreeNode<*>): Boolean {
        return oldNode.content == newNode.content
    }

    /**
     * collapse all root nodes.
     */
    fun collapseAll() {
        // Back up the nodes are displaying.
        val temp = backupDisplayNodes()
        //find all root nodes.
        val roots = ArrayList<TreeNode<*>>()
        for (displayNode in displayNodes) {
            if (displayNode.isRoot)
                roots.add(displayNode)
        }
        //Close all root nodes.
        for (root in roots) {
            if (root.isExpand)
                removeChildNodes(root)
        }
        notifyDiff(temp)
    }

    @NonNull
    private fun backupDisplayNodes(): List<TreeNode<*>> {
        val temp = ArrayList<TreeNode<*>>()
        for (displayNode in displayNodes) {
            try {
                temp.add(displayNode.makeClone())
            } catch (e: CloneNotSupportedException) {
                temp.add(displayNode)
            }

        }
        return temp
    }

    fun collapseNode(pNode: TreeNode<*>) {
        val temp = backupDisplayNodes()
        removeChildNodes(pNode)
        notifyDiff(temp)
    }

    fun collapseBrotherNode(pNode: TreeNode<*>) {
        val temp = backupDisplayNodes()
        if (pNode.isRoot) {
            val roots = ArrayList<TreeNode<*>>()
            for (displayNode in displayNodes) {
                if (displayNode.isRoot)
                    roots.add(displayNode)
            }
            //Close all root nodes.
            for (root in roots) {
                if (root.isExpand && root != pNode)
                    removeChildNodes(root)
            }
        } else {
            val parent = pNode.parent ?: return
            val pChildList = parent.children
            for (node in pChildList) {
                if (node == pNode || !node.isExpand)
                    continue
                removeChildNodes(node)
            }
        }
        notifyDiff(temp)
    }

    companion object {
        private const val KEY_IS_EXPAND = "IS_EXPAND"
    }

}

abstract class TreeBinder<VH : RecyclerView.ViewHolder> : LayoutItemType {
    abstract fun provideViewHolder(itemView: View): VH

    abstract fun bindView(holder: VH, position: Int, node: TreeNode<*>)

    open class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

        protected fun <T : View> findViewById(@IdRes id: Int): T {
            return itemView.findViewById<View>(id) as T
        }
    }

}