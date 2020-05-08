package com.breadwallet.ui.atm

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import cash.just.wac.WacSDK
import cash.just.wac.model.AtmMachine
import cash.just.wac.model.CashCodeResponse
import cash.just.wac.model.CashCodeStatusResponse
import cash.just.wac.model.CashStatus
import cash.just.wac.model.SendVerificationCodeResponse
import cash.just.wac.model.parseError
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.legacy.wallet.wallets.bitcoin.WalletBitcoinManager
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.breadwallet.ui.send.SendSheetController
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.platform.PlatformTransactionBus
import kotlinx.android.synthetic.main.fragment_request_cash_code.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Suppress("TooManyFunctions")
class RequestCashCodeController(
    args: Bundle
) : BaseController(args) {

    constructor(atm: AtmMachine) : this(
        bundleOf(atmMachine to atm)
    )

    companion object {
        private const val HTTP_OK_CODE = 200
        private const val CLICKS_TO_START_ANIMATION = 3
        private const val atmMachine = "RequestCashCodeController.Atm"
        private const val MAP_FRAGMENT_TAG = "MAP_FRAGMENT_TAG"
    }

    override val layoutId = R.layout.fragment_request_cash_code

    private enum class VerificationState {
        PHONE,
        EMAIL
    }
    private var currentVerificationMode: VerificationState = VerificationState.PHONE
    private var coinCount = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)

        val atm: AtmMachine = arg(atmMachine)

        verificationGroup.visibility = View.VISIBLE
        confirmGroup.visibility = View.GONE

        noPhoneButton.setOnClickListener {
            toggleVerification()
        }

        handlePlatformMessages().launchIn(viewCreatedScope)

        prepareMap(view.context, atm)

        atmTitle.text = atm.addressDesc
        amount.helperText = "Min ${atm.min}$ max ${atm.max}$, multiple of ${atm.bills.toFloat().toInt()}$ bills"

        getAtmCode.setOnClickListener {
            // dropView.startAnimation()

            if (!WacSDK.isSessionCreated()) {
                Toast.makeText(view.context, "Invalid session", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (checkFields()) {
                Toast.makeText(view.context, "Complete the fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (checkAmount(atm)) {
                Toast.makeText(view.context, "Amount not valid, it has to be between ${atm.min.toInt()} and ${atm.max.toInt()}.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val amount = getAmount()!!.toFloat().toInt()
            val bills = atm.bills.toFloat().toInt()
            if (amount.rem(bills) != 0) {
                Toast.makeText(view.context, "Amount must be multiple of ${atm.bills}$", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            requestVerificationCode(view.context)
        }

        confirmAction.setOnClickListener {


            if (!WacSDK.isSessionCreated()) {
                Toast.makeText(view.context, "invalid session", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (getToken().isNullOrEmpty()) {
                Toast.makeText(view.context, "Token is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard(view.context, token.editText!!)
            createCashCode(view.context, atm)
        }

        dropView.setDrawables(R.drawable.bitcoin, R.drawable.bitcoin)
    }

    private fun playCoinSound() {
        val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.smw_coin)
        mediaPlayer.start()

        coinCount++
        mediaPlayer.setOnCompletionListener { mp ->
            mp?.let{
                it.reset()
                it.release()
            }
        }

        if (coinCount == CLICKS_TO_START_ANIMATION) {
            dropView.startAnimation()
        }
    }

    private fun requestVerificationCode(context: Context){
        WacSDK.sendVerificationCode(
            getName()!!,
            getSurname()!!,
            getPhone(),
            getEmail()
        ).enqueue(object: Callback<SendVerificationCodeResponse> {
            override fun onResponse(
                call: Call<SendVerificationCodeResponse>,
                response: Response<SendVerificationCodeResponse>
            ) {
                if (response.code() == HTTP_OK_CODE) {
                    Toast.makeText(context, response.body()!!.data.items[0].result, Toast.LENGTH_SHORT).show()
                    if (getEmail() != null) {
                        confirmationMessage.text = "We've sent a confirmation token to your email."
                    } else {
                        confirmationMessage.text = "We've sent a confirmation token to your phone by SMS."
                    }
                    verificationGroup.visibility = View.GONE
                    confirmGroup.visibility = View.VISIBLE

                } else {
                    Toast.makeText(context, "error" + response.code(), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SendVerificationCodeResponse>, t: Throwable) {
                Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun hideKeyboard(context: Context, editText: EditText){
        val imm: InputMethodManager? =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    private fun createCashCode(context: Context, atm: AtmMachine){

        WacSDK.createCashCode(atm.atmId, getAmount()!!, getToken()!!)
            .enqueue(object: Callback<CashCodeResponse> {
                override fun onResponse(
                    call: Call<CashCodeResponse>,
                    response: Response<CashCodeResponse>
                ) {
                    if (response.code() == HTTP_OK_CODE) {
                        val code = response.body()!!.data.items[0].secureCode
                        proceedWithCashCode(context, code)
                    } else {

                        val errorBody = response.errorBody()
                        errorBody?.let {
                            it.parseError().error.server_message.let { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        Toast.makeText(context, "error " + response.code(), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<CashCodeResponse>,
                    t: Throwable
                ) {
                    Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun proceedWithCashCode(context: Context, code:String) {
        AtmSharedPreferencesManager.setWithdrawalRequest(context, code)

        WacSDK.checkCashCodeStatus(code).enqueue(object: Callback<CashCodeStatusResponse> {
            override fun onResponse(
                call: Call<CashCodeStatusResponse>,
                response: Response<CashCodeStatusResponse>
            ) {
                val cashStatus = response.body()!!.data!!.items[0]
                showDialog(context, code, cashStatus)
            }

            override fun onFailure(call: Call<CashCodeStatusResponse>, t: Throwable) {
                Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDialog(context:Context, code:String, cashStatus: CashStatus){
        BRDialog.showCustomDialog(
            context, "Withdrawal requested",
            "Please send the amount of ${cashStatus.btc_amount} BTC to the ATM",
            "Send", "Details", { dialog ->
                goToSend(cashStatus.btc_amount, cashStatus.address)
                dialog.dismissWithAnimation()
            },
            { dialog ->
                goToDetails(context, code)
                dialog.dismissWithAnimation()
            }, null)
    }

    private fun prepareMap(context : Context, atm:AtmMachine) {
        val fragment = createAndHideMap(context)
        fragment.getMapAsync(object: OnMapReadyCallback {
            override fun onMapReady(googleMap: GoogleMap?) {
                googleMap ?: return

                googleMap.uiSettings.isMapToolbarEnabled = false
                googleMap.uiSettings.isMyLocationButtonEnabled = false
                googleMap.uiSettings.isScrollGesturesEnabled = false
                googleMap.uiSettings.isZoomGesturesEnabled = false
                googleMap.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = false
                showMap(context)
                googleMap.setOnMapLoadedCallback(OnMapLoadedCallback {
                    addMarkerAndMoveCamera(context, googleMap, atm)
                    coinCount = 0
                    dropView.stopAnimation()
                })

                googleMap.setOnInfoWindowClickListener {
                    playCoinSound()
                }
            }
        })
    }

    private fun goToDetails(context:Context, code:String){
        Toast.makeText(context, "goToDetails $code", Toast.LENGTH_SHORT).show()
    }

    private fun goToSend(btc:String, address:String) {
        val builder = CryptoRequest.Builder()
        builder.address = address
        builder.amount = btc.toFloat().toBigDecimal()
        builder.currencyCode = WalletBitcoinManager.BITCOIN_CURRENCY_CODE
        val request = builder.build()
        router.pushController(RouterTransaction.with(SendSheetController(
            request //make it default
        )))
    }

    private fun showMap(context:Context) {
        val fragmentManager = AtmMapHelper.getActivityFromContext(context)!!.supportFragmentManager
        val fragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG)
        fragment?.let{
            fragmentManager.beginTransaction()
                .show(fragment)
                .commit()
        }
    }

    private fun createAndHideMap(context:Context): SupportMapFragment {
        val fragment = AtmMapHelper.addMapFragment(context, R.id.smallMapFragment, MAP_FRAGMENT_TAG)
        val fragmentManager = AtmMapHelper.getActivityFromContext(context)!!.supportFragmentManager
        fragmentManager.beginTransaction()
            .hide(fragment)
            .commit()
        return fragment
    }

    private fun addMarkerAndMoveCamera(context: Context, googleMap: GoogleMap, atm: AtmMachine){
        val markerOpt = WacMarker.getMarker(context, atm)

        val marker = googleMap.addMarker(markerOpt)
        marker.tag = atm

        val cameraPosition: CameraPosition = CameraPosition.Builder()
            .target(markerOpt.position)
            .zoom(15f)
            .build()

        val cameraUpdate: CameraUpdate = CameraUpdateFactory
            .newCameraPosition(cameraPosition)
        googleMap.moveCamera(cameraUpdate)
    }

    private fun checkAmount(atm : AtmMachine): Boolean {
        val amount = getAmount()?.toFloatOrNull() ?: return false
        val min = atm.min.toFloatOrNull()
        val max = atm.min.toFloatOrNull()
        if (min != null && max != null) {
            return (amount >= min && amount <= max)
        }
        return true
    }

    private fun checkFields(): Boolean {
        return getAmount().isNullOrEmpty() ||
            getName().isNullOrEmpty() || getSurname().isNullOrEmpty() ||
            (getEmail().isNullOrEmpty() && getPhone().isNullOrEmpty())
    }

    private fun getAmount(): String? {
        return amount.editText?.text.toString()
    }

    private fun getToken(): String? {
        return token.editText?.text.toString()
    }

    private fun getName(): String? {
        return firstName.editText?.text.toString()
    }

    private fun getSurname(): String? {
        return lastName.editText?.text.toString()
    }

    private fun getPhone() : String? {
        return phoneNumber.editText?.text.toString()
    }

    private fun getEmail() : String? {
        return email.editText?.text.toString()
    }

    private fun toggleVerification() {
        if (currentVerificationMode == VerificationState.PHONE) {
            phoneNumber.visibility = View.GONE
            email.visibility = View.VISIBLE
            noPhoneButton.text = "Phone Number"
            currentVerificationMode = VerificationState.EMAIL
        } else {
            phoneNumber.visibility = View.VISIBLE
            email.visibility = View.GONE
            noPhoneButton.text = "No Phone?"
            currentVerificationMode = VerificationState.PHONE
        }
    }

    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Dispatchers.Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }
}
