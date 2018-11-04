package com.lgsdiamond.bigtree.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.lgsdiamond.bigtree.*
import com.lgsdiamond.bigtree.amway.*
import com.lgsdiamond.lgsutility.LgsSoundUtil
import com.lgsdiamond.lgsutility.LgsTextContent
import com.lgsdiamond.lgsutility.LgsTextTitle
import com.lgsdiamond.lgsutility.toTitleFace

class ABONodeBinder : TreeBinder<ABONodeBinder.ViewHolder>() {

    override val layoutId = R.layout.item_abo

    private fun makePopupABO(anchor: View): PopupMenu {

        val popup = PopupMenu(gAppContext, anchor)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.long_click_abo, popup.menu)
        popup.menu.toTitleFace()
        return popup
    }

    override fun provideViewHolder(itemView: View): ViewHolder {
        return ViewHolder(itemView)
    }

    override fun bindView(holder: ViewHolder, position: Int, node: TreeNode<*>) {
        require(node is AmNode)

        val amNode = node as AmNode
        val abo = amNode.content as ABO
        val level = amNode.measureLevel()

        when {
            (abo == NetworkFragment.activeFrag?.getFocused()) -> {
                holder.ivABOPin.background = ContextCompat.getDrawable(gAppContext, R.drawable.border_line_solid)
            }
            else -> {
                holder.ivABOPin.background = null
            }
        }

        when {
            (abo.isTopSponsor) -> {
                holder.tvOverview.visibility = View.VISIBLE
                holder.tvOverview.text = abo.name
                val backColor = if (NetworkFragment.activeFrag is PlanningFragment) Color.rgb(0xFF, 0xD0, 0xD0)
                else if (NetworkFragment.activeFrag is SimulationFragment) Color.rgb(0xD0, 0xFF, 0xD0)
                else Color.rgb(0xD0, 0xD0, 0xFF)
                holder.itemView.setBackgroundColor(backColor)
            }
            else -> {
                holder.tvOverview.visibility = View.GONE
                setBackgroundByLevel(holder.itemView, level)
            }
        }

        holder.ivArrow.rotation = 0f
        holder.ivArrow.setImageResource(R.drawable.ic_amway_arrow_right)
        val rotateDegree = if (node.isExpand) 90 else 0
        holder.ivArrow.rotation = rotateDegree.toFloat()

        val bonusRate = abo.firstBonusRate
        holder.tvPINTitle.text = "${(bonusRate * 100f).toInt()}%"
        holder.ivABOPin.setImageResource(if (bonusRate >= AmSystem.FIRST_BONUS_MAX_RATE)
            R.drawable.ic_abo_independent else R.drawable.ic_abo)

        holder.tvTitle.text = abo.toTitle()
        holder.tvDesc.text = abo.toDesc()

        holder.tvBonusChange.showChange(abo.bonus.totalChange)
        holder.tvPVChange.showChange(abo.pv.groupChange)

        if (node.isLeaf)
            holder.ivArrow.visibility = View.INVISIBLE
        else
            holder.ivArrow.visibility = View.VISIBLE

        holder.itemView.setOnLongClickListener {
            val popup = makePopupABO(holder.itemView)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.expand_all -> {
                        amNode.expandAll()
                        MainActivity.playSound(LgsSoundUtil.soundClick)
                    }
                    R.id.collapse_all -> {
                        amNode.collapseAll()
                        MainActivity.playSound(LgsSoundUtil.soundClick)
                    }
                    R.id.reset_base -> {
                        amNode.resetBase()
                    }
                    R.id.remove_ABO -> {
                        NetworkFragment.activeFrag?.removeMemberNode(amNode)
                    }
                    R.id.add_ABO_20 -> {
                        amNode.expand()
                        amNode.addMemberNode(AmNode(AmMember.newABO))
                    }
                    R.id.add_ABO_1000 -> {
                        amNode.expand()
                        val abo1000 = AmMember.newABO
                        amNode.addMemberNode(AmNode(abo1000))
                        abo1000.pv.resetPersonal(1000f)
                    }
                    R.id.add_node3 -> {
                        amNode.expand()
                        amNode.addNetwork3()
                    }
                    R.id.add_node6 -> {
                        amNode.expand()
                        amNode.addNetwork6()
                    }
                    R.id.add_node64 -> {
                        amNode.expand()
                        amNode.addNetwork64()
                    }
                    R.id.add_node642 -> {
                        amNode.expand()
                        amNode.addNetwork642()
                    }
                    R.id.add_onMember -> {
                        amNode.expand()
                        amNode.addMemberNode(AmNode(AmMember.newOnMember))
                    }
                    R.id.add_offMember -> {
                        amNode.expand()
                        amNode.addMemberNode(AmNode(AmMember.newOffMember))
                    }
                    R.id.add_PV_10_ABO -> {
                        abo.pv.addPersonal(10.0f)
                    }
                    R.id.subtract_PV_10_ABO -> {
                        abo.pv.subtractPersonal(10.0f)
                    }
                    R.id.add_PV_100_ABO -> {
                        abo.pv.addPersonal(100.0f)
                    }
                    R.id.subtract_PV_100_ABO -> {
                        abo.pv.subtractPersonal(100.0f)
                    }
                    R.id.enter_PV_ABO -> {
                        NetworkFragment.activeFrag?.changePVFromInput(abo)
                    }
                    R.id.set_focused_abo -> {
                        NetworkFragment.activeFrag?.changeFocused(abo)
                        MainActivity.playSound(LgsSoundUtil.soundSliding)
                    }
                    R.id.chane_name_ABO -> {
                        NetworkFragment.activeFrag?.changeMemberName(abo)
                    }
                }
                NetworkFragment.activeFrag?.refresh(amNode)
                true    // consume the click
            }
            MainActivity.playSound(LgsSoundUtil.soundTick)
            popup.show()
            true
        }
    }

    class ViewHolder(rootView: View) : TreeBinder.ViewHolder(rootView) {
        val ivArrow: ImageView = findViewById(R.id.iv_arrow)
        val ivABOPin: ImageView = findViewById(R.id.iv_abo_pin)
        val tvTitle: LgsTextTitle = findViewById(R.id.tv_title_abo)
        val tvDesc: LgsTextContent = findViewById(R.id.tv_desc_abo)
        val tvBonusChange: LgsTextContent = findViewById(R.id.tv_bonus_change_abo)
        val tvPVChange: LgsTextContent = findViewById(R.id.tv_pv_change_abo)
        val tvPINTitle: LgsTextContent = findViewById(R.id.tv_pin_title)
        val tvOverview: LgsTextTitle = findViewById(R.id.tv_abo_overview)
    }

    companion object {
        fun setBackgroundByLevel(view: View, level: Int) {
            val blue = 255 - (level % 5) * 20
            val red = 255 - (level % 3) * 20
            view.setBackgroundColor(Color.argb(0xFF, red, 0xFF, blue))
        }
    }
}

fun handlePopupOnMember(anchor: View, member: AmMember) {
    val popup = PopupMenu(gAppContext, anchor)
    val inflater = popup.menuInflater
    inflater.inflate(R.menu.long_click_onmember, popup.menu)
    popup.menu.toTitleFace()
    val node = member.node
    popup.setOnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            R.id.remove_onMember -> {
                NetworkFragment.activeFrag?.removeMemberNode(node)
            }
            R.id.add_PV_10_onMember -> {
                member.pv.addPersonal(10.0f)
            }
            R.id.subtract_PV_10_onMember -> {
                member.pv.subtractPersonal(10.0f)
            }
            R.id.add_PV_100_onMember -> {
                member.pv.addPersonal(100.0f)
            }
            R.id.subtract_PV_100_onMember -> {
                member.pv.subtractPersonal(100.0f)
            }
            R.id.enter_PV_onMember -> {
                NetworkFragment.activeFrag?.changePVFromInput(member)
            }
            R.id.change_name_onMember -> {
                NetworkFragment.activeFrag?.changeMemberName(member)
            }
        }
        NetworkFragment.activeFrag?.refresh(node)
        true    // consume the click
    }
    popup.show()
}


class OnMemberNodeBinder : TreeBinder<OnMemberNodeBinder.ViewHolder>() {

    override val layoutId = R.layout.item_onmember

    override fun provideViewHolder(itemView: View): ViewHolder {
        return ViewHolder(itemView)
    }

    override fun bindView(holder: ViewHolder, position: Int, node: TreeNode<*>) {
        val amNode = node as AmNode
        val onMember = node.content as OnMember
        holder.itemView.setOnLongClickListener {
            handlePopupOnMember(holder.itemView, onMember)
            true
        }

        ABONodeBinder.setBackgroundByLevel(holder.itemView, amNode.measureLevel())

        val member = node.content as OnMember
        holder.tvDesc.text = member.toDesc()
        holder.tvPVChange.showChange(member.pv.groupChange)
    }

    inner class ViewHolder(rootView: View) : TreeBinder.ViewHolder(rootView) {
        val tvDesc: LgsTextTitle = findViewById(R.id.tv_desc_onMember)
        val tvPVChange: LgsTextContent = findViewById(R.id.tv_pv_change_onMember)
    }
}

class OffMemberNodeBinder : TreeBinder<OffMemberNodeBinder.ViewHolder>() {

    override val layoutId = R.layout.item_offmember

    private fun makePopupOffMember(anchor: View): PopupMenu {
        val popup = PopupMenu(gAppContext, anchor)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.long_click_offmember, popup.menu)
        popup.menu.toTitleFace()
        return popup
    }

    override fun provideViewHolder(itemView: View): ViewHolder {
        return ViewHolder(itemView)
    }

    override fun bindView(holder: ViewHolder, position: Int, node: TreeNode<*>) {
        val amNode = node as AmNode
        val offMember = node.content
        holder.itemView.setOnLongClickListener {
            val popup = makePopupOffMember(holder.itemView)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.remove_offMember -> {
                        NetworkFragment.activeFrag?.removeMemberNode(amNode)
                    }
                    R.id.add_PV_10_offMember -> {
                        offMember.pv.addPersonal(10.0f)
                    }
                    R.id.subtract_PV_10_offMember -> {
                        offMember.pv.subtractPersonal(10.0f)
                    }
                    R.id.add_PV_100_offMember -> {
                        offMember.pv.addPersonal(100.0f)
                    }
                    R.id.subtract_PV_100_offMember -> {
                        offMember.pv.subtractPersonal(100.0f)
                    }
                    R.id.enter_PV_offMember -> {
                        NetworkFragment.activeFrag?.changePVFromInput(offMember)
                    }
                    R.id.chane_name_offMember -> {
                        NetworkFragment.activeFrag?.changeMemberName(offMember)
                    }
                }
                NetworkFragment.activeFrag?.refresh(amNode)
                true    // consume the click
            }
            popup.show()
            true
        }

        ABONodeBinder.setBackgroundByLevel(holder.itemView, amNode.measureLevel())

        val member = node.content as OffMember
        holder.tvDesc.text = member.toDesc()
        holder.tvPVChange.showChange(member.pv.groupChange)
    }

    inner class ViewHolder(rootView: View) : TreeBinder.ViewHolder(rootView) {
        val tvDesc: LgsTextTitle = findViewById(R.id.tv_desc_offMember)
        val tvPVChange: LgsTextContent = findViewById(R.id.tv_pv_change_offMember)
    }
}

internal fun TextView.showChange(change: Float) {

    val activeFrag = NetworkFragment.activeFrag

    if (activeFrag != null) {
        if (!activeFrag.trackChange) {
            visibility = View.GONE
            return
        }

        if (change != 0f) {
            visibility = View.VISIBLE
            if (change > 0.0f) {
                text = "+${change.to1000Won()}"
                setTextColor(Color.BLUE)
            } else {
                text = change.to1000Won()
                setTextColor(Color.RED)
            }
        } else {
            visibility = View.GONE
        }
    }
}