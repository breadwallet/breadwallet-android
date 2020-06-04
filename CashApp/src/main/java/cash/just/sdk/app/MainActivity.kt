package cash.just.sdk.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import cash.just.sdk.Cash
import cash.just.sdk.CashSDK
import cash.just.sdk.model.AtmListResponse
import cash.just.sdk.model.CashCodeResponse
import cash.just.sdk.model.CashCodeStatusResponse
import cash.just.sdk.model.SendVerificationCodeResponse
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        afterLoginPanel.visibility = View.GONE

        loginButton.setOnClickListener {
            val server = if (serverToggleButton.isChecked) Cash.BtcNetwork.TEST_NET
            else Cash.BtcNetwork.MAIN_NET

            CashSDK.createSession(server, object: Cash.SessionCallback {
                override fun onSessionCreated(sessionKey: String) {
                    session.setText(sessionKey)
                    afterLoginPanel.visibility = View.VISIBLE
                }

                override fun onError(errorMessage: String?) {
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
        }

        getAtmList.setOnClickListener {
            list.text.clear()
            CashSDK.getAtmList().enqueue(object: retrofit2.Callback<AtmListResponse> {
                override fun onFailure(call: Call<AtmListResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<AtmListResponse>, response: Response<AtmListResponse>) {
                    list.setText(response.body()!!.data.toString())
                }
            })
        }

        getAtmListByLatitude.setOnClickListener {
            list.text.clear()
            CashSDK.getAtmListByLocation(lat.text.toString(), lon.text.toString())
                .enqueue(object: retrofit2.Callback<AtmListResponse> {
                    override fun onFailure(call: Call<AtmListResponse>, t: Throwable) {
                        Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                    }

                    override fun onResponse(call: Call<AtmListResponse>, response: Response<AtmListResponse>) {
                        list.setText(response.body()!!.data.toString())
                    }
            })
        }

        checkCode.setOnClickListener {
            CashSDK.checkCashCodeStatus(code.text.toString()).enqueue(object: retrofit2.Callback<CashCodeStatusResponse> {
                override fun onResponse(call: Call<CashCodeStatusResponse>, response: Response<CashCodeStatusResponse>) {
                    if (response.isSuccessful
                        && response.body() != null
                        && response.body()!!.data != null
                        && response.body()!!.data!!.items.isNotEmpty()
                    ) {
                        val result = response.body()!!.data!!.items[0]
                        Toast.makeText(applicationContext, result.getCodeStatus().toString() + " " + result.toString(), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, response.code().toString(), Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<CashCodeStatusResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }
            })
        }

        sendVerificationCode.setOnClickListener {
            CashSDK.sendVerificationCode(
                firstName.text.toString(),
                lastName.text.toString(),
                phoneNumber.text.toString(),
                email.text.toString()).enqueue(object: retrofit2.Callback<SendVerificationCodeResponse> {
                override fun onFailure(call: Call<SendVerificationCodeResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<SendVerificationCodeResponse>, responseVerification: Response<SendVerificationCodeResponse>) {
                    if (responseVerification.isSuccessful) {
                        Toast.makeText(applicationContext, responseVerification.body()!!.data.toString(), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, responseVerification.code().toString(), Toast.LENGTH_LONG).show()
                    }
                }
            })
        }

        createCode.setOnClickListener {
            CashSDK.createCashCode(
                atmId.text.toString(),
                amount.text.toString(),
                verificationCode.text.toString()).enqueue(object: retrofit2.Callback<CashCodeResponse> {
                override fun onFailure(call: Call<CashCodeResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<CashCodeResponse>, response: Response<CashCodeResponse>) {
                    if (response.isSuccessful) {
                        val responseText = response.body()!!.data.toString()
                        createCodeResult.setText(responseText)
                        Toast.makeText(applicationContext, responseText, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, response.code().toString(), Toast.LENGTH_LONG).show()
                    }
                }
            })
        }
    }
}
