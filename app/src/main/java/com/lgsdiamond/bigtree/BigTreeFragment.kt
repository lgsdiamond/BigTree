package com.lgsdiamond.bigtree

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View

/**
 * A simple abstract [Fragment] super subclass of all other fragment.
 */
abstract class BigTreeFragment : Fragment() {
    abstract val barTitle: String       // for app bar title

    internal abstract fun setActionBarTitle()
    internal abstract fun getTitleTag(): String
    internal abstract fun handleOptionsItem(id: Int): Boolean
    internal abstract fun initFragmentUI(view: View)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFragmentUI(view)
    }
}
