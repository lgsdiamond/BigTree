package com.lgsdiamond.bigtree.tree

import android.content.Context
import android.database.DataSetObservable
import android.database.DataSetObserver
import android.graphics.*
import android.os.Build
import android.support.annotation.ColorInt
import android.support.annotation.LayoutRes
import android.support.annotation.Px
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.*
import android.widget.Adapter
import android.widget.AdapterView
import com.lgsdiamond.bigtree.R
import com.lgsdiamond.bigtree.amway.AmMember
import java.util.*

object AlgorithmFactory {

    fun createBuchheimWalker(configuration: BuchheimWalkerConfiguration): Algorithm {
        return BuchheimWalkerAlgorithm(configuration)
    }

    fun createDefaultBuchheimWalker(): Algorithm {
        return BuchheimWalkerAlgorithm()
    }
}

class BuchheimWalkerConfiguration(val siblingSeparation: Int, val subtreeSeparation: Int)


class BuchheimWalkerAlgorithm @JvmOverloads constructor(private val mConfiguration:
                                                        BuchheimWalkerConfiguration
                                                        = BuchheimWalkerConfiguration(DEFAULT_SIBLING_SEPARATION, DEFAULT_SUBTREE_SEPARATION)) : Algorithm {
    private val mNodeData = HashMap<AmNode, BuchheimWalkerNodeData>()

    private fun createNodeData(node: AmNode): BuchheimWalkerNodeData {
        val nodeData = BuchheimWalkerNodeData()
        nodeData.ancestor = node
        mNodeData[node] = nodeData

        return nodeData
    }

    private fun getNodeData(node: AmNode?): BuchheimWalkerNodeData {
        return mNodeData[node]!!
    }

    private fun firstWalk(node: AmNode, depth: Int, number: Int) {
        val nodeData = createNodeData(node)
        nodeData.depth = depth
        nodeData.number = number

        if (isLeaf(node)) {
            // if the node has no left sibling, prelim(node) should be set to 0, but we don't have to set it
            // here, because it's already initialized with 0
            if (hasLeftSibling(node)) {
                val leftSibling = getLeftSibling(node)
                nodeData.prelim = getPrelim(leftSibling) + getSpacing(leftSibling!!, node)
            }
        } else {
            val leftMost = getLeftMostChild(node)
            val rightMost = getRightMostChild(node)
            var defaultAncestor = leftMost

            var next: AmNode? = leftMost
            var i = 1
            while (next != null) {
                firstWalk(next, depth + 1, i++)
                defaultAncestor = apportion(next, defaultAncestor)

                next = getRightSibling(next)
            }

            executeShifts(node)

            val midPoint = 0.5 * (getPrelim(leftMost) + getPrelim(rightMost) + rightMost!!.width - node.width)

            if (hasLeftSibling(node)) {
                val leftSibling = getLeftSibling(node)
                nodeData.prelim = getPrelim(leftSibling) + getSpacing(leftSibling!!, node)
                nodeData.modifier = nodeData.prelim - midPoint
            } else {
                nodeData.prelim = midPoint
            }
        }
    }

    private fun secondWalk(node: AmNode, modifier: Double) {
        val nodeData = getNodeData(node)
        node.x = (nodeData.prelim.toInt() + modifier.toInt())
        node.y = nodeData.depth
        node.treeLevel = nodeData.depth

        for (w in node.children) {
            secondWalk((w as AmNode), modifier + nodeData.modifier)
        }
    }

    private fun executeShifts(node: AmNode) {
        var shift = 0.0
        var change = 0.0
        var w = getRightMostChild(node)
        while (w != null) {
            val nodeData = getNodeData(w)

            nodeData.prelim = nodeData.prelim + shift
            nodeData.modifier = nodeData.modifier + shift
            change += nodeData.change
            shift += nodeData.shift + change

            w = getLeftSibling(w)
        }
    }

    private fun apportion(node: AmNode, ancestor: AmNode): AmNode {
        var defaultAncestor = ancestor
        if (hasLeftSibling(node)) {
            val leftSibling = getLeftSibling(node)

            var vip = node
            var vop: AmNode? = node
            var vim = leftSibling
            var vom: AmNode? = getLeftMostChild((vip.parent!! as AmNode))

            var sip = getModifier(vip)
            var sop = getModifier(vop)
            var sim = getModifier(vim)
            var som = getModifier(vom)

            var nextRight = nextRight(vim!!)
            var nextLeft = nextLeft(vip)

            while (nextRight != null && nextLeft != null) {
                vim = nextRight
                vip = nextLeft
                vom = nextLeft(vom!!)
                vop = nextRight(vop!!)

                setAncestor(vop, node)

                val shift = getPrelim(vim) + sim - (getPrelim(vip) + sip) + getSpacing(vim, vip)
                if (shift > 0) {
                    moveSubtree(ancestor(vim, node, defaultAncestor), node, shift)
                    sip += shift
                    sop += shift
                }

                sim += getModifier(vim)
                sip += getModifier(vip)
                som += getModifier(vom)
                sop += getModifier(vop)

                nextRight = nextRight(vim)
                nextLeft = nextLeft(vip)
            }

            if (nextRight != null && nextRight(vop!!) == null) {
                setThread(vop, nextRight)
                setModifier(vop, getModifier(vop) + sim - sop)
            }

            if (nextLeft != null && nextLeft(vom!!) == null) {
                setThread(vom, nextLeft)
                setModifier(vom, getModifier(vom) + sip - som)
                defaultAncestor = node
            }
        }

        return defaultAncestor
    }

    private fun setAncestor(v: AmNode?, ancestor: AmNode) {
        getNodeData(v).ancestor = ancestor
    }

    private fun setModifier(v: AmNode, modifier: Double) {
        getNodeData(v).modifier = modifier
    }

    private fun setThread(v: AmNode, thread: AmNode) {
        getNodeData(v).thread = thread
    }

    private fun getPrelim(v: AmNode?): Double {
        return getNodeData(v).prelim
    }

    private fun getModifier(vip: AmNode?): Double {
        return getNodeData(vip).modifier
    }

    private fun moveSubtree(wm: AmNode, wp: AmNode, shift: Double) {
        val wpNodeData = getNodeData(wp)
        val wmNodeData = getNodeData(wm)

        val subtrees = wpNodeData.number - wmNodeData.number
        wpNodeData.change = wpNodeData.change - shift / subtrees
        wpNodeData.shift = wpNodeData.shift + shift
        wmNodeData.change = wmNodeData.change + shift / subtrees
        wpNodeData.prelim = wpNodeData.prelim + shift
        wpNodeData.modifier = wpNodeData.modifier + shift
    }

    private fun ancestor(vim: AmNode, node: AmNode, defaultAncestor: AmNode): AmNode {
        val vipNodeData = getNodeData(vim)

        return if (vipNodeData.ancestor!!.parent === node.parent) {
            vipNodeData.ancestor!!
        } else defaultAncestor

    }

    private fun nextRight(node: AmNode): AmNode? {
        return if (node.hasChildren) {
            getRightMostChild(node)
        } else getNodeData(node).thread

    }

    private fun nextLeft(node: AmNode): AmNode? {
        return if (node.hasChildren) {
            getLeftMostChild(node)
        } else getNodeData(node).thread

    }

    private fun getSpacing(leftNode: AmNode, rightNode: AmNode): Int {
        return mConfiguration.siblingSeparation + leftNode.width
    }

    private fun isLeaf(node: AmNode): Boolean {
        return node.children.isEmpty()
    }

    private fun getLeftSibling(node: AmNode): AmNode? {
        if (!hasLeftSibling(node)) {
            return null
        }

        val parent = node.parent
        val children = parent!!.children
        val nodeIndex = children.indexOf(node)
        return (children[nodeIndex - 1] as AmNode)
    }

    private fun hasLeftSibling(node: AmNode): Boolean {
        val parent = node.parent ?: return false

        val nodeIndex = parent!!.children.indexOf(node)
        return nodeIndex > 0
    }

    private fun getRightSibling(node: AmNode): AmNode? {
        if (!hasRightSibling(node)) {
            return null
        }

        val parent = node.parent
        val children = parent!!.children
        val nodeIndex = children.indexOf(node)
        return (children[nodeIndex + 1] as AmNode)
    }

    private fun hasRightSibling(node: AmNode): Boolean {
        val parent = node.parent ?: return false

        val children = parent.children
        val nodeIndex = children.indexOf(node)
        return nodeIndex < children.size - 1
    }

    private fun getLeftMostChild(node: AmNode): AmNode {
        return (node.children[0] as AmNode)
    }

    private fun getRightMostChild(node: AmNode): AmNode? {
        val children = node.children
        return if (children.isEmpty()) {
            null
        } else (children[children.size - 1] as AmNode)

    }

    override fun run(root: AmNode) {
        mNodeData.clear()

        firstWalk(root, 0, 0)
        secondWalk(root, -getPrelim(root))
    }

    companion object {

        private const val DEFAULT_SIBLING_SEPARATION = 30      // Horizontal Gap
        private const val DEFAULT_SUBTREE_SEPARATION = 0
    }
}

internal class BuchheimWalkerNodeData {
    var ancestor: AmNode? = null
    var thread: AmNode? = null
    var number: Int = 0
    var depth: Int = 0
    var prelim: Double = 0.toDouble()
    var modifier: Double = 0.toDouble()
    var shift: Double = 0.toDouble()
    var change: Double = 0.toDouble()
}

abstract class BaseWideTreeAdapter<VH>(context: Context, @param:LayoutRes private var mLayoutRes: Int) : WideTreeAdapter<VH> {
    private var mRootNode: AmNode? = null

    private var mAlgorithm: Algorithm? = null

    private val mLayoutInflater: LayoutInflater

    private val mDataSetObservable = DataSetObservable()

    fun assignLayoutID(id: Int) {
        mLayoutRes = id
    }

    override var algorithm: Algorithm
        get() {
            if (mAlgorithm == null) {
                mAlgorithm = AlgorithmFactory.createDefaultBuchheimWalker()
            }

            return mAlgorithm!!
        }
        set(algorithm) {
            requireNotNull(algorithm) { "algorithm can't be null" }
            mAlgorithm = algorithm
        }


    override fun getCount(): Int = if (mRootNode != null) mRootNode!!.nodeCount else 0

    override fun getViewTypeCount(): Int = 2

    override fun isEmpty(): Boolean = false

    init {
        mLayoutInflater = LayoutInflater.from(context)
    }

    override fun notifySizeChanged() {
        if (mRootNode != null) {
            algorithm.run(mRootNode!!)
        }
    }

    override fun setRootNode(rootNode: AmNode) {
        requireNotNull(rootNode) { "rootNode can't be null" }

        if (mRootNode != null) {
            (mRootNode as AmNode).removeTreeNodeObserver(this)
        } else if (rootNode == mRootNode) {
            notifyDataChanged(mRootNode!!)
        } else {
            mRootNode = rootNode
            (mRootNode as AmNode).addTreeNodeObserver(this)
            notifyDataChanged(mRootNode!!)
        }
    }

    override fun getNode(position: Int): AmNode? {
        val list = ArrayList<AmNode>()
        list.add(mRootNode!!)

        return if (mRootNode != null) breadthSearch(list, position) else null
    }

    private fun breadthSearch(nodes: List<AmNode>, position: Int): AmNode {
        if (nodes.size > position) {
            return nodes[position]
        }

        val childNodes = ArrayList<AmNode>()
        for (n in nodes) {
            for (child in n.children)
                childNodes.add(child as AmNode)
        }

        return breadthSearch(childNodes, position - nodes.size)
    }

    override fun getScreenPosition(position: Int): Point {
        val node = getNode(position)

        return Point(node!!.x, node.y)
    }

    override fun notifyDataChanged(node: AmNode) {
        mDataSetObservable.notifyChanged()
    }

    override fun notifyNodeAdded(node: AmNode, parent: AmNode) {
        mDataSetObservable.notifyInvalidated()
    }

    override fun notifyNodeRemoved(node: AmNode, parent: AmNode) {
        mDataSetObservable.notifyInvalidated()
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {
        mDataSetObservable.registerObserver(observer)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver) {
        mDataSetObservable.unregisterObserver(observer)
    }

    override fun getItem(position: Int): Any {
        return getNode(position)!!.member
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: VH

        if (convertView == null) {
            view = mLayoutInflater.inflate(getItemViewType(position), parent, false)
            viewHolder = onCreateViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as VH
        }

        val node = getNode(position)
        onBindViewHolder(viewHolder, node!!.member, position)

        return view
    }

    override fun getItemViewType(position: Int): Int {
        return if (getNode(position)!!.isLeaf) R.layout.node_icon else R.layout.node_icon_wide
    }
}

//

interface Algorithm {
    fun run(root: AmNode)
}

interface WideTreeAdapter<VH> : Adapter, WideTreeNodeObserver {

    /**
     * Returns the currently set algorithm. It uses the [BuchheimWalkerAlgorithm] as default,
     * if no algorithm is previously set.
     *
     * @return
     */
    /**
     * Set an algorithm, which is used for laying out the tree.
     *
     * @param algorithm the algorithm to use for laying out the tree
     */
    var algorithm: Algorithm

    fun notifySizeChanged()

    /**
     * Set a new root node. This triggers the re-drawing of the whole view.
     *
     * @param rootNode
     */
    fun setRootNode(rootNode: AmNode)

    /**
     * Returns the node at a given {code position}.
     *
     * @param position
     * @return
     */
    fun getNode(position: Int): AmNode?

    /**
     * Returns the screen position from the node at {code position}
     *
     * @param position
     * @return
     */
    fun getScreenPosition(position: Int): Point

    fun onCreateViewHolder(view: View): VH

    fun onBindViewHolder(viewHolder: VH, member: AmMember, position: Int)
}

// WideTreeView
class WideTreeView : AdapterView<WideTreeAdapter<*>>, GestureDetector.OnGestureListener {

    var mLinePath = Path()
    var mLinePaint = Paint()
    private var mLineThickness: Int = 0
    private var mLineColor: Int = 0
    private var mLevelSeparation: Int = 0

    /**
     * @return `true` if using same size for each node, `false` otherwise.
     */
    var isUsingMaxSize: Boolean = false
        private set

    private lateinit var mAdapterV: WideTreeAdapter<*>
    private var mMaxChildWidth: Int = 0
    private var mMaxChildHeight: Int = 0
    private var mMinChildHeight: Int = 0
    private var mRect: Rect? = null
    private val mBoundaries = Rect()

    private var mDataSetObserver: DataSetObserver? = null

    private var mGestureDetector: GestureDetector? = null

    /**
     * @return Returns the value of how thick the lines between the nodes are.
     */
    /**
     * Sets a new value for the thickness of the lines between the nodes.
     *
     * @param lineThickness new value for the thickness
     */
    var lineThickness: Int
        get() = mLineThickness
        set(lineThickness) {
            mLineThickness = lineThickness
            initPaint()
            invalidate()
        }

    /**
     * @return Returns the color of the lines between the nodes.
     */
    /**
     * Sets a new color for the lines between the nodes.A change to this value
     * invokes a re-drawing of the tree.
     *
     * @param lineColor the new color
     */
    var lineColor: Int
        @ColorInt
        get() = mLineColor
        set(@ColorInt lineColor) {
            mLineColor = lineColor
            initPaint()
            invalidate()
        }

    /**
     * Returns the value of how much space should be used between two levels.
     *
     * @return level separation value
     */
    /**
     * Sets a new value of how much space should be used between two levels. A change to this value
     * invokes a re-drawing of the tree.
     *
     * @param levelSeparation new value for the level separation
     */
    var levelSeparation: Int
        @Px
        get() = mLevelSeparation
        set(@Px levelSeparation) {
            mLevelSeparation = levelSeparation
            invalidate()
            requestLayout()
        }

    private val screenXCenter: Int
        get() = pivotX.toInt() - getChildAt(0).measuredWidth / 2

    @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WideTreeView, 0, 0)
        try {
            mLevelSeparation = a.getDimensionPixelSize(R.styleable.WideTreeView_levelSeparation, DEFAULT_LINE_LENGTH)
            mLineThickness = a.getDimensionPixelSize(R.styleable.WideTreeView_lineThickness, DEFAULT_LINE_THICKNESS)
            mLineColor = a.getColor(R.styleable.WideTreeView_lineColor, DEFAULT_LINE_COLOR)
            isUsingMaxSize = a.getBoolean(R.styleable.WideTreeView_useMaxSize, DEFAULT_USE_MAX_SIZE)
        } finally {
            a.recycle()
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        mGestureDetector = GestureDetector(context, this)

        if (attrs != null) {
            initAttrs(context, attrs)
        }
        initPaint()
    }

    private fun initPaint() {
        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLinePaint.strokeWidth = mLineThickness.toFloat()
        mLinePaint.color = mLineColor
        mLinePaint.style = Paint.Style.STROKE
        mLinePaint.strokeJoin = Paint.Join.ROUND    // set the join to round you want
        //        mLinePaint.strokeCap = Paint.Cap.ROUND ;      // set the paint cap to round too
        mLinePaint.pathEffect = CornerPathEffect(10f)   // set the path effect when they join.
    }

    private fun positionItems() {
        var maxLeft = Integer.MAX_VALUE
        var maxRight = Integer.MIN_VALUE
        var maxTop = Integer.MAX_VALUE
        var maxBottom = Integer.MIN_VALUE

        var globalPadding = 0
        var localPadding = 0
        var currentLevel = 0
        for (index in 0 until mAdapterV.count) {
            val child = getChildAt(index)

            val width = child.measuredWidth
            val height = child.measuredHeight

            val screenPosition = mAdapterV.getScreenPosition(index)
            val node = mAdapterV.getNode(index)

            if (height > mMinChildHeight) {
                localPadding = Math.max(localPadding, height - mMinChildHeight)
            }

            if (currentLevel != node!!.treeLevel) {
                globalPadding += localPadding
                localPadding = 0
                currentLevel = node.treeLevel
            }

            // calculate the size and position of this child
            val left = screenPosition.x + screenXCenter
            val top = screenPosition.y * mMinChildHeight + node.treeLevel * mLevelSeparation + globalPadding
            val right = left + width
            val bottom = top + height

            child.layout(left, top, right, bottom)
            node.x = left
            node.y = top

            maxRight = Math.max(maxRight, right)
            maxLeft = Math.min(maxLeft, left)
            maxBottom = Math.max(maxBottom, bottom)
            maxTop = Math.min(maxTop, top)
        }

        mBoundaries.set(maxLeft - (width - Math.abs(maxLeft)) - Math.abs(maxLeft), -height, maxRight, maxBottom)
    }

    /**
     * Returns the index of the child that contains the coordinates given.
     *
     * @param x X-coordinate
     * @param y Y-coordinate
     * @return The index of the child that contains the coordinates. If no child
     * is found then it returns INVALID_INDEX
     */
    private fun getContainingChildIndex(x: Int, y: Int): Int {
        if (mRect == null) {
            mRect = Rect()
        }
        for (index in 0 until childCount) {
            getChildAt(index).getHitRect(mRect)
            if (mRect!!.contains(x, y)) {
                return index
            }
        }
        return INVALID_INDEX
    }

    private fun clickChildAt(x: Int, y: Int) {
        val index = getContainingChildIndex(x, y)
        // no child found at this position
        if (index == INVALID_INDEX) {
            return
        }

        val itemView = getChildAt(index)
        val id = mAdapterV.getItemId(index)
        performItemClick(itemView, index, id)
    }

    private fun longClickChildAt(x: Int, y: Int) {

        val index = getContainingChildIndex(x, y)
        // no child found at this position
        if (index == INVALID_INDEX) {
            return
        }

        val itemView = getChildAt(index)
        val id = mAdapterV.getItemId(index)
        val listener = onItemLongClickListener
        listener?.onItemLongClick(this, itemView, index, id)
    }

    var squareLine: Boolean = true
    private fun drawLines(canvas: Canvas, node: AmNode) {
        if (node.hasChildren) {
            for (child in node.children) {
                drawLines(canvas, child as AmNode)
            }
        }

        val isIndependent = node.isIndependent

        if (node.hasParent) {
            mLinePath.reset()

            val parent = node.parent as AmNode
            val xMid = node.x.toFloat() + node.width / 2
            val yMid = node.y.toFloat() - mLevelSeparation / 2
            val yTick = node.y.toFloat() - mLevelSeparation / 4
            val xMidParent = parent.x.toFloat() + parent.width / 2
            val yParent = parent.y.toFloat() + parent.height
            val ticSize = 20f

            if (squareLine) {

                mLinePath.moveTo(xMid, node.y.toFloat())
                mLinePath.lineTo(xMid, yMid)
                mLinePath.lineTo(xMidParent, yMid)
                if (isIndependent) {
                    mLinePath.moveTo(xMid - ticSize, yTick - ticSize / 4)
                    mLinePath.lineTo(xMid + ticSize, yTick - ticSize / 4)
                    mLinePath.moveTo(xMid - ticSize, yTick + ticSize / 4)
                    mLinePath.lineTo(xMid + ticSize, yTick + ticSize / 4)
                }

                canvas.drawPath(mLinePath, mLinePaint)
                mLinePath.reset()

                mLinePath.moveTo(xMidParent, yMid)
                mLinePath.lineTo(xMidParent,
                        yParent)

            } else {
                mLinePath.moveTo(xMid, node.y.toFloat())
                mLinePath.lineTo(xMidParent, yParent)
            }
            canvas.drawPath(mLinePath, mLinePaint)
        }
    }

    /**
     * Whether to use the max available size for each node, so all nodes have the same size. A
     * change to this value invokes a re-drawing of the tree.
     *
     * @param useMaxSize `true` if using same size for each node, `false` otherwise.
     */
    fun setUseMaxSize(useMaxSize: Boolean) {
        isUsingMaxSize = useMaxSize
        invalidate()
        requestLayout()
    }

    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        clickChildAt(e.x.toInt() + scrollX, e.y.toInt() + scrollY)
        return true
    }

    override fun onScroll(downEvent: MotionEvent, event: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        val newScrollX = scrollX + distanceX
        val newScrollY = scrollY + distanceY

        if (mBoundaries.contains(newScrollX.toInt(), newScrollY.toInt())) {
            scrollBy(distanceX.toInt(), distanceY.toInt())
        }
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        longClickChildAt(event.x.toInt() + scrollX, event.y.toInt() + scrollY)
    }

    override fun onFling(event1: MotionEvent, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return true
    }

    override fun getAdapter(): WideTreeAdapter<*>? {
        return mAdapterV
    }

    override fun setAdapter(adapterV: WideTreeAdapter<*>) {

        try {
            mAdapterV.unregisterDataSetObserver(mDataSetObserver)
        } catch (e: UninitializedPropertyAccessException) {
        } finally {
            mAdapterV = adapterV
            mDataSetObserver = TreeDataSetObserver()
            mAdapterV.registerDataSetObserver(mDataSetObserver)

            requestLayout()
        }
//        if (mAdapterV != null && mDataSetObserver != null) {
//            mAdapterV.unregisterDataSetObserver(mDataSetObserver)
//        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int,
                          bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (mAdapterV == null) {
            return
        }

        positionItems()

        invalidate()
    }

    override fun getSelectedView(): View? {
        return null
    }

    override fun setSelection(position: Int) {}

    override fun dispatchDraw(canvas: Canvas) {
        if (mAdapterV == null) return

        super.dispatchDraw(canvas)

        val rootNode = mAdapterV.getNode(0)
        if (rootNode != null) {
            drawLines(canvas, rootNode!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector!!.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (mAdapterV == null) {
            return
        }

        var maxWidth = 0
        var maxHeight = 0
        var minHeight = Integer.MAX_VALUE

        for (i in 0 until mAdapterV.count) {
            val child = mAdapterV.getView(i, null, this)

            var params: ViewGroup.LayoutParams? = child.layoutParams
            if (params == null) {
                params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            addViewInLayout(child, -1, params, true)


            val childWidthSpec = if (params.width > 0) {
                View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY)
            } else {
                View.MeasureSpec.UNSPECIFIED
            }

            val childHeightSpec = if (params.height > 0) {
                View.MeasureSpec.makeMeasureSpec(params.height, View.MeasureSpec.EXACTLY)
            } else {
                View.MeasureSpec.UNSPECIFIED
            }

            child.measure(childWidthSpec, childHeightSpec)
            val node = mAdapterV.getNode(i)
            val measuredWidth = child.measuredWidth
            val measuredHeight = child.measuredHeight
            node!!.setSize(measuredWidth, measuredHeight)

            maxWidth = Math.max(maxWidth, measuredWidth)
            maxHeight = Math.max(maxHeight, measuredHeight)
            minHeight = Math.min(minHeight, measuredHeight)
        }

        mMaxChildWidth = maxWidth
        mMaxChildHeight = maxHeight
        mMinChildHeight = minHeight

        if (isUsingMaxSize) {
            removeAllViewsInLayout()
            for (i in 0 until mAdapterV.count) {
                val child = mAdapterV.getView(i, null, this)

                var params: ViewGroup.LayoutParams? = child.layoutParams
                if (params == null) {
                    params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                addViewInLayout(child, -1, params, true)

                val widthSpec = View.MeasureSpec.makeMeasureSpec(mMaxChildWidth, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(mMaxChildHeight, View.MeasureSpec.EXACTLY)
                child.measure(widthSpec, heightSpec)

                val node = mAdapterV.getNode(i)
                node!!.setSize(child.measuredWidth, child.measuredHeight)
            }
        }

        mAdapterV.notifySizeChanged()
    }

    private inner class TreeDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()

            refresh()
        }

        override fun onInvalidated() {
            super.onInvalidated()

            refresh()
        }

        private fun refresh() {
            invalidate()
            requestLayout()
        }
    }

    companion object {

        private const val INVALID_INDEX = -1

        const val DEFAULT_USE_MAX_SIZE = false
        private const val DEFAULT_LINE_LENGTH = 120      // Vertical Gap
        private const val DEFAULT_LINE_THICKNESS = 5
        private const val DEFAULT_LINE_COLOR = Color.BLACK

//        const val DEFAULT_USE_MAX_SIZE = false
//        private const val DEFAULT_LINE_LENGTH = 100
//        private const val DEFAULT_LINE_THICKNESS = 5
//        private const val DEFAULT_LINE_COLOR = Color.BLACK
//        private const val INVALID_INDEX = -1
    }
}
