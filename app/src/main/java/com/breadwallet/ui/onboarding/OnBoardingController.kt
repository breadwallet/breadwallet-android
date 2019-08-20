/**
 * BreadWallet
 *
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/12/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.breadwallet.R
import com.breadwallet.presenter.activities.InputPinActivity
import com.breadwallet.presenter.activities.PaperKeyActivity
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.security.BRKeyStore
import com.breadwallet.tools.security.PostAuth
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.BaseController
import com.platform.APIClient
import com.platform.HTTPServer
import kotlinx.android.synthetic.main.activity_on_boarding.*
import kotlinx.android.synthetic.main.fragment_onboarding.*

class OnBoardingController(args: Bundle? = null) : BaseController(args) {

    companion object {
        @JvmStatic
        fun showBuyScreen(activity: Activity) {
            // TODO: Don't hardcode currency code, don't rely on WalletBitcoinManager
            val url = "${HTTPServer.getPlatformUrl(HTTPServer.URL_BUY)}?currency=BTC"
            UiUtils.startPlatformBrowser(activity, url)
        }
    }

    override val layoutId = R.layout.activity_on_boarding

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        button_skip.setOnClickListener {
            EventUtils.pushEvent(EventUtils.EVENT_SKIP_BUTTON)
            EventUtils.pushEvent(EventUtils.EVENT_FINAL_PAGE_BROWSE_FIRST)

            button_skip.isEnabled = false

            if (BRKeyStore.getPinCode(applicationContext).isNotEmpty()) {
                UiUtils.startBreadActivity(activity, true)
            } else {
                setupPin(false)
            }
        }
        button_back.setOnClickListener {
            EventUtils.pushEvent(EventUtils.EVENT_BACK_BUTTON)
            router.handleBack()
        }

        view_pager.apply {
            adapter = object : RouterPagerAdapter(this@OnBoardingController) {
                override fun configureRouter(router: Router, position: Int) {
                    if (!router.hasRootController()) {
                        router.setRoot(RouterTransaction.with(
                                when (position) {
                                    0 -> PageOneController()
                                    1 -> PageTwoController()
                                    2 -> PageThreeController()
                                    else -> error("Unknown position")
                                }
                        ))
                    }
                }

                override fun getCount(): Int = 3
            }
            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    val isFirstPage = position == 0
                    button_skip.isVisible = isFirstPage
                    button_back.isVisible = isFirstPage

                    updateIndicators(position)
                    dispatchEvent(position)
                }
            })
        }
    }

    private fun updateIndicators(page: Int) {
        listOf(indicator1, indicator2, indicator3)
                .forEachIndexed { index, indicator ->
                    indicator.background = resources!!.getDrawable(when (index) {
                        page -> R.drawable.page_indicator_active
                        else -> R.drawable.page_indicator_inactive
                    })
                }
    }

    private fun dispatchEvent(page: Int) {
        EventUtils.pushEvent(when (page) {
            0 -> EventUtils.EVENT_GLOBE_PAGE_APPEARED
            1 -> EventUtils.EVENT_COINS_PAGE_APPEARED
            2 -> EventUtils.EVENT_FINAL_PAGE_APPEARED
            else -> error("invalid position")
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        loading_view.isVisible = false
    }

    fun setupPin(goToBuy: Boolean) {
        loading_view.isVisible = true
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            PostAuth.getInstance().onCreateWalletAuth(activity, false) {
                APIClient.getInstance(applicationContext).updatePlatform()
                activity?.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
                startActivity(Intent(applicationContext, InputPinActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                    if (goToBuy) {
                        putExtra(
                                InputPinActivity.EXTRA_PIN_NEXT_SCREEN,
                                PaperKeyActivity.DoneAction.SHOW_BUY_SCREEN.name
                        )
                    }
                    putExtra(InputPinActivity.EXTRA_PIN_IS_ONBOARDING, true)
                })
            }
        }
    }
}

class PageOneController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.fragment_onboarding
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        primary_text.setText(R.string.OnboardingPageTwo_title)
        secondary_text.setText(R.string.OnboardingPageTwo_subtitle)
    }
}

class PageTwoController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.fragment_onboarding
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        primary_text.setText(R.string.OnboardingPageThree_title)
        secondary_text.setText(R.string.OnboardingPageThree_subtitle)
        image_view.setImageDrawable(resources!!.getDrawable(R.drawable.ic_currencies))
    }
}

class PageThreeController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.fragment_onboarding

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        val onBoardingController = (parentController as? OnBoardingController)
        last_screen_title.isVisible = true
        button_buy.isVisible = true
        button_browse.isVisible = true
        primary_text.isVisible = false
        secondary_text.isVisible = false
        image_view.isVisible = false
        button_buy.setOnClickListener {
            button_browse.isEnabled = false
            button_buy.isEnabled = false
            onBoardingController?.setupPin(true)
        }
        button_browse.setOnClickListener {
            button_browse.isEnabled = false
            button_buy.isEnabled = false

            EventUtils.pushEvent(EventUtils.EVENT_FINAL_PAGE_BROWSE_FIRST)
            onBoardingController?.setupPin(false)
        }
    }
}