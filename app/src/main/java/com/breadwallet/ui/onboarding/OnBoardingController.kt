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

import android.content.Context
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.View
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.breadwallet.R
import com.breadwallet.legacy.presenter.activities.util.BRActivity
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_on_boarding.*
import kotlinx.android.synthetic.main.fragment_onboarding.*
import kotlinx.coroutines.SupervisorJob
import org.kodein.di.direct
import org.kodein.di.erased.instance

class OnBoardingController(
    args: Bundle? = null
) : BaseMobiusController<OnBoardingModel, OnBoardingEvent, OnBoardingEffect>(args) {

    private val activeIndicator by lazy {
        val resId = R.drawable.page_indicator_active
        checkNotNull(resources).getDrawable(resId, checkNotNull(activity).theme)
    }
    private val inactiveIndicator by lazy {
        val resId = R.drawable.page_indicator_inactive
        checkNotNull(resources).getDrawable(resId, checkNotNull(activity).theme)
    }

    override val layoutId = R.layout.activity_on_boarding

    private val effectJob = SupervisorJob()
    private val _effectHandler by lazy {
        OnBoardingEffectHandler(
            effectJob,
            direct.instance(),
            direct.instance(),
            { eventConsumer },
            { router },
            { activity as Context })
    }

    override val defaultModel = OnBoardingModel.DEFAULT
    override val init = OnBoardingInit
    override val update = OnBoardingUpdate
    override val effectHandler: Connectable<OnBoardingEffect, OnBoardingEvent> =
        CompositeEffectHandler.from(
            Connectable { _effectHandler },
            nestedConnectable({ NavigationEffectHandler(activity as BRActivity) }, { effect ->
                when (effect) {
                    is OnBoardingEffect.ShowError -> NavigationEffect.GoToErrorDialog(
                        title = "",
                        message = effect.message
                    )
                    else -> null
                }
            }),
            nestedConnectable({ RouterNavigationEffectHandler(router) }, { effect ->
                when (effect) {
                    OnBoardingEffect.Browse,
                    OnBoardingEffect.Skip -> NavigationEffect.GoToSetPin(onboarding = true)
                    OnBoardingEffect.Buy -> NavigationEffect.GoToSetPin(
                        onboarding = true,
                        onComplete = OnCompleteAction.GO_TO_BUY
                    )
                    else -> null
                }
            })
        )

    override fun bindView(output: Consumer<OnBoardingEvent>): Disposable {
        view_pager.adapter = OnBoardingPageAdapter()
        view_pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                output.accept(OnBoardingEvent.OnPageChanged(position + 1))
            }
        })
        button_skip.setOnClickListener {
            output.accept(OnBoardingEvent.OnSkipClicked)
        }
        button_back.setOnClickListener {
            output.accept(OnBoardingEvent.OnBackClicked)
        }

        return Disposable {}
    }

    override fun OnBoardingModel.render() {
        ifChanged(OnBoardingModel::page) { page ->
            listOf(indicator1, indicator2, indicator3)
                .forEachIndexed { index, indicator ->
                    indicator.background = when (page) {
                        index + 1 -> activeIndicator
                        else -> inactiveIndicator
                    }
                }
        }

        ifChanged(OnBoardingModel::isFirstPage) { isFirstPage ->
            button_skip.isVisible = isFirstPage
            button_back.isVisible = isFirstPage
        }

        ifChanged(OnBoardingModel::isLoading) { isLoading ->
            loading_view.isVisible = isLoading
            button_skip.isEnabled = !isLoading
        }
    }

    inner class OnBoardingPageAdapter : RouterPagerAdapter(this) {
        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val root = when (position) {
                    0 -> PageOneController()
                    1 -> PageTwoController()
                    2 -> PageThreeController()
                    else -> error("Unknown position")
                }
                router.setRoot(RouterTransaction.with(root))
            }
        }

        override fun getCount(): Int = 3
    }

    override fun handleBack() = currentModel.isLoading

    override fun onDestroy() {
        effectJob.cancel()
        super.onDestroy()
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
        val resources = checkNotNull(resources)
        val theme = checkNotNull(activity).theme
        primary_text.setText(R.string.OnboardingPageThree_title)
        secondary_text.setText(R.string.OnboardingPageThree_subtitle)
        image_view.setImageDrawable(resources.getDrawable(R.drawable.ic_currencies, theme))
    }
}

class PageThreeController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.fragment_onboarding

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        val onBoardingController = (parentController as OnBoardingController)

        last_screen_title.isVisible = true
        button_buy.isVisible = true
        button_browse.isVisible = true
        primary_text.isVisible = false
        secondary_text.isVisible = false
        image_view.isVisible = false
        button_buy.setOnClickListener {
            onBoardingController.eventConsumer.accept(OnBoardingEvent.OnBuyClicked)
        }
        button_browse.setOnClickListener {
            onBoardingController.eventConsumer.accept(OnBoardingEvent.OnBrowseClicked)
        }
    }
}
