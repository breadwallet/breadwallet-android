/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 11/6/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.atm

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import cash.just.wac.Wac
import cash.just.wac.WacSDK
import cash.just.wac.model.AtmMachine
import cash.just.wac.model.CashCodeResponse
import cash.just.wac.model.SendVerificationCodeResponse
import cash.just.wac.model.parseError
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
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
        private const val atmMachine = "RequestCashCodeController.Atm"
    }

    override val layoutId = R.layout.fragment_request_cash_code

    private enum class VerificationState {
        PHONE,
        EMAIL
    }
    private var currentVerificationMode: VerificationState = VerificationState.PHONE

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
        amount.helperText = "Min ${atm.min}$ max ${atm.max}$"
        atmTitle.setOnClickListener {
            WacSDK.createSession(object: Wac.SessionCallback {
                override fun onSessionCreated(sessionKey: String) {
                    Toast.makeText(view.context, "session created", Toast.LENGTH_SHORT).show()
                }

                override fun onError(errorMessage: String?) {
                    Toast.makeText(view.context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
        }

        getAtmCode.setOnClickListener {
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

            WacSDK.sendVerificationCode(
                getName()!!,
                getSurname()!!,
                getEmail(),
                getPhone()
            ).enqueue(object: Callback<SendVerificationCodeResponse> {
                override fun onResponse(
                    call: Call<SendVerificationCodeResponse>,
                    response: Response<SendVerificationCodeResponse>
                ) {
                    if (response.code() == 200) {
                        Toast.makeText(view.context, response.body()!!.data.items[0].result, Toast.LENGTH_SHORT).show()
                        if (getEmail() != null) {
                            confirmationMessage.text = "We've sent a confirmation token to your email."
                        } else {
                            confirmationMessage.text = "We've sent a confirmation token to your phone by SMS."
                        }
                        verificationGroup.visibility = View.GONE
                        confirmGroup.visibility = View.VISIBLE

                    } else {
                        Toast.makeText(view.context, "error" + response.code(), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SendVerificationCodeResponse>, t: Throwable) {
                    Toast.makeText(view.context, t.message, Toast.LENGTH_SHORT).show()
                }
            })
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

            WacSDK.createCashCode(atm.atmId, getAmount()!!, getToken()!!)
                .enqueue(object: Callback<CashCodeResponse> {
                    override fun onResponse(
                        call: Call<CashCodeResponse>,
                        response: Response<CashCodeResponse>
                    ) {

                        if (response.code() == 200) {
                            val code = response.body()!!.data.items[0].secureCode
                            Toast.makeText(view.context, "code created", Toast.LENGTH_SHORT).show()
                            AtmSharedPreferencesManager.setWithdrawalRequest(view.context, code)
                            verificationGroup.visibility = View.GONE
                            confirmGroup.visibility = View.VISIBLE

                        } else {

                            val errorBody = response.errorBody()
                            errorBody?.let {
                                it.parseError().error.server_message.let { message ->
                                    Toast.makeText(view.context, message, Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                            Toast.makeText(view.context, "error " + response.code(), Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<CashCodeResponse>,
                        t: Throwable
                    ) {
                        Toast.makeText(view.context, t.message, Toast.LENGTH_SHORT).show()
                    }
                })
            }
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
                })
            }
        })
    }


    private fun showMap(context:Context) {
        val fragmentManager = AtmMapHelper.getActivityFromContext(context)!!.supportFragmentManager
        val fragment = fragmentManager.findFragmentByTag("SMALL_MAP")
        fragment?.let{
            fragmentManager.beginTransaction()
                .show(fragment)
                .commit()
        }
    }

    private fun createAndHideMap(context:Context): SupportMapFragment {
        val fragment = AtmMapHelper.addMapFragment(context, R.id.smallMapFragment, "SMALL_MAP")
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
            .zoom(13f)
            .build()

        val cameraUpdate: CameraUpdate = CameraUpdateFactory
            .newCameraPosition(cameraPosition)
        googleMap.moveCamera(cameraUpdate)
    }

    private fun checkAmount(atm : AtmMachine): Boolean {
        val amount = getAmount()?.toFloatOrNull()
        val min = atm.min.toFloatOrNull()
        val max = atm.min.toFloatOrNull()
        if (amount == null) {
           return false
        } else if(min != null && max != null) {
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
