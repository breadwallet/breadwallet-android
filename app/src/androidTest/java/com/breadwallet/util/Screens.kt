package com.breadwallet.util

import android.view.View
import com.agoda.kakao.common.views.KView
import com.agoda.kakao.edit.KEditText
import com.agoda.kakao.image.KImageView
import com.agoda.kakao.pager.KViewPager
import com.agoda.kakao.progress.KProgressBar
import com.agoda.kakao.recycler.KRecyclerItem
import com.agoda.kakao.recycler.KRecyclerView
import com.agoda.kakao.screen.Screen
import com.agoda.kakao.switch.KSwitch
import com.agoda.kakao.text.KButton
import com.agoda.kakao.text.KTextView
import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.settings.SettingsController
import com.breadwallet.ui.settings.fastsync.FastSyncController
import com.breadwallet.uiview.KeyboardView
import com.kaspersky.components.kautomator.component.common.views.UiView
import com.kaspersky.components.kautomator.component.edit.UiEditText
import com.kaspersky.components.kautomator.component.text.UiButton
import com.kaspersky.components.kautomator.component.text.UiTextView
import com.kaspersky.components.kautomator.screen.UiScreen
import com.kaspersky.kaspresso.screens.KScreen
import org.hamcrest.Matcher

class KIntroScreen : Screen<KIntroScreen>() {

    init {
        rootView = KView {
            isCompletelyDisplayed()
            withId(R.id.intro_layout)
        }
    }

    val getStarted = KButton {
        isCompletelyDisplayed()
        withId(R.id.button_new_wallet)
    }

    val recover = KButton {
        isCompletelyDisplayed()
        withId(R.id.button_recover_wallet)
    }
}

class OnBoardingScreen : Screen<OnBoardingScreen>() {
    init {
        rootView = KView {
            isCompletelyDisplayed()
            withId(R.id.layoutOnboarding)
        }
    }

    val skip = KButton {
        isDisplayed()
        withId(R.id.button_skip)
    }
    val pager = KViewPager {
        isDisplayed()
        withId(R.id.view_pager)
    }
    val primaryText = KTextView {
        isVisible()
        isCompletelyDisplayed()
        withId(R.id.primary_text)
    }
    val secondaryText = KTextView {
        isVisible()
        isCompletelyDisplayed()
        withId(R.id.secondary_text)
    }
    val lastScreenText = KTextView {
        isVisible()
        isCompletelyDisplayed()
        withId(R.id.last_screen_title)
    }

    val buy = KButton {
        isVisible()
        isDisplayed()
        withId(R.id.button_buy)
    }

    val browse = KButton {
        isVisible()
        isDisplayed()
        withId(R.id.button_browse)
    }

    val loading = KView {
        withId(R.id.loading_view)
    }
}

class InputPinScreen : Screen<InputPinScreen>() {

    init {
        rootView = KView {
            isCompletelyDisplayed()
            withId(R.id.layoutSetPin)
        }
    }

    val title = KTextView {
        isDisplayed()
        withId(R.id.title)
    }

    val keyboard = KeyboardView(R.id.layoutSetPin) {
        isCompletelyDisplayed()
        withId(R.id.brkeyboard)
    }
}

class KWriteDownScreen : Screen<KWriteDownScreen>() {

    init {
        rootView = KView {
            withId(R.id.activity_write_down)
        }
    }

    val close = KButton {
        withId(R.id.close_button)
    }

    val writeDownKey = KButton {
        withId(R.id.button_write_down)
    }
}

class WebScreen : Screen<WebScreen>() {
    init {
        rootView = KView {
            withId(R.id.layoutWebController)
        }
    }
}

object ShowPaperKeyScreen : UiScreen<ShowPaperKeyScreen>() {
    override val packageName: String = BuildConfig.APPLICATION_ID

    val next = UiButton {
        withId(this@ShowPaperKeyScreen.packageName, "next_button")
    }

    val word = UiView {
        withId(this@ShowPaperKeyScreen.packageName, "word_button")
    }
}

object ProvePaperKeyScreen : UiScreen<ProvePaperKeyScreen>() {
    override val packageName: String = BuildConfig.APPLICATION_ID

    val firstWordLabel = UiTextView {
        withId(this@ProvePaperKeyScreen.packageName, "first_word_label")
    }

    val lastWordLabel = UiTextView {
        withId(this@ProvePaperKeyScreen.packageName, "last_word_label")
    }

    val firstWord = UiEditText {
        withId(this@ProvePaperKeyScreen.packageName, "first_word")
    }

    val secondWord = UiEditText {
        withId(this@ProvePaperKeyScreen.packageName, "second_word")
    }
}

class KHomeScreen : Screen<KHomeScreen>() {
    init {
        rootView = KView {
            withId(R.id.layoutHome)
        }
    }

    val totalAssets = KTextView {
        isCompletelyDisplayed()
        withId(R.id.total_assets_usd)
    }

    val menu = KView {
        isCompletelyDisplayed()
        withId(R.id.menu_layout)
    }

    val wallets = KRecyclerView({
        withId(R.id.rv_wallet_list)
    }, itemTypeBuilder = {
        itemType(::KWalletItem)
    })

    class KWalletItem(parent: Matcher<View>) : KRecyclerItem<KWalletItem>(parent) {
        val name = KTextView(parent) { withId(R.id.wallet_name) }
        val progress = KProgressBar(parent) { withId(R.id.sync_progress) }
    }
}

object KSettingsScreen : KScreen<KSettingsScreen>() {
    override val layoutId: Int = R.layout.controller_settings

    override val viewClass: Class<*> = SettingsController::class.java

    val back = KButton {
        isCompletelyDisplayed()
        withId(R.id.back_button)
    }

    val close = KButton {
        isCompletelyDisplayed()
        withId(R.id.close_button)
    }

    val recycler = KRecyclerView({
        isCompletelyDisplayed()
        withId(R.id.settings_list)
    }, itemTypeBuilder = {
        itemType(::KSettingsItem)
    })

    class KSettingsItem(parent: Matcher<View>) : KRecyclerItem<KSettingsItem>(parent) {
        val title = KTextView(parent) { withId(R.id.item_title) }
        val addon = KTextView(parent) { withId(R.id.item_addon) }
        val subHeader = KTextView(parent) { withId(R.id.item_sub_header) }
        val icon = KImageView(parent) { withId(R.id.setting_icon) }
    }
}

object KFastSyncScreen : KScreen<KFastSyncScreen>() {
    override val layoutId: Int = R.layout.controller_fast_sync

    override val viewClass: Class<*> = FastSyncController::class.java

    val switch = KSwitch {
        isCompletelyDisplayed()
        withId(R.id.switch_fast_sync)
    }

    val back = KButton {
        isCompletelyDisplayed()
        withId(R.id.back_btn)
    }
}

object KDialogScreen : KScreen<KDialogScreen>() {
    override val layoutId: Int = R.layout.controller_alert_dialog

    override val viewClass: Class<*> = AlertDialogController::class.java

    val positive = KButton {
        isCompletelyDisplayed()
        withId(R.id.pos_button)
    }

    val negative = KButton {
        isCompletelyDisplayed()
        withId(R.id.neg_button)
    }
}

class KWalletScreen : Screen<KWalletScreen>() {
    init {
        rootView = KView {
            withId(R.id.layoutWalletScreen)
        }
    }

    val send = KButton {
        isDisplayed()
        withId(R.id.send_button)
    }

    val receive = KButton {
        isDisplayed()
        withId(R.id.receive_button)
    }

    val transactions = KRecyclerView({
        isDisplayed()
        withId(R.id.tx_list)
    }, itemTypeBuilder = {
        itemType(::KTransactionItem)
    })

    class KTransactionItem(parent: Matcher<View>) : KRecyclerItem<KTransactionItem>(parent)
}

class KIntroRecoveryScreen : Screen<KIntroRecoveryScreen>() {
    init {
        rootView = KView {
            withId(R.id.layoutRecoverIntro)
        }
    }

    val next = KButton {
        isClickable()
        isCompletelyDisplayed()
        withId(R.id.send_button)
    }
}

class KRecoveryKeyScreen : Screen<KRecoveryKeyScreen>() {
    init {
        rootView = KView {
            withId(R.id.layoutRecoverWallet)
        }
    }

    val next = KButton { withId(R.id.send_button) }
    val loading = KView { withId(R.id.loading_view) }

    fun enterPhrase(phrase: String) {
        val words = phrase.split(" ")
        word1.replaceText(words[0])
        word2.replaceText(words[1])
        word3.replaceText(words[2])
        word4.replaceText(words[3])
        word5.replaceText(words[4])
        word6.replaceText(words[5])
        word7.replaceText(words[6])
        word8.replaceText(words[7])
        word9.replaceText(words[8])
        word10.replaceText(words[9])
        word11.replaceText(words[10])
        word12.replaceText(words[11])
    }

    val word1 = KEditText {
        isCompletelyDisplayed()
        withId(R.id.word1)
    }
    val word2 = KEditText { withId(R.id.word2) }
    val word3 = KEditText { withId(R.id.word3) }
    val word4 = KEditText { withId(R.id.word4) }
    val word5 = KEditText { withId(R.id.word5) }
    val word6 = KEditText { withId(R.id.word6) }
    val word7 = KEditText { withId(R.id.word7) }
    val word8 = KEditText { withId(R.id.word8) }
    val word9 = KEditText { withId(R.id.word9) }
    val word10 = KEditText { withId(R.id.word10) }
    val word11 = KEditText { withId(R.id.word11) }
    val word12 = KEditText { withId(R.id.word12) }
}

class KSendScreen : Screen<KSendScreen>() {
    init {
        rootView = KView {
            withId(R.id.layoutSendSheet)
        }
    }

    val paste = KButton {
        isCompletelyDisplayed()
        withId(R.id.buttonPaste)
    }

    val amount = KEditText {
        isCompletelyDisplayed()
        withId(R.id.textInputAmount)
    }

    val keyboard = KeyboardView(R.id.layoutSendSheet) {
        isCompletelyDisplayed()
        withId(R.id.keyboard)
    }

    val send = KButton {
        isCompletelyDisplayed()
        withId(R.id.buttonSend)
    }
}

class KSignalScreen : Screen<KSignalScreen>() {
    init {
        rootView = KView {
            withId(R.id.layoutSignal)
        }
    }

    val title = KTextView {
        withId(R.id.title)
    }
}

class KTxDetailsScreen : Screen<KTxDetailsScreen>() {

    init {
        rootView = KView {
            withId(R.id.layoutTransactionDetails)
        }
    }

    val action = KTextView {
        isDisplayed()
        withId(R.id.tx_action)
    }
}

class KConfirmationScreen : Screen<KConfirmationScreen>() {
    init {
        rootView = KView {
            withId(R.id.layoutBackground)
        }
    }

    val send = KTextView {
        isCompletelyDisplayed()
        withId(R.id.ok_btn)
    }

    val cancel = KTextView {
        isCompletelyDisplayed()
        withId(R.id.cancel_btn)
    }

    val amountToSend = KTextView {
        withId(R.id.amount_value)
    }
}

class KPinAuthScreen : Screen<KPinAuthScreen>() {
    init {
        rootView = KView {
            withId(R.id.activity_pin)
        }
    }

    val title = KTextView {
        withParent { withId(R.id.pin_dialog) }
        isDisplayed()
        withId(R.id.title)
    }

    val keyboard = KeyboardView(R.id.activity_pin) {
        withId(R.id.brkeyboard)
    }
}
