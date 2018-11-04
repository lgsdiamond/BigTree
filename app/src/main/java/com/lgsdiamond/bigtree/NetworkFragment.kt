package com.lgsdiamond.bigtree

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.*
import android.widget.ImageView
import com.lgsdiamond.bigtree.LgsMainActivity.Companion.optionsMenu
import com.lgsdiamond.bigtree.NetworkFragment.OnFragmentInteractionListener
import com.lgsdiamond.bigtree.amway.*
import com.lgsdiamond.bigtree.tree.*
import com.lgsdiamond.lgsutility.*
import kotlinx.android.synthetic.main.enter_text.*
import kotlinx.android.synthetic.main.focused_abo.*
import kotlinx.android.synthetic.main.fragment_network.*
import kotlinx.android.synthetic.main.monthly_develop.*
import kotlinx.android.synthetic.main.wide_tree_view.*
import kotlinx.coroutines.experimental.async
import java.util.*


/**
 * A simple [BigTreeFragment] subclass.
 * Activities that contain this fragment must implement the
 * [OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [NetworkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
open class NetworkFragment : BigTreeFragment() {
    override val barTitle = "네트워크 연습"
    open val dbTableName = "NETWORK"
    open var currentMonth: AmMonth? = null

    private lateinit var tvAdapter: TreeAdapter
    private lateinit var memberDBHelper: MemberDBHelper

    private var memberNodes: MutableList<AmNode> = ArrayList()

    private var mParam1: String? = null
    private var mParam2: String? = null
    private var mListener: OnFragmentInteractionListener? = null

    private var focusedABO: ABO? = null

    override fun getTitleTag(): String {
        return barTitle
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.options_menu_network, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        menu?.toTitleFace()
        val item = menu?.findItem(R.id.toggle_change_track)
        item?.isChecked = trackChange
    }

    open fun saveNetworkData() {
        async {
            memberDBHelper.writeNetworkEntries(memberNodes)
        }
    }

    open fun settingNetwork() {

    }

    override fun handleOptionsItem(id: Int): Boolean {
        when (id) {
            R.id.setting_network -> {
                settingNetwork()
            }
            R.id.save_data_network -> {
                saveNetworkData()
            }
            R.id.back_default_network -> {
                memberNodes = makeDefaultNetwork()
            }
            R.id.expand_all_view -> {
                for (node in memberNodes) {
                    node.expandAll()
                }
                MainActivity.playSound(LgsSoundUtil.soundTick)
            }
            R.id.collapse_all_view -> {
                for (node in memberNodes) {
                    node.collapseAll()
                }
                MainActivity.playSound(LgsSoundUtil.soundTick)
            }
            R.id.no_member_view -> {
                memberNodes.clear()
                NetworkFragment.activeFrag?.changeFocused(null)
            }
            R.id.add_ABO_20_view -> {
                memberNodes.add(AmNode(AmMember.newABO))
            }
            R.id.add_ABO_1000_view -> {
                val abo1000 = AmMember.newABO
                memberNodes.add(AmNode(abo1000))
                abo1000.pv.resetPersonal(1000f)
            }
            R.id.no_focus_view -> {
                NetworkFragment.activeFrag?.changeFocused(null)
                MainActivity.playSound(LgsSoundUtil.soundTick)
            }
            R.id.toggle_change_track -> {
                NetworkFragment.activeFrag?.toggleChangeTrack()
            }
        }

        refresh()
        return true    // consume the click
    }

    override fun setActionBarTitle() {
        val actionBar = (activity as MainActivity).supportActionBar
        actionBar!!.title = barTitle
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param mParam1 Parameter 1.
     * @param mParam2 Parameter 2.
     *zxC @return A new instance of fragment ReportFragment.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_network, container, false)
    }

    fun changeMemberName(member: AmMember) {
        showTextEnter(false, member.name)

        btnAcceptText.setOnClickListener {
            hideTextEnter()
            val newName = edtText.text.toString().trim()
            member.name = newName
            refresh(member.node)
        }
    }

    fun changePVFromInput(member: AmMember) {
        showTextEnter(true, member.pv.personal.toString())

        btnAcceptText.setOnClickListener {
            hideTextEnter()

            try {
                val newPV = java.lang.Float.valueOf(edtText.text.toString().trim()).toFloat()
                if (newPV < 0f) {
                    "개인 PV가 0.0 보다 작을 수는 없습니다.".toToastTitle()
                } else {
                    val change = newPV - member.pv.personal
                    if (change >= 0)
                        member.pv.addPersonal(change)
                    else
                        member.pv.subtractPersonal(-change)

                    refresh(member.node)
                }
            } catch (nfe: NumberFormatException) {
                "텍스트가 숫자 형식이 아닙니다.".toToastTitle()
            }
        }
    }

    private fun showTextEnter(isDecimal: Boolean, inText: String) {
        lo_enter_text.visibility = View.VISIBLE
        edtText.inputType = if (isDecimal) (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL) else InputType.TYPE_CLASS_TEXT
        edtText.setText(inText)

        LgsUtility.showSoftKeyboard(true)
        edtText.setSelectAllOnFocus(true)
        edtText.requestFocus()
        edtText.selectAll()
    }

    private fun hideTextEnter() {
        LgsUtility.showSoftKeyboard(false)
        lo_enter_text.visibility = View.GONE
    }

    override fun initFragmentUI(view: View) {

        // for db
        memberDBHelper = MemberDBHelper(dbTableName)
        memberDBHelper.open()

        makeInitialNetwork()

        // for UI
        hideTextEnter()
        btnCancelText.setOnClickListener {
            hideTextEnter()
        }

        btn_prev_month.setOnClickListener {
            val curMonth = currentMonth
            if (curMonth != null) {
                currentMonth = curMonth - 1
                updateFocused()
            }
        }

        btn_next_month.setOnClickListener {
            val curMonth = currentMonth
            if (curMonth != null) {
                currentMonth = curMonth + 1
                updateFocused()
            }
        }

        rvMemberList.layoutManager = LinearLayoutManager(gAppContext)

        val viewBinders = Arrays.asList(ABONodeBinder(), OnMemberNodeBinder(), OffMemberNodeBinder())
        tvAdapter = TreeAdapter(memberNodes, viewBinders)

        // whether collapse child nodes when their parent node was close.
        //        tvAdapter.ifCollapseChildWhileCollapseParent(true);
        tvAdapter.setOnTreeNodeListener(object : TreeAdapter.OnTreeNodeListener {

            override fun onClick(node: TreeNode<*>, holder: RecyclerView.ViewHolder): Boolean {
                if (!node.isLeaf) {
                    //Update and toggle the node.
                    onToggle(!node.isExpand, holder)
                    //                    if (!node.isExpand())
                    //                        tvAdapter.collapseBrotherNode(node);
                }
                return false
            }

            override fun onToggle(isExpand: Boolean, holder: RecyclerView.ViewHolder) {
                val aboViewHolder = holder as ABONodeBinder.ViewHolder
                val ivArrow = aboViewHolder.ivArrow
                val rotateDegree = if (isExpand) 90 else -90
                ivArrow.animate().rotationBy(rotateDegree.toFloat())
                        .start()
            }
        })
        rvMemberList.adapter = tvAdapter

        changeFocused(
                when {(focusedABO != null) -> focusedABO
                    (memberNodes.size > 0) -> (memberNodes.last().content as ABO)
                    else -> null
                }
        )

        lo_focused_abo.setOnLongClickListener {
            toggleWideTreeView()
            true
        }

        // for wide tree view
        tvMain.setAdapter(wideAdapter)
        showHideWideTreeView(false)
    }

    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    fun refresh(amNode: AmNode? = null) {
        tvAdapter.refresh(memberNodes)

        if ((amNode != null) && (focusedABO != null)) {
            if (amNode.containsSponsorNode(focusedABO!!.node)) updateFocused()
        }
    }

    fun removeMemberNode(node: AmNode) {
        if ((focusedABO != null) && (focusedABO!!.node.containsSponsorNode(node)))
            changeFocused(null)

        val parent = node.parent
        if (parent == null) {
            memberNodes.remove(node)
        } else {
            (parent as AmNode).removeMemberNode(node)
        }

        refresh()
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    open fun makeDefaultNetwork(): MutableList<AmNode> {
        val aMemberNodes: MutableList<AmNode> = ArrayList()

        // Basic Model
        val single = AmNode(ABO("Single"))

        val basic = AmNode(ABO("Basic"))
        basic.addMemberNode(
                AmNode(ABO("You"))
                        .addMemberNode(AmNode(ABO("A")))
                        .addMemberNode(AmNode(ABO("B")))
        )

        aMemberNodes.add(single)
        single.resetBase()

        aMemberNodes.add(basic)
        basic.resetBase()

        val model642One = AmNode(ABO("You-6"))
        model642One.addNetwork6()
        aMemberNodes.add(model642One)
        model642One.resetBase()

        val model642Two = AmNode(ABO("You-64"))
        model642Two.addNetwork64()
        aMemberNodes.add(model642Two)
        model642Two.resetBase()

        val model642Three = AmNode(ABO("You-642"))
        model642Three.addNetwork642()
        aMemberNodes.add(model642Three)
        model642Three.resetBase()

        return aMemberNodes
    }

    var networkInitialized: Boolean = false
    fun makeInitialNetwork() {
        if (networkInitialized) return

        val savedMemberNodes = memberDBHelper.readNetworkEntries()

        memberNodes = savedMemberNodes?.let { savedMemberNodes } ?: makeDefaultNetwork()

        networkInitialized = true
    }

    private fun updateFocused() {
        if (focusedABO == null) return

        val abo = focusedABO!!

        lo_focused_abo.visibility = View.VISIBLE
        tv_focused_abo_overview.text = "[${abo.name}]-${abo.pinTitle}: 총보너스=${abo.bonus.total.to1000Won()}"

        value_pv.text = abo.pv.toString().toSpanTitleValue()
        value_bonus.text = abo.bonus.toString().toSpanTitleValue()

        if (currentMonth == null) {
            lo_monthly_record.visibility = View.GONE
        } else {
            lo_monthly_record.visibility = View.VISIBLE
            tv_current_month.text = currentMonth.toString()  // TODO: Testing
        }

        wideAdapter.setRootNode(abo.node)
    }

    fun getFocused(): ABO? = focusedABO
    fun changeFocused(new: ABO?) {
        if (new == null) {
            lo_focused_abo.visibility = View.GONE
            focusedABO = null

            lo_monthly_record.visibility = View.GONE
            return
        }

        focusedABO = new

        updateFocused()
    }

    var trackChange: Boolean = false
    fun toggleChangeTrack() {
        trackChange = !trackChange
        if (trackChange) {
            memberNodes.forEach { it.resetBase() }
        }
        adjustOptionSoundMenuItem()
    }

    private fun adjustOptionSoundMenuItem() {
        val item = optionsMenu.findItem(R.id.toggle_change_track)
        item.isChecked = trackChange
    }

    // for Recording
    fun finalizeMonthRecord() {
        memberNodes.forEach { if (it.content is ABO) (it.content as ABO).finalizeMonth(currentMonth) }
    }

    companion object {
        var activeFrag: NetworkFragment? = null
    }

    // extensions
    private fun CharSequence.toSpanTitleValue(): CharSequence {
        val spannableString = SpannableString(this)

        var startIndex = this.indexOf('[', 0)
        var endIndex = this.indexOf(']', startIndex + 1)
        while ((startIndex != -1) && (endIndex != -1)) {
            spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(gAppContext, R.color.colorDataLabel)),
                    startIndex, endIndex + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(RelativeSizeSpan(0.8f),
                    startIndex, endIndex + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            startIndex = endIndex + 1

            startIndex = this.indexOf('[', startIndex)
            endIndex = this.indexOf(']', startIndex + 1)
        }

        return spannableString
    }

    private inner class WideTreeViewHolder(view: View) {
        internal var mCard: CardView = view.findViewById(R.id.wide_node_card_view)
        internal var mIcon: ImageView = view.findViewById(R.id.wide_node_icon)
        internal var mTxtPercent: LgsTextContent = view.findViewById(R.id.wide_node_percent)
        internal var mTxtPV: LgsTextContent = view.findViewById(R.id.wide_node_pv)
        internal var mTxtGroupPV: LgsTextContent = view.findViewById(R.id.wide_group_pv)
        internal var mTxtBonus: LgsTextContent = view.findViewById(R.id.wide_node_bonus)
    }

//    private inner class WideTreeViewHolder(view: View) {
//        internal var mTextView: TextView = view.findViewById(R.id.wide_node_text)
//    }

    private var showWideTreeView = false
    private val wideAdapter = object : BaseWideTreeAdapter<WideTreeViewHolder>(gAppContext, R.layout.node_icon) {
        override fun onCreateViewHolder(view: View): WideTreeViewHolder {
            return WideTreeViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: WideTreeViewHolder, member: AmMember, position: Int) {
            viewHolder.mCard.setOnLongClickListener {
                handlePopupOnMember(it, member)
                true
            }

            val iconID = when {
                (member is ABO) -> {
                    if (member.isIndependent) R.drawable.ic_abo_independent else R.drawable.ic_abo
                }
                (member is OnMember) -> R.drawable.ic_onmember
                else -> R.drawable.ic_offmember
            }
            viewHolder.mIcon.setImageResource(iconID)
            if (member is ABO) {
                viewHolder.mTxtPercent.visibility = View.VISIBLE
                viewHolder.mTxtPercent.text = "${(member.firstBonusRate * 100f).toInt()}%"
            } else {
                viewHolder.mTxtPercent.visibility = View.GONE
            }
            viewHolder.mTxtPV.text = member.pv.personal.to1000Won()
            viewHolder.mTxtBonus.text = if (member is ABO) member.bonus.total.to1000Won() else ""

            if (member.node.isLeaf || member !is ABO) {
                viewHolder.mTxtGroupPV.visibility = View.GONE
            } else {
                viewHolder.mTxtGroupPV.visibility = View.VISIBLE
                viewHolder.mTxtGroupPV.text = member.pv.personalGroup.to1000Won()
            }
        }

//        override fun onBindViewHolder(viewHolder: WideTreeViewHolder, data: Any, position: Int) {
//            viewHolder.mTextView.text = data.toString()
//        }
    }

//    private val wideAdapter = object : BaseWideTreeAdapter<WideTreeViewHolder>(gAppContext, R.layout.node) {
//        override fun onCreateViewHolder(view: View): WideTreeViewHolder {
//            return WideTreeViewHolder(view)
//        }
//
//        override fun onBindViewHolder(viewHolder: WideTreeViewHolder, data: Any, position: Int) {
//            viewHolder.mTextView.text = data.toString()
//        }
//    }

    private fun toggleWideTreeView() {
        showWideTreeView = !showWideTreeView
        showHideWideTreeView(showWideTreeView)
    }

    private fun showHideWideTreeView(toShow: Boolean) {
        if (toShow) {
            lo_wide_tree_view.visibility = View.VISIBLE
            rvMemberList.visibility = View.GONE

            focusedABO?.let {
                wideAdapter.setRootNode(focusedABO!!.node)
            }
        } else {
            lo_wide_tree_view.visibility = View.GONE
            rvMemberList.visibility = View.VISIBLE
        }

        showWideTreeView = toShow
    }
}

open class ExerciseFragment : NetworkFragment() {
    override val barTitle = "네트워크 연습"
    override val dbTableName = "NETWORK"
    override var currentMonth: AmMonth? = null
}

