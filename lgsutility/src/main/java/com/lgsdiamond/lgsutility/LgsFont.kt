package com.lgsdiamond.lgsutility

import android.graphics.Paint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.TypefaceSpan
import android.view.Menu
import android.view.MenuItem

class LgsFontUtil {
    companion object {
        lateinit var titleFace: Typeface
        lateinit var contentFace: Typeface

        fun initiateFaces(title: Typeface, content: Typeface) {
            titleFace = title
            contentFace = content
        }

        fun customFaceMenu(menu: Menu, menuFace: Typeface) {

            fun applyFontToMenuItem(mi: MenuItem) {
                val mNewTitle = SpannableString(mi.title)
                mNewTitle.setSpan(CustomTypefaceSpan("", menuFace), 0, mNewTitle.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                mi.title = mNewTitle
            }

            for (i in 0..(menu.size() - 1)) {
                val menuItem = menu.getItem(i)

                val subMenu = menuItem.subMenu
                if ((subMenu != null) && subMenu.size() > 0) {
                    customFaceMenu(subMenu, menuFace)
                }
                applyFontToMenuItem(menuItem)
            }
        }
    }
}

class CustomTypefaceSpan(family: String, private val newFace: Typeface) : TypefaceSpan(family) {

    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, newFace)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, newFace)
    }

    private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
        val oldStyle: Int
        val old = paint.typeface

        oldStyle = old?.let { 0 } ?: old.style

        val fake = oldStyle and tf.style.inv()
        if (fake and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }

        if (fake and Typeface.ITALIC != 0) {
            paint.textSkewX = -0.25f
        }

        paint.typeface = tf
    }
}
