package com.breadwallet.presenter.spend

import android.content.Context
import androidx.annotation.StyleRes
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Litewallet
 * Created by Mohamed Barry on 6/10/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
class AuthBottomSheetDialog(
    context: Context,
    @StyleRes theme: Int,
    private val backPressedCallback: () -> Unit
) : BottomSheetDialog(context, theme) {

    override fun onBackPressed() {
        backPressedCallback()
    }
}
