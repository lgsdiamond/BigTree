package com.lgsdiamond.lgsutility

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Typeface
import android.view.View
import android.view.inputmethod.InputMethodManager

lateinit var gMainActivity: Activity
lateinit var gAppContext: Context

class LgsUtility {
    companion object {
        fun initiate(mainActivity: Activity, appContext: Context,
                     titleFace: Typeface, contentFace: Typeface) {
            gMainActivity = mainActivity
            gAppContext = appContext
            LgsFontUtil.initiateFaces(titleFace, contentFace)
        }

        fun showSoftKeyboard(toShow: Boolean) {
            val imm = gMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            var view = gMainActivity.currentFocus
            if (view == null) {
                view = View(gAppContext)
            }

            if (toShow) {
                view.isFocusableInTouchMode = true
                view.requestFocus()
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            } else {

                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        fun installedPackage(packageID: String): Boolean {
            var isExist = false

            val pkgMgr = gMainActivity.packageManager
            val mApps: List<ResolveInfo>
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            mApps = pkgMgr.queryIntentActivities(mainIntent, 0)

            try {
                for (i in mApps.indices) {
                    if (mApps[i].activityInfo.packageName.startsWith(packageID)) {
                        isExist = true
                        break
                    }
                }
            } catch (e: Exception) {
                isExist = false
            }
            return isExist
        }

        fun openAndroidApp(packageID: String, packageName: String) {
            if (installedPackage(packageID)) {
                val intent = gMainActivity.packageManager.getLaunchIntentForPackage(packageID)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                gMainActivity.startActivity(intent)
            } else {
                ("\"" + packageName + "앱이 설치되지 않았습니다.").toToastTitle()
            }
        }
    }
}
