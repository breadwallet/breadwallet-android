/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 6/7/19.
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
package com.breadwallet.ui.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.databinding.ActivityInAppNotificationBinding
import com.breadwallet.ext.viewModel
import com.breadwallet.legacy.presenter.activities.util.BRActivity
import com.breadwallet.model.InAppMessage
import com.breadwallet.tools.util.asLink
import com.breadwallet.ui.MainActivity
import com.breadwallet.util.CryptoUriParser
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.erased.instance

/**
 * Screen used to display an in-app notification.
 */
class InAppNotificationActivity : BRActivity(), KodeinAware {

    companion object {
        private const val EXT_NOTIFICATION = "com.breadwallet.ui.notification.EXT_NOTIFICATION"

        fun start(context: Context, notification: InAppMessage) {
            val intent = Intent(context, InAppNotificationActivity::class.java).apply {
                putExtra(EXT_NOTIFICATION, notification)
            }
            context.startActivity(intent)
        }
    }

    override val kodein by closestKodein()

    private val breadBox by instance<BreadBox>()
    private val uriParser by instance<CryptoUriParser>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val viewModel by viewModel {
        InAppNotificationViewModel(intent.getParcelableExtra(EXT_NOTIFICATION))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.hasExtra(EXT_NOTIFICATION)) {
            finish()
            return
        }
        val binding = ActivityInAppNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.closeButton.setOnClickListener {
            onBackPressed()
        }
        binding.notificationBtn.setOnClickListener {
            viewModel.markAsRead(true)
            val actionUrl = viewModel.notification.actionButtonUrl
            if (!actionUrl.isNullOrEmpty()) {
                scope.launch(Dispatchers.Main) {
                    if (actionUrl.asLink(breadBox, uriParser) != null) {
                        Intent(this@InAppNotificationActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .putExtra(MainActivity.EXTRA_DATA, actionUrl)
                            .run(this@InAppNotificationActivity::startActivity)
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl)))
                    }
                    finish()
                }
            } else {
                finish()
            }
        }

        binding.notificationTitle.text = viewModel.notification.title
        binding.notificationBody.text = viewModel.notification.body
        binding.notificationBtn.text = viewModel.notification.actionButtonText
        Picasso.get().load(viewModel.notification.imageUrl).into(binding.notificationImage)

        viewModel.markAsShown()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (!isFinishing) {
            viewModel.markAsRead(false)
        }
    }
}
