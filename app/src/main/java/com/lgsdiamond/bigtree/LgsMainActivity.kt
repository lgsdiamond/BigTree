package com.lgsdiamond.bigtree

import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.lgsdiamond.lgsutility.LgsUtility
import com.lgsdiamond.lgsutility.toContentFace
import com.lgsdiamond.lgsutility.toTitleFace
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

val gAppContext: Context by lazy { LgsMainActivity.lgsMainActivity.applicationContext }
val gMainActivity: MainActivity
    get() = (LgsMainActivity.lgsMainActivity as MainActivity)

abstract class LgsMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        
        // LGS: global properties and initiate LgsUtility
        lgsMainActivity = this@LgsMainActivity
        LgsUtility.initiate(gMainActivity, gAppContext, titleFace, contentFace)

        super.onCreate(savedInstanceState)

        // LGS: fix orientation to vertical
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.menu.toTitleFace()

        // LGS: addition
        initMainUI()
        initFragments()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            val count = fragmentManager.backStackEntryCount
            if (count > 0) {
                fragmentManager.popBackStack()
            } else {
                finishApp(true)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        menu.toTitleFace()
        assignIconOptionsMenu(menu)

        optionsMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return if (handleOptionsItem(item.itemId)) true else super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        handleNavigationItem(item.itemId)
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    // LGS: addition - abstract functions

    abstract fun handleNavigationItem(id: Int): Boolean
    abstract fun handleOptionsItem(id: Int): Boolean
    abstract fun assignIconOptionsMenu(menu: Menu)

    abstract fun initMainUI()
    abstract fun initFragments()

    // LGS: addition - exit app
    protected fun finishApp(toAsk: Boolean) {
        if (toAsk) {
            val builder = AlertDialog.Builder(this)
                    .setTitle("BigTree 앱 나가기".toTitleFace())
                    .setMessage("BigTree 앱을 마치겠습니까?".toContentFace())
                    .setPositiveButton("Yes".toTitleFace()) { _, _ ->
                        moveTaskToBack(true)
                        finish()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                    .setNegativeButton("No".toTitleFace(), null)

            val dialog = builder.create()
            dialog.show()
        } else {
            moveTaskToBack(true)
            finish()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    companion object {
        lateinit var lgsMainActivity: LgsMainActivity
        lateinit var optionsMenu: Menu

        // for typefaces
        val titleFace: Typeface by lazy {
            Typeface.createFromAsset(gMainActivity.assets,
                    gMainActivity.getString(R.string.title_face_font_path))
        }
        val contentFace: Typeface by lazy {
            Typeface.createFromAsset(gMainActivity.assets,
                    gMainActivity.getString(R.string.content_face_font_path))
        }
    }
}