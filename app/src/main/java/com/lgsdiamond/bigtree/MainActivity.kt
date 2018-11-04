package com.lgsdiamond.bigtree

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.support.v4.view.GravityCompat
import android.view.Menu
import android.view.View
import android.widget.ImageView
import com.lgsdiamond.lgsutility.LgsSoundUtil
import com.lgsdiamond.lgsutility.LgsUtility
import com.lgsdiamond.lgsutility.setIconInMenu
import com.lgsdiamond.lgsutility.toToastTitle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

// for debugging
const val APP_LOG_TAG: String = "_BigTree"

class MainActivity : LgsMainActivity(),
        NetworkFragment.OnFragmentInteractionListener,
        ManualFragment.OnFragmentInteractionListener {

    private var currentFrag: BigTreeFragment? = null

    private val exerciseFrag: ExerciseFragment by lazy { ExerciseFragment() }
    private val simulationFrag: SimulationFragment by lazy { SimulationFragment() }
    private val planningFrag: PlanningFragment by lazy { PlanningFragment() }
    private val manualFrag: ManualFragment by lazy { ManualFragment() }

    override fun handleNavigationItem(id: Int): Boolean {
        // Handle navigation view item clicks here.
        var frag: BigTreeFragment? = null
        when (id) {
            R.id.nav_network -> frag = exerciseFrag
            R.id.nav_simulation -> frag = simulationFrag
            R.id.nav_planning -> frag = planningFrag
            R.id.nav_manual -> frag = manualFrag
            R.id.nav_sABN -> LgsUtility.openAndroidApp(SABN_PACKAGE_ID, SABN_PACKAGE_NAME)
            R.id.nav_amway_home -> {
                val url = "http://amway.co.kr"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            R.id.nav_terminate -> finishApp(false)
        }
        if (frag != null && frag != currentFrag) {
            switchFragment(frag)
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun handleOptionsItem(id: Int): Boolean {
        when (id) {
            R.id.action_login -> {
                playSound(LgsSoundUtil.soundSliding)

                isAppRegistered = false
                LgsUtility.showSoftKeyboard(false)
                showCurrentFragment(false)
                showUIElement(true)
            }
            R.id.action_sound -> {
                gIsSoundOn = !gIsSoundOn
                writePrefSoundOn()
                adjustOptionSoundMenuItem()
            }
            R.id.action_settings -> {
            }
            R.id.action_finish -> {
                finishApp(false)
            }
            else -> return NetworkFragment.activeFrag?.handleOptionsItem(id) ?: true
        }
        return true
    }

    private fun adjustOptionSoundMenuItem() {
        val item = optionsMenu.findItem(R.id.action_sound)
        item.isChecked = gIsSoundOn
    }

    override fun assignIconOptionsMenu(menu: Menu) {
        menu.setIconInMenu(R.id.action_login, R.string.action_login, R.drawable.ic_login)
        menu.setIconInMenu(R.id.action_sound, R.string.action_sound, R.drawable.ic_sound)
        menu.setIconInMenu(R.id.action_settings, R.string.action_settings, R.drawable.ic_setting_main)
        menu.setIconInMenu(R.id.action_finish, R.string.action_finish, R.drawable.ic_finish)

        val item = menu.findItem(R.id.action_sound)
        item.isChecked = gIsSoundOn

    }

    override fun initMainUI() {
        // read app preferences
        readPreferences()
        mainPhoto.scaleType = ImageView.ScaleType.FIT_XY

        btnRegister.setOnClickListener { _ ->
            val userName = edtUserName.text.toString().trim()
            if (userName.isEmpty()) {
                setRegisteredUser("")
                "유효한 회원 정보가 아닙니다.".toToastTitle()
                playSound(LgsSoundUtil.soundSliding)
            } else {
                setRegisteredUser(userName)
                playSound(LgsSoundUtil.soundOpening)
                ("\"$userName\"(으)로 등록되었습니다.").toToastTitle()
                showUIElement(false)
                if (currentFrag == null) {
                    switchFragment(exerciseFrag)
                } else {
                    showCurrentFragment(true)
                }
            }
            LgsUtility.showSoftKeyboard(false)
        }

        initiateActivity()
    }

    private fun setActionbarTitle() {
        val actionBar = supportActionBar
        actionBar!!.title = resources.getString(R.string.title_login)
    }

    var registeredUserName: String = ""
    private var isAppRegistered: Boolean = false

    private fun initiateActivity() {
        if (registeredUserName.isEmpty()) {
            isAppRegistered = false
            setActionbarTitle()
            showUIElement(true)
            ("사용자가 등록되지 않았습니다.").toToastTitle()

            playSound(LgsSoundUtil.soundSliding)
        } else {
            isAppRegistered = true
            showUIElement(false)
            ("사용자는 \"$registeredUserName\"입니다.").toToastTitle()

            playSound(LgsSoundUtil.soundOpening)
            switchFragment(exerciseFrag)
        }
        setRegisteredUser(registeredUserName)
    }

    private fun showUIElement(toShow: Boolean) {
        val visualStatus = if (toShow) View.VISIBLE else View.INVISIBLE

        if (toShow) setActionbarTitle()

        txtUserInfo.visibility = visualStatus

        btnRegister.visibility = visualStatus
        btnRegister.typeface = titleFace

        edtUserName.visibility = visualStatus
        edtUserName.typeface = contentFace

        labUserName.visibility = visualStatus

        labUserName.clearFocus()
    }


    override fun initFragments() {
    }

    override fun onFragmentInteraction(uri: Uri) {
    }

    private fun switchFragment(frag: BigTreeFragment?) {

        if (frag == null || frag == currentFrag) return    // no need, it is same fragment

        val fm = supportFragmentManager

        // now transactions
        val trans = fm.beginTransaction()
        trans.addToBackStack("")

        trans.replace(R.id.layoutContentMain, frag, frag.getTitleTag())   // replace with Tag
                .show(frag)
                .commit()
        fm.executePendingTransactions()

        frag.setActionBarTitle()

        currentFrag = frag        // now it becomes current
        if (currentFrag is NetworkFragment)
            NetworkFragment.activeFrag = (currentFrag as NetworkFragment)
    }

    private fun showCurrentFragment(toShow: Boolean) {
        if (currentFrag == null) return

        val theFrag = currentFrag!!

        theFrag.setActionBarTitle()

        val fm = supportFragmentManager
        if (toShow) {
            fm.beginTransaction()
                    .show(theFrag)
                    .commit()
        } else {
            fm.beginTransaction()
                    .hide(theFrag)
                    .commit()
        }
    }

    private val defPreferences: SharedPreferences by lazy { getSharedPreferences(PREF_NAME, 0) }
    private fun setRegisteredUser(name: String) {
        registeredUserName = name
        edtUserName.setText(registeredUserName)
        isAppRegistered = !registeredUserName.isEmpty()
        writePrefUserName()
    }

    private fun writePrefSoundOn() {
        val pref = defPreferences
        pref.edit()
                .putBoolean(PREF_KEY_SOUND, gIsSoundOn)
                .apply()
    }

    private fun writePrefUserName() {
        defPreferences.edit()
                .putString(PREF_KEY_NAME, registeredUserName)
                .apply()
    }

    private fun readPreferences() {
        registeredUserName = defPreferences.getString(PREF_KEY_NAME, "")
        gIsSoundOn = defPreferences.getBoolean(PREF_KEY_SOUND, true)
    }

    companion object {
        internal const val PREF_NAME = "BigTreePref"
        internal const val PREF_KEY_NAME = "pref_key_name"
        internal const val PREF_KEY_SOUND = "pref_key_sound"

        internal const val AMWAY_ON_PACKAGE_ID = "com.ewide.amway"
        internal const val AMWAY_ON_PACKAGE_NAME = "Amway-On"

        internal const val SABN_PACKAGE_ID = "sABN.UI.SmartABNAndroid"
        internal const val SABN_PACKAGE_NAME = "sABN"

        // for sound play option
        private var gIsSoundOn = true

        fun playSound(media: MediaPlayer) {
            if (gIsSoundOn) media.start()
        }
    }
}

// LGS: addition - global properties


