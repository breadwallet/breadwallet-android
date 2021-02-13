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
import android.view.View
import androidx.core.view.isVisible
import androidx.viewpager.widget.ViewPager
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.onboarding.OnBoarding.E
import com.breadwallet.ui.onboarding.OnBoarding.F
import com.breadwallet.ui.onboarding.OnBoarding.M
import kotlinx.android.synthetic.main.controller_on_boarding.*
import kotlinx.android.synthetic.main.controller_onboarding_page.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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

    override val defaultModel = M.DEFAULT
    override val init = OnBoardingInit
    override val update = OnBoardingUpdate

    override val flowEffectHandler
        get() = createOnBoardingHandler(direct.instance())

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        view_pager.adapter = OnBoardingPageAdapter()
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return merge(
            button_skip.clicks().map { E.OnSkipClicked },
            button_back.clicks().map { E.OnBackClicked },
            callbackFlow<E.OnPageChanged> {
                val channel = channel
                val listener = object : ViewPager.SimpleOnPageChangeListener() {
                    override fun onPageSelected(position: Int) {
                        channel.offer(E.OnPageChanged(position + 1))
                    }
                }
                view_pager.addOnPageChangeListener(listener)
                awaitClose {
                    view_pager.removeOnPageChangeListener(listener)
                }
            }
        )
    }

    override fun M.render() {
        ifChanged(M::page) { page ->
            listOf(indicator1, indicator2, indicator3)
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
}

class PageOneController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.controller_onboarding_page
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        primary_text.setText(R.string.OnboardingPageTwo_title)
        secondary_text.setText(R.string.OnboardingPageTwo_subtitle)
    }
}

class PageTwoController(args: Bundle? = null) : BaseController(args) {
    override val layoutId = R.layout.controller_onboarding_page
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
    override val layoutId = R.layout.controller_onboarding_page

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
            onBoardingController.eventConsumer.accept(E.OnBuyClicked)
        }
        button_browse.setOnClickListener {
            onBoardingController.eventConsumer.accept(E.OnBrowseClicked)
        }
    }
}
