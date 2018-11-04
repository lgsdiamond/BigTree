package com.lgsdiamond.bigtree

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.lgsdiamond.bigtree.ManualFragment.OnFragmentInteractionListener
import com.lgsdiamond.lgsutility.LgsAnimationUtil
import kotlinx.android.synthetic.main.fragment_manual.*


/**
 * A simple [BigTreeFragment] subclass.
 * Activities that contain this fragment must implement the
 * [OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ManualFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ManualFragment : BigTreeFragment() {
    override val barTitle = "\"매뉴얼\" 보기"

    private var mParam1: String? = null
    private var mParam2: String? = null
    private var mListener: OnFragmentInteractionListener? = null

    override fun getTitleTag(): String {
        return barTitle
    }

    override fun setActionBarTitle() {
        val actionBar = (activity as MainActivity).supportActionBar
        actionBar!!.title = barTitle
    }

    override fun handleOptionsItem(id: Int): Boolean {
        return true
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param mParam1 Parameter 1.
     * @param mParam2 Parameter 2.
     * @return A new instance of fragment ReportFragment.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manual, container, false)
    }

    override fun initFragmentUI(view: View) {
        setManualFile(FILENAME_SP_MANUAL)

        btn_SP_manual.setOnClickListener {
            setManualFile(FILENAME_SP_MANUAL)
        }

        btn_GP_manual.setOnClickListener {
            setManualFile(FILENAME_GP_MANUAL)
        }

        btn_PT_manual.setOnClickListener {
            setManualFile(FILENAME_PT_MANUAL)
        }

        btn_FAA_manual.setOnClickListener {
            setManualFile(FILENAME_FAA_MANUAL)
        }
        btn_business_guide.setOnClickListener {
            setManualFile(FILENAME_BUSINESS_GUIDE)
        }

        LgsAnimationUtil.animateCenterScale(view)
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

    private fun setManualFile(fName: String) {
        pdfManual.fromAsset(fName)
                .swipeHorizontal(true)
                .pageFitPolicy(FitPolicy.BOTH)
                .load()
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

    companion object {
        private val ARG_PARAM1 = "param1"
        private val ARG_PARAM2 = "param2"

        fun newInstance(param1: String, param2: String): ManualFragment {
            val fragment = ManualFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }

        const val FILENAME_SP_MANUAL: String = "PF18_SP_manual.pdf"
        const val FILENAME_GP_MANUAL: String = "PF18_GP_manual.pdf"
        const val FILENAME_PT_MANUAL: String = "PF18_PT_manual.pdf"
        const val FILENAME_FAA_MANUAL: String = "PF18_FAA_manual.pdf"
        const val FILENAME_BUSINESS_GUIDE: String = "BusinessReferenceGuide(1709).pdf"
    }
}
