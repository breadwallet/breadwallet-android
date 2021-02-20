package com.breadwallet.presenter.fragments

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.trimmedLength
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.breadwallet.R
import com.breadwallet.presenter.customviews.BRKeyboard
import com.breadwallet.presenter.customviews.BRLinearLayoutWithCaret
import com.breadwallet.presenter.entities.PaymentItem
import com.breadwallet.tools.animation.BRAnimator
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.animation.SpringAnimator
import com.breadwallet.tools.manager.*
import com.breadwallet.tools.security.BRSender
import com.breadwallet.tools.security.BitcoinUrlHandler
import com.breadwallet.tools.threads.BRExecutor
import com.breadwallet.tools.util.*
import com.breadwallet.wallet.BRWalletManager
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import timber.log.Timber
import java.math.BigDecimal
import java.util.regex.Pattern

/**
 * BreadWallet
 *
 *
 * Created by Mihail Gutan <mihail></mihail>@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
class FragmentSend : Fragment() {
    private lateinit var backgroundLayout: ScrollView
    private lateinit var signalLayout: LinearLayout
    private lateinit var keyboard: BRKeyboard
    private lateinit var addressEdit: EditText
    private lateinit var scan: Button
    private lateinit var paste: Button
    private lateinit var send: Button
    private lateinit var donate: Button
    private lateinit var commentEdit: EditText
    private lateinit var amountBuilder: StringBuilder
    private lateinit var isoText: TextView
    private lateinit var amountEdit: EditText
    private lateinit var balanceText: TextView
    private lateinit var feeText: TextView
    private lateinit var edit: ImageView
    private var curBalance: Long = 0
    private var selectedIso: String? = null
    private lateinit var isoButton: Button
    private var keyboardIndex = 0
    private lateinit var keyboardLayout: LinearLayout
    private lateinit var close: ImageButton
    private lateinit var amountLayout: ConstraintLayout
    private lateinit var feeLayout: BRLinearLayoutWithCaret
    private var feeButtonsShown = false
    private lateinit var feeDescription: TextView
    private lateinit var warningText: TextView
    private var amountLabelOn = true
    private var ignoreCleanup = false

    private lateinit var udDomainEdit: EditText
    private lateinit var udLookupButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_send, container, false)
        backgroundLayout = rootView.findViewById(R.id.background_layout)
        signalLayout = rootView.findViewById<View>(R.id.signal_layout) as LinearLayout
        keyboard = rootView.findViewById<View>(R.id.keyboard) as BRKeyboard
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button)
        keyboard.setBRKeyboardColor(R.color.white)
        isoText = rootView.findViewById<View>(R.id.iso_text) as TextView
        addressEdit = rootView.findViewById<View>(R.id.address_edit) as EditText
        scan = rootView.findViewById<View>(R.id.scan) as Button
        paste = rootView.findViewById<View>(R.id.paste_button) as Button

        udDomainEdit = rootView.findViewById(R.id.ud_address_edit)
        udLookupButton = rootView.findViewById(R.id.ud_lookup_button)

        send = rootView.findViewById<View>(R.id.send_button) as Button
        donate = rootView.findViewById<View>(R.id.donate_button) as Button
        commentEdit = rootView.findViewById<View>(R.id.comment_edit) as EditText
        amountEdit = rootView.findViewById<View>(R.id.amount_edit) as EditText
        balanceText = rootView.findViewById<View>(R.id.balance_text) as TextView
        feeText = rootView.findViewById<View>(R.id.fee_text) as TextView
        edit = rootView.findViewById<View>(R.id.edit) as ImageView
        isoButton = rootView.findViewById<View>(R.id.iso_button) as Button
        keyboardLayout = rootView.findViewById<View>(R.id.keyboard_layout) as LinearLayout
        amountLayout = rootView.findViewById<View>(R.id.amount_layout) as ConstraintLayout
        feeLayout = rootView.findViewById<View>(R.id.fee_buttons_layout) as BRLinearLayoutWithCaret
        feeDescription = rootView.findViewById<View>(R.id.fee_description) as TextView
        warningText = rootView.findViewById<View>(R.id.warning_text) as TextView
        close = rootView.findViewById<View>(R.id.close_button) as ImageButton
        selectedIso = if (BRSharedPrefs.getPreferredLTC(context)) "LTC" else BRSharedPrefs.getIso(context)
        amountBuilder = StringBuilder(0)
        setListeners()
        isoText.text = getString(R.string.Send_amountLabel)
        isoText.textSize = 18f
        isoText.setTextColor(context!!.getColor(R.color.light_gray))
        isoText.requestLayout()
        signalLayout.setOnTouchListener(SlideDetector(context, signalLayout))
        AnalyticsManager.logCustomEvent(BRConstants._20191105_VSC)
        setupFeesSelector(rootView)
        showFeeSelectionButtons(feeButtonsShown)
        edit.setOnClickListener {
            feeButtonsShown = !feeButtonsShown
            showFeeSelectionButtons(feeButtonsShown)
        }
        keyboardIndex = signalLayout.indexOfChild(keyboardLayout)
        //TODO: all views are using the layout of this button. Views should be refactored without it
        // Hiding until layouts are built.
        val faq = rootView.findViewById<View>(R.id.faq_button) as ImageButton
        showKeyboard(false)
        signalLayout.layoutTransition = BRAnimator.getDefaultTransition()
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        bindProgressButton(udLookupButton)
    }

    private fun setupFeesSelector(rootView: View) {
        val feesSegment = rootView.findViewById<RadioGroup>(R.id.fees_segment)
        feesSegment.setOnCheckedChangeListener { _, checkedId -> onFeeTypeSelected(checkedId) }
        onFeeTypeSelected(R.id.regular_fee_but)
    }

    private fun onFeeTypeSelected(checkedId: Int) {
        val feeManager = FeeManager.getInstance()
        when (checkedId) {
            R.id.regular_fee_but -> {
                feeManager.setFeeType(FeeManager.REGULAR)
                BRWalletManager.getInstance().setFeePerKb(feeManager.fees.regular)
                setFeeInformation(R.string.FeeSelector_regularTime, 0, 0, View.GONE)
            }
            R.id.economy_fee_but -> {
                feeManager.setFeeType(FeeManager.ECONOMY)
                BRWalletManager.getInstance().setFeePerKb(feeManager.fees.economy)
                setFeeInformation(R.string.FeeSelector_economyTime, R.string.FeeSelector_economyWarning, R.color.red_text, View.VISIBLE)
            }
            R.id.luxury_fee_but -> {
                feeManager.setFeeType(FeeManager.LUXURY)
                BRWalletManager.getInstance().setFeePerKb(feeManager.fees.luxury)
                setFeeInformation(R.string.FeeSelector_luxuryTime, R.string.FeeSelector_luxuryMessage, R.color.light_gray, View.VISIBLE)
            }
            else -> {
            }
        }
        updateText()
    }

    private fun setFeeInformation(@StringRes deliveryTime: Int, @StringRes warningStringId: Int, @ColorRes warningColorId: Int, visibility: Int) {
        feeDescription.text = getString(R.string.FeeSelector_estimatedDeliver, getString(deliveryTime))
        if (warningStringId != 0) {
            warningText.setText(warningStringId)
        }
        if (warningColorId != 0) {
            warningText.setTextColor(resources.getColor(warningColorId, null))
        }
        warningText.visibility = visibility
    }

    private fun setListeners() {
        amountEdit.setOnClickListener {
            showKeyboard(true)
            if (amountLabelOn) { //only first time
                amountLabelOn = false
                amountEdit.hint = "0"
                amountEdit.textSize = 24f
                balanceText.visibility = View.VISIBLE
                feeText.visibility = View.VISIBLE
                edit.visibility = View.VISIBLE
                isoText.setTextColor(context!!.getColor(R.color.almost_black))
                isoText.text = BRCurrency.getSymbolByIso(activity, selectedIso)
                isoText.textSize = 28f
                val scaleX = amountEdit.scaleX
                amountEdit.scaleX = 0f
                val tr = AutoTransition()
                tr.interpolator = OvershootInterpolator()
                tr.addListener(object : Transition.TransitionListener {
                    override fun onTransitionStart(transition: Transition) {}
                    override fun onTransitionEnd(transition: Transition) {
                        amountEdit.requestLayout()
                        amountEdit.animate().setDuration(100).scaleX(scaleX)
                    }

                    override fun onTransitionCancel(transition: Transition) {}
                    override fun onTransitionPause(transition: Transition) {}
                    override fun onTransitionResume(transition: Transition) {}
                })
                val set = ConstraintSet()
                set.clone(amountLayout)
                TransitionManager.beginDelayedTransition(amountLayout, tr)
                val px4 = Utils.getPixelsFromDps(context, 4)
                set.connect(balanceText.id, ConstraintSet.TOP, isoText.id, ConstraintSet.BOTTOM, px4)
                set.connect(feeText.id, ConstraintSet.TOP, balanceText.id, ConstraintSet.BOTTOM, px4)
                set.connect(feeText.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px4)
                set.connect(isoText.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, px4)
                set.connect(isoText.id, ConstraintSet.BOTTOM, -1, ConstraintSet.TOP, -1)
                set.applyTo(amountLayout)
            }
        }

        //needed to fix the overlap bug
        commentEdit.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                amountLayout.requestLayout()
                return@OnKeyListener true
            }
            false
        })
        paste.setOnClickListener(View.OnClickListener {
            if (!BRAnimator.isClickAllowed()) return@OnClickListener
            val bitcoinUrl = BRClipboardManager.getClipboard(activity)
            if (Utils.isNullOrEmpty(bitcoinUrl) || !isInputValid(bitcoinUrl)) {
                showClipboardError()
                return@OnClickListener
            }
            val obj = BitcoinUrlHandler.getRequestFromString(bitcoinUrl)
            if (obj?.address == null) {
                showClipboardError()
                return@OnClickListener
            }
            val address = obj.address
            val wm = BRWalletManager.getInstance()
            if (BRWalletManager.validateAddress(address)) {
                val app: Activity? = activity
                if (app == null) {
                    Timber.e("paste onClick: app is null")
                    return@OnClickListener
                }
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
                    if (wm.addressContainedInWallet(address)) {
                        app.runOnUiThread(Runnable {
                            BRDialog.showCustomDialog(activity!!, "", resources.getString(R.string.Send_containsAddress), resources.getString(R.string.AccessibilityLabels_close), null, { brDialogView -> brDialogView.dismiss() }, null, null, 0)
                            BRClipboardManager.putClipboard(activity, "")
                        })
                    } else if (wm.addressIsUsed(address)) {
                        app.runOnUiThread(Runnable {
                            BRDialog.showCustomDialog(activity!!, getString(R.string.Send_UsedAddress_firstLine), getString(R.string.Send_UsedAddress_secondLIne), "Ignore", "Cancel", { brDialogView ->
                                brDialogView.dismiss()
                                addressEdit.setText(address)
                            }, { brDialogView -> brDialogView.dismiss() }, null, 0)
                        })
                    } else {
                        app.runOnUiThread(Runnable { addressEdit.setText(address) })
                    }
                }
            } else {
                showClipboardError()
            }
        })
        isoButton.setOnClickListener {
            selectedIso = if (selectedIso.equals(BRSharedPrefs.getIso(context), ignoreCase = true)) {
                "LTC"
            } else {
                BRSharedPrefs.getIso(context)
            }
            updateText()
        }
        scan.setOnClickListener(View.OnClickListener {
            if (!BRAnimator.isClickAllowed()) return@OnClickListener
            saveMetaData()
            BRAnimator.openScanner(activity, BRConstants.SCANNER_REQUEST)
        })

        udLookupButton.setOnClickListener {
            // Disable the button until the domain string is at least 4 chars long (e.g a.zil)
            if (udDomainEdit.text.trimmedLength() < 4) return@setOnClickListener
            lifecycleScope.executeAsyncTask(
                    onPreExecute = {
                        udLookupButton.showProgress {
                            buttonText = null
                            progressColorRes = R.color.litecoin_litewallet_blue
                        }
                        udLookupButton.isEnabled = false
                        addressEdit.text = null
                        AnalyticsManager.logCustomEventWithParams(BRConstants._20201121_SIL, Bundle().apply { putLong(BRConstants.START_TIME, System.currentTimeMillis()) })
                    },
                    doInBackground = { UDResolution().resolve(udDomainEdit.text.trim().toString()) },
                    onPostExecute = {
                        if (it.error == null) {
                            AnalyticsManager.logCustomEventWithParams(BRConstants._20201121_DRIA, Bundle().apply { putLong(BRConstants.SUCCESS_TIME, System.currentTimeMillis()) })
                            addressEdit.setText(it.address)
                            BRAnimator.showBreadSignal(requireActivity(), getString(R.string.Send_UnstoppableDomains_domainResolved), null, R.drawable.ic_check_mark_white, null)
                        } else {
                            AnalyticsManager.logCustomEventWithParams(BRConstants._20201121_FRIA, Bundle().apply {
                                putLong(BRConstants.FAILURE_TIME, System.currentTimeMillis())
                                putString(BRConstants.ERROR, it.error.localizedMessage)
                            })
                            Timber.d(it.error)
                        }
                        udLookupButton.isEnabled = true
                        udLookupButton.hideProgress(R.string.Send_UnstoppableDomains_lookup)
                    }
            )
        }

        send.setOnClickListener(View.OnClickListener { //not allowed now
            if (!BRAnimator.isClickAllowed()) {
                return@OnClickListener
            }
            var allFilled = true
            val address = addressEdit.text.toString()
            val amountStr = amountBuilder.toString()
            val iso = selectedIso
            val comment = commentEdit.text.toString()

            //get amount in satoshis from any isos
            val bigAmount = BigDecimal(if (Utils.isNullOrEmpty(amountStr)) "0" else amountStr)
            val satoshiAmount = BRExchange.getSatoshisFromAmount(activity, iso, bigAmount)
            if (address.isEmpty() || !BRWalletManager.validateAddress(address)) {
                allFilled = false
                SpringAnimator.failShakeAnimation(activity, addressEdit)
            }
            if (amountStr.isEmpty()) {
                allFilled = false
                SpringAnimator.failShakeAnimation(activity, amountEdit)
            }
            if (satoshiAmount.toLong() > BRWalletManager.getInstance().getBalance(activity)) {
                SpringAnimator.failShakeAnimation(activity, balanceText)
                SpringAnimator.failShakeAnimation(activity, feeText)
            }
            if (allFilled) {
                BRSender.getInstance().sendTransaction(context, PaymentItem(arrayOf(address), null, satoshiAmount.toLong(), null, false, comment))
                AnalyticsManager.logCustomEvent(BRConstants._20191105_DSL)
            }
        })
        donate.setOnClickListener(View.OnClickListener { //not allowed now
            if (!BRAnimator.isClickAllowed()) {
                return@OnClickListener
            }
            BRAnimator.showDynamicDonationFragment(activity!!)
        })
        backgroundLayout.setOnClickListener(View.OnClickListener {
            if (!BRAnimator.isClickAllowed()) return@OnClickListener
            activity!!.onBackPressed()
        })
        close.setOnClickListener {
            activity?.onBackPressed()
        }
        addressEdit.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                Utils.hideKeyboard(activity)
                Handler().postDelayed({ showKeyboard(true) }, 500)
            }
            false
        }
        keyboard.addOnInsertListener { key -> handleClick(key) }
    }

    private fun showKeyboard(b: Boolean) {
        val curIndex = keyboardIndex
        if (!b) {
            signalLayout.removeView(keyboardLayout)
        } else {
            Utils.hideKeyboard(activity)
            if (signalLayout.indexOfChild(keyboardLayout) == -1) signalLayout.addView(keyboardLayout, curIndex) else signalLayout.removeView(keyboardLayout)
        }
    }

    private fun showClipboardError() {
        BRDialog.showCustomDialog(activity!!, getString(R.string.Send_emptyPasteboard), resources.getString(R.string.Send_invalidAddressTitle), getString(R.string.AccessibilityLabels_close), null, { brDialogView -> brDialogView.dismiss() }, null, null, 0)
        BRClipboardManager.putClipboard(activity, "")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val observer = signalLayout.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (observer.isAlive) {
                    observer.removeOnGlobalLayoutListener(this)
                }
                BRAnimator.animateBackgroundDim(backgroundLayout, false)
                BRAnimator.animateSignalSlide(signalLayout, false) {
                    val bundle = arguments
                    if (bundle?.getString("url") != null) setUrl(bundle.getString("url"))
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        FeeManager.getInstance().resetFeeType()
    }

    override fun onResume() {
        super.onResume()
        loadMetaData()
    }

    override fun onPause() {
        super.onPause()
        Utils.hideKeyboard(activity)
        if (!ignoreCleanup) {
            savedIso = null
            savedAmount = null
            savedMemo = null
        }
    }

    private fun handleClick(key: String?) {
        if (key == null) {
            Timber.d("handleClick: key is null! ")
            return
        }
        when {
            key.isEmpty() -> {
                handleDeleteClick()
            }
            Character.isDigit(key[0]) -> {
                handleDigitClick(key.substring(0, 1).toInt())
            }
            key[0] == '.' -> {
                handleSeparatorClick()
            }
        }
    }

    private fun handleDigitClick(dig: Int) {
        val currAmount = amountBuilder.toString()
        val iso = selectedIso
        if (BigDecimal(currAmount + dig.toString()).toDouble()
                <= BRExchange.getMaxAmount(activity, iso).toDouble()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equals("0", ignoreCase = true)) amountBuilder = StringBuilder("")
            if (currAmount.contains(".") && currAmount.length - currAmount.indexOf(".") > BRCurrency.getMaxDecimalPlaces(iso)) return
            amountBuilder.append(dig)
            updateText()
        }
    }

    private fun handleSeparatorClick() {
        val currAmount = amountBuilder.toString()
        if (currAmount.contains(".") || BRCurrency.getMaxDecimalPlaces(selectedIso) == 0) return
        amountBuilder.append(".")
        updateText()
    }

    private fun handleDeleteClick() {
        val currAmount = amountBuilder.toString()
        if (currAmount.isNotEmpty()) {
            amountBuilder.deleteCharAt(currAmount.length - 1)
            updateText()
        }
    }

    private fun updateText() {
        if (activity == null) return
        val tmpAmount = amountBuilder.toString()
        setAmount()
        val iso = selectedIso
        val currencySymbol = BRCurrency.getSymbolByIso(activity, selectedIso)
        curBalance = BRWalletManager.getInstance().getBalance(activity)
        if (!amountLabelOn) isoText.text = currencySymbol
        isoButton.text = String.format("%s(%s)", BRCurrency.getCurrencyName(activity, selectedIso), currencySymbol)
        //Balance depending on ISO
        val satoshis = if (Utils.isNullOrEmpty(tmpAmount) || tmpAmount.equals(".", ignoreCase = true)) 0 else if (selectedIso.equals("btc", ignoreCase = true)) BRExchange.getSatoshisForBitcoin(activity, BigDecimal(tmpAmount)).toLong() else BRExchange.getSatoshisFromAmount(activity, selectedIso, BigDecimal(tmpAmount)).toLong()
        val balanceForISO = BRExchange.getAmountFromSatoshis(activity, iso, BigDecimal(curBalance))
        Timber.d("updateText: balanceForISO: %s", balanceForISO)

        //formattedBalance
        val formattedBalance = BRCurrency.getFormattedCurrencyString(activity, iso, balanceForISO)
        //Balance depending on ISO
        var fee: Long
        if (satoshis == 0L) {
            fee = 0
        } else {
            fee = BRWalletManager.getInstance().feeForTransactionAmount(satoshis).toLong()
            if (fee == 0L) {
                Timber.i("updateText: fee is 0, trying the estimate")
                fee = BRWalletManager.getInstance().feeForTransaction(addressEdit.text.toString(), satoshis).toLong()
            }
        }
        val feeForISO = BRExchange.getAmountFromSatoshis(activity, iso, BigDecimal(if (curBalance == 0L) 0 else fee))
        Timber.d("updateText: feeForISO: %s", feeForISO)
        //formattedBalance
        val aproxFee = BRCurrency.getFormattedCurrencyString(activity, iso, feeForISO)
        Timber.d("updateText: aproxFee: %s", aproxFee)
        if (BigDecimal(if (tmpAmount.isEmpty() || tmpAmount.equals(".", ignoreCase = true)) "0" else tmpAmount).toDouble() > balanceForISO.toDouble()) {
            balanceText.setTextColor(context!!.getColor(R.color.warning_color))
            feeText.setTextColor(context!!.getColor(R.color.warning_color))
            amountEdit.setTextColor(context!!.getColor(R.color.warning_color))
            if (!amountLabelOn) isoText.setTextColor(context!!.getColor(R.color.warning_color))
        } else {
            balanceText.setTextColor(context!!.getColor(R.color.light_gray))
            feeText.setTextColor(context!!.getColor(R.color.light_gray))
            amountEdit.setTextColor(context!!.getColor(R.color.almost_black))
            if (!amountLabelOn) isoText.setTextColor(context!!.getColor(R.color.almost_black))
        }
        balanceText.text = getString(R.string.Send_balance, formattedBalance)
        feeText.text = String.format(getString(R.string.Send_fee), aproxFee)
        donate.text = getString(R.string.Donate_title, currencySymbol)
        donate.isEnabled = curBalance >= BRConstants.DONATION_AMOUNT * 2
        amountLayout.requestLayout()
    }

    fun setUrl(url: String?) {
        val obj = BitcoinUrlHandler.getRequestFromString(url) ?: return
        if (obj.address != null) {
            addressEdit.setText(obj.address.trim { it <= ' ' })
        }
        if (obj.message != null) {
            commentEdit.setText(obj.message)
        }
        if (obj.amount != null) {
            val iso = selectedIso
            val satoshiAmount = BigDecimal(obj.amount).multiply(BigDecimal(100000000))
            amountBuilder = StringBuilder(BRExchange.getAmountFromSatoshis(activity, iso, satoshiAmount).toPlainString())
            updateText()
        }
    }

    private fun showFeeSelectionButtons(b: Boolean) {
        if (!b) {
            signalLayout.removeView(feeLayout)
        } else {
            signalLayout.addView(feeLayout, signalLayout.indexOfChild(amountLayout) + 1)
        }
    }

    private fun setAmount() {
        val tmpAmount = amountBuilder.toString()
        var divider = tmpAmount.length
        if (tmpAmount.contains(".")) {
            divider = tmpAmount.indexOf(".")
        }
        val newAmount = StringBuilder()
        for (i in tmpAmount.indices) {
            newAmount.append(tmpAmount[i])
            if (divider > 3 && divider - 1 != i && divider > i && (divider - i - 1) % 3 == 0) {
                newAmount.append(",")
            }
        }
        amountEdit.setText(newAmount.toString())
    }

    private fun isInputValid(input: String): Boolean {
        return Pattern.matches("[a-zA-Z0-9]*", input)
    }

    // from the link above
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Timber.d("onConfigurationChanged: hidden")
            showKeyboard(true)
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            Timber.d("onConfigurationChanged: shown")
            showKeyboard(false)
        }
    }

    private fun saveMetaData() {
        if (commentEdit.text.toString().isNotEmpty()) savedMemo = commentEdit.text.toString()
        if (amountBuilder.toString().isNotEmpty()) savedAmount = amountBuilder.toString()
        savedIso = selectedIso
        ignoreCleanup = true
    }

    private fun loadMetaData() {
        ignoreCleanup = false
        if (!Utils.isNullOrEmpty(savedMemo)) commentEdit.setText(savedMemo)
        if (!Utils.isNullOrEmpty(savedIso)) selectedIso = savedIso
        if (!Utils.isNullOrEmpty(savedAmount)) {
            amountBuilder = StringBuilder(savedAmount!!)
            Handler().postDelayed({
                amountEdit.performClick()
                updateText()
            }, 500)
        }
    }

    companion object {
        private var savedMemo: String? = null
        private var savedIso: String? = null
        private var savedAmount: String? = null
    }
}