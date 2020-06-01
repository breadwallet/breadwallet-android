/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/12/19.
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
package com.breadwallet.ui.onboarding

import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import android.view.View
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.onboarding.OnBoarding.E
import com.breadwallet.ui.onboarding.OnBoarding.F
import com.breadwallet.ui.onboarding.OnBoarding.M
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_on_boarding.*
import kotlinx.android.synthetic.main.controller_onboarding_page.*
import kotlinx.coroutines.SupervisorJob
import org.kodein.di.direct
import org.kodein.di.erased.instance

class OnBoardingController(
    args: Bundle? = null
) : BaseMobiusController<M, E, F>(args) {

    private val activeIndicator by lazy {
        val resId = R.drawable.page_indicator_active
        checkNotNull(resources).getDrawable(resId, checkNotNull(activity).theme)
    }
    private val inactiveIndicator by lazy {
        val resId = R.drawable.page_indicator_inactive
        checkNotNull(resources).getDrawable(resId, checkNotNull(activity).theme)
    }

    override val layoutId = R.layout.controller_on_boarding

    private val effectJob = SupervisorJob()
    private val _effectHandler by lazy {
        OnBoardingHandler(
            effectJob,
            applicationContext as BreadApp,
            direct.instance(),
            direct.instance(),
            direct.instance(),
            { eventConsumer },
            { router }
        )
    }

    override val defaultModel = M.DEFAULT
    override val init = OnBoardingInit
    override val update = OnBoardingUpdate
    override val effectHandler: Connectable<F, E> =
        CompositeEffectHandler.from(
            Connectable { _effectHandler },
            nestedConnectable({ RouterNavigationEffectHandler(router) }, { effect ->
                when (effect) {
                    is F.ShowError -> NavigationEffect.GoToErrorDialog(
                        title = "",
                        message = effect.message
                    )
                    F.Browse,
                    F.Skip -> NavigationEffect.GoToSetPin(onboarding = true)
                    F.Buy -> NavigationEffect.GoToSetPin(
                        onboarding = true,
                        onComplete = OnCompleteAction.GO_TO_BUY
                    )
                    else -> null
                }
            })
        )

    override fun bindView(output: Consumer<E>): Disposable {
        view_pager.adapter = OnBoardingPageAdapter()
        view_pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                output.accept(E.OnPageChanged(position + 1))
            }
        })
        button_skip.setOnClickListener {
            output.accept(E.OnSkipClicked)
        }
        button_back.setOnClickListener {
            output.accept(E.OnBackClicked)
        }

        return Disposable {}
    }

    override fun M.render() {
        ifChanged(M::page) { page ->
            listOf(indicator1, indicator2)
                .forEachIndexed { index, indicator ->
                    indicator.background = when (page) {
                        index + 1 -> activeIndicator
                        else -> inactiveIndicator
                    }
                }
        }

        ifChanged(M::isFirstPage) { isFirstPage ->
            button_skip.isVisible = isFirstPage
            button_back.isVisible = isFirstPage
        }

        ifChanged(M::isLoading) { isLoading ->
            loading_view.isVisible = isLoading
            button_skip.isEnabled = !isLoading
        }
    }

    inner class OnBoardingPageAdapter : RouterPagerAdapter(this) {
        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val root = when (position) {
                    0 -> PageOneController()
                    // disabling page 2
                    // 1 -> PageTwoController()
                    1 -> PageThreeController()
                    else -> error("Unknown position")
                }
                router.setRoot(RouterTransaction.with(root))
            }
        }

        // override fun getCount(): Int = 3
        override fun getCount(): Int = 2
    }

    override fun handleBack() = currentModel.isLoading

    override fun onDestroy() {
        effectJob.cancel()
        super.onDestroy()
    }
}

class PageOneController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.controller_onboarding_page_one
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        primary_text.setText(R.string.OnboardingPageTwo_title)
        secondary_text.text = "Our wallet is open source and " +
            "fully compatible with any other wallet."
    }
}

class PageTwoController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.controller_onboarding_page
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        val resources = checkNotNull(resources)
        val theme = checkNotNull(activity).theme
        primary_text.setText("Buy BTC and withdraw $ in our American ATMs")
        secondary_text.setText("1000 ATM are already available and our network is keep growing ")
        // image_view.setImageDrawable(resources.getDrawable(R.drawable.atm_guy, theme))
    }
}

class PageThreeController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.controller_onboarding_page

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        val onBoardingController = (parentController as OnBoardingController)

        last_screen_title.isVisible = true
        button_buy.isVisible = false
        button_browse.isVisible = true
        primary_text.isVisible = false
        secondary_text.isVisible = false
        image_view.isVisible = false

        button_buy.setOnClickListener {
            onBoardingController.eventConsumer.accept(E.OnBuyClicked)
        }
        button_browse.setOnClickListener {
            onBoardingController.eventConsumer.accept(E.OnBrowseClicked)
        }
    }
}
