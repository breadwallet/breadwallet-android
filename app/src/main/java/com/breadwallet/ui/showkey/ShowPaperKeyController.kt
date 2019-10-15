/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 10/10/19.
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
package com.breadwallet.ui.showkey

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.breadwallet.R
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.util.DefaultOnPageChangeListener
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_paper_key.*
import kotlinx.android.synthetic.main.fragment_word_item.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class ShowPaperKeyController(args: Bundle) :
    BaseMobiusController<ShowPaperKeyModel, ShowPaperKeyEvent, ShowPaperKeyEffect>(args) {

    companion object {
        private const val EXTRA_PHRASE = "phrase"
        private const val EXTRA_ON_COMPLETE = "on-complete"
        private const val NAVIGATION_BUTTONS_WEIGHT = 1
        private const val BUTTONS_LAYOUT_WEIGHT_SUM_DEFAULT = 2.0f
        private const val BUTTONS_LAYOUT_WEIGHT_SUM_SINGLE = 1.0f
    }

    constructor(
        phrase: List<String>,
        onComplete: OnCompleteAction
    ) : this(bundleOf(EXTRA_PHRASE to phrase, EXTRA_ON_COMPLETE to onComplete.name))

    private val phrase: List<String> = arg(EXTRA_PHRASE)
    private val onComplete = OnCompleteAction.valueOf(arg(EXTRA_ON_COMPLETE))

    override val layoutId = R.layout.activity_paper_key
    override val defaultModel = ShowPaperKeyModel.createDefault(phrase, onComplete)
    override val update = ShowPaperKeyUpdate
    override val init = ShowPaperKeyInit
    override val effectHandler = CompositeEffectHandler.from<ShowPaperKeyEffect, ShowPaperKeyEvent>(
        nestedConnectable({ direct.instance<NavigationEffectHandler>() }, { effect ->
            when (effect) {
                ShowPaperKeyEffect.GoToBuy -> NavigationEffect.GoToBuy
                else -> null
            }
        }),
        nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
            when (effect) {
                ShowPaperKeyEffect.GoToHome -> NavigationEffect.GoToHome
                is ShowPaperKeyEffect.GoToPaperKeyProve -> NavigationEffect.GoToPaperKeyProve(
                    effect.phrase,
                    effect.onComplete
                )
                else -> null
            }
        })
    )

    override fun bindView(output: Consumer<ShowPaperKeyEvent>): Disposable {
        next_button.setOnClickListener {
            output.accept(ShowPaperKeyEvent.OnNextClicked)
        }
        previous_button.setOnClickListener {
            output.accept(ShowPaperKeyEvent.OnPreviousClicked)
        }
        close_button.setOnClickListener {
            output.accept(ShowPaperKeyEvent.OnCloseClicked)
        }
        words_pager.addOnPageChangeListener(object : DefaultOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                output.accept(ShowPaperKeyEvent.OnPageChanged(position))
            }
        })
        return Disposable { }
    }

    override fun ShowPaperKeyModel.render() {
        ifChanged(ShowPaperKeyModel::phrase) {
            words_pager.adapter = WordPagerAdapter(this@ShowPaperKeyController, phrase)
        }
        ifChanged(ShowPaperKeyModel::currentWord) {
            words_pager.currentItem = currentWord
            item_index.text = resources?.getString(
                R.string.WritePaperPhrase_step,
                currentWord + 1,
                phrase.size
            )
            updateButtons(currentWord > 0)
        }
    }

    /** Show or hide the "Previous" button used to navigate the ViewPager. */
    private fun updateButtons(showPrevious: Boolean) {
        val nextButtonParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        if (!showPrevious) {
            buttons_layout.weightSum = BUTTONS_LAYOUT_WEIGHT_SUM_SINGLE

            nextButtonParams.weight = NAVIGATION_BUTTONS_WEIGHT.toFloat()
            nextButtonParams.gravity = Gravity.CENTER_HORIZONTAL
            nextButtonParams.setMargins(
                resources!!.getDimension(R.dimen.margin).toInt(),
                0,
                resources!!.getDimension(R.dimen.margin).toInt(),
                0
            )
            next_button.layoutParams = nextButtonParams
            next_button.height = resources!!.getDimension(R.dimen.large_button_height).toInt()

            previous_button.visibility = View.GONE
        } else {
            buttons_layout.weightSum = BUTTONS_LAYOUT_WEIGHT_SUM_DEFAULT

            nextButtonParams.weight = NAVIGATION_BUTTONS_WEIGHT.toFloat()
            nextButtonParams.setMargins(0, 0, resources!!.getDimension(R.dimen.margin).toInt(), 0)
            next_button.layoutParams = nextButtonParams
            next_button.height = resources!!.getDimension(R.dimen.large_button_height).toInt()

            val previousButtonParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            previousButtonParams.weight = NAVIGATION_BUTTONS_WEIGHT.toFloat()
            previousButtonParams.setMargins(
                resources!!.getDimension(R.dimen.margin).toInt(),
                0,
                0,
                0
            )
            previous_button.layoutParams = previousButtonParams
            previous_button.visibility = View.VISIBLE
            previous_button.height = resources!!.getDimension(R.dimen.large_button_height).toInt()
        }
    }
}

class WordPagerAdapter(
    host: Controller,
    private val words: List<String>
) : RouterPagerAdapter(host) {
    override fun configureRouter(router: Router, position: Int) {
        router.replaceTopController(RouterTransaction.with(WordController(words[position])))
    }

    override fun getCount() = words.size
}

class WordController(args: Bundle? = null) : BaseController(args) {
    companion object {
        private const val EXT_WORD = "word"
    }

    constructor(word: String) : this(
        bundleOf(EXT_WORD to word)
    )

    override val layoutId = R.layout.fragment_word_item
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        word_button.text = arg(EXT_WORD)
    }
}