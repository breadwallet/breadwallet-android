package com.breadwallet.presenter.spend

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import com.breadwallet.R
import com.breadwallet.tools.util.Utils
import com.breadwallet.tools.util.addFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Litewallet
 * Created by Mohamed Barry on 6/10/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
class AuthBottomSheetDialogFragment : RoundedBottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_authentication, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addFragment(LoginFragment(), false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (requireDialog() as BottomSheetDialog).dismissWithAnimation = true
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackPressed()
                }
            }
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AuthBottomSheetDialog(requireContext(), theme) { onBackPressed() }
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.setPeekHeight(0, true)
        behavior.setExpandedOffset(Utils.getPixelsFromDps(context, 20))
        behavior.isFitToContents = false
        behavior.isHideable = true

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })

        dialog.setOnShowListener {
            val bottomSheet: FrameLayout? = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            val lp = bottomSheet?.layoutParams
            lp?.height = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet?.layoutParams = lp
        }
        return dialog
    }

    fun onBackPressed() {
        if (childFragmentManager.backStackEntryCount == 0) {
            dialog?.dismiss()
        } else {
            childFragmentManager.popBackStack()
        }
    }
}
