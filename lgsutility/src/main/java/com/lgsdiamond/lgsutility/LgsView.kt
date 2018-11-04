package com.lgsdiamond.lgsutility

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.support.v7.widget.*
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.lgsdiamond.lgsutility.LgsFontUtil.Companion.titleFace

// LGS: addition - extensions
fun CharSequence.toTitleFace() = spanFace(LgsFontUtil.titleFace)

fun CharSequence.toContentFace() = spanFace(LgsFontUtil.contentFace)

fun String.toToastTitle() {
    gMainActivity.runOnUiThread {
        Toast.makeText(gAppContext, this.toTitleFace(), Toast.LENGTH_SHORT).show()
    }
}

fun String.removeWhitespaces(): String {
    return this.replace("\\s+".toRegex(), "")
}

fun CharSequence.spanFace(face: Typeface): CharSequence {
    val span = SpannableString(this)
    span.setSpan(CustomTypefaceSpan("", face), 0, this.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return span
}

fun Menu.toTitleFace() {

    fun applyFontToMenuItem(mi: MenuItem, face: Typeface) {
        val mNewTitle = SpannableString(mi.title)
        mNewTitle.setSpan(CustomTypefaceSpan("", face), 0, mNewTitle.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        mi.title = mNewTitle
    }

    for (i in 0..(size() - 1)) {
        val menuItem = getItem(i)

        val subMenu = menuItem.subMenu
        if ((subMenu != null) && subMenu.size() > 0) {
            subMenu.toTitleFace()
        }
        applyFontToMenuItem(menuItem, LgsFontUtil.titleFace)
    }
}

fun Menu.setIconInMenu(menuItemId: Int, labelId: Int, iconId: Int) {
    val item = this.findItem(menuItemId)
    val title = gMainActivity.resources.getString(labelId)

    val builder = SpannableStringBuilder("    $title")
    builder.setSpan(ImageSpan(gAppContext, iconId), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    builder.setSpan(titleFace, 2, 2 + title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    item.title = builder
}

class LgsTextTitle : AppCompatTextView {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = LgsFontUtil.titleFace
    }
}

class LgsTextContent : AppCompatTextView {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = LgsFontUtil.contentFace
    }
}

class LgsEditText : AppCompatEditText {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = LgsFontUtil.contentFace
    }
}

class LgsButton : AppCompatButton {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setTextColor(if (enabled) Color.BLACK else Color.DKGRAY)
    }

    init {
        typeface = LgsFontUtil.titleFace
    }

    fun backToNormal() {
        setTextColor(Color.BLACK)
    }

    fun autoDelayedClick() {
        val delay = 1000L
        postDelayed({ performClick() }, delay)
    }
}

class LgsImageButton : AppCompatImageButton {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

class LgsCardView : AppCompatImageView {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

class LgsCheckBox : AppCompatCheckBox {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = LgsFontUtil.titleFace
    }
}

class LgsSwitch : Switch {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = LgsFontUtil.titleFace
    }
}

class LgsSpinner : AppCompatSpinner {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
    }
}

class LgsArrayAdapter<T>(context: Context, textViewResourceId: Int, data: Array<T>)
    : ArrayAdapter<T>(context, textViewResourceId, data) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        if (view is TextView) {
            view.typeface = LgsFontUtil.titleFace
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        if (view is TextView) {
            view.typeface = LgsFontUtil.titleFace
        }
        return view
    }
}