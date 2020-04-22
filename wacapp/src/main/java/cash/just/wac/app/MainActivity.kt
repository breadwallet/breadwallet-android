package cash.just.wac.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import cash.just.wac.WAC
import cash.just.wac.model.ATMListResponse
import cash.just.wac.model.LoginResponse
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginButton.setOnClickListener {
            WAC.login().enqueue(object: retrofit2.Callback<LoginResponse> {
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    session.setText(response.body()!!.data.sessionKey)
                }
            })
        }

        getAtmList.setOnClickListener {
            WAC.getAtmList(session.text.toString()).enqueue(object: retrofit2.Callback<ATMListResponse> {
                override fun onFailure(call: Call<ATMListResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<ATMListResponse>, response: Response<ATMListResponse>) {
                    list.setText(response.body()!!.data.toString())
                }
            })
        }
    }
}
