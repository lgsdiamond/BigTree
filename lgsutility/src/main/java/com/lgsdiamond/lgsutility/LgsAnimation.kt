package com.lgsdiamond.lgsutility

import android.view.View
import android.view.animation.*

class LgsAnimationUtil {
    companion object {
        fun animateCenterScale(view: View) {
            val set = AnimationSet(true)
            val scale = ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.5f, Animation.RELATIVE_TO_PARENT, 0.5f)
            val alpha = AlphaAnimation(0.0f, 1.0f)
            set.addAnimation(scale)
            set.addAnimation(alpha)
            set.duration = 700L
            set.interpolator = OvershootInterpolator()
            view.startAnimation(set)
        }
    }
}