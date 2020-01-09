package com.breadwallet.ui.settings.fastsync

import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.effecthandler.metadata.MetaDataEffect
import com.breadwallet.effecthandler.metadata.MetaDataEffectHandler
import com.breadwallet.effecthandler.metadata.MetaDataEvent
import com.breadwallet.model.SyncMode
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.flowbind.checked
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.breadwallet.util.CurrencyCode
import com.breadwallet.util.isBitcoin
import com.spotify.mobius.Connectable
import com.spotify.mobius.flow.subtypeEffectHandler
import com.spotify.mobius.flow.transform
import kotlinx.android.synthetic.main.controller_fast_sync.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.util.Locale

private const val CURRENCY_CODE = "currency_code"

@UseExperimental(ExperimentalCoroutinesApi::class)
class FastSyncController(
    args: Bundle
) : BaseMobiusController<FastSyncModel, FastSyncEvent, FastSyncEffect>(args),
    AlertDialogController.Listener {

    constructor(currencyCode: CurrencyCode) : this(
        bundleOf(CURRENCY_CODE to currencyCode)
    )

    override val layoutId = R.layout.controller_fast_sync
    override val defaultModel = FastSyncModel.createDefault(arg(CURRENCY_CODE))
    override val init = FastSyncInit
    override val update = FastSyncUpdate
    override val flowEffectHandler
        get() = subtypeEffectHandler<FastSyncEffect, FastSyncEvent> {
            addTransformer<FastSyncEffect.Nav>(direct.instance<NavEffectTransformer>())
            addActionSync<FastSyncEffect.ShowDisableFastSyncDialog>(
                Main,
                ::showDisableFastSyncDialog
            )
            addFunctionSync<FastSyncEffect.LoadCurrencyIds>(Default) {
                FastSyncEvent.OnCurrencyIdsUpdated(
                    TokenUtil.getTokenItems(applicationContext)
                        .associateBy { it.symbol.toLowerCase(Locale.ROOT) }
                        .mapValues { it.value.currencyId ?: "" }
                )
            }
            addTransformer<FastSyncEffect.MetaData> { effects ->
                effects
                    .map { effect ->
                        when (effect) {
                            is FastSyncEffect.MetaData.SetSyncMode ->
                                MetaDataEffect.UpdateWalletMode(
                                    effect.currencyId,
                                    effect.mode.walletManagerMode
                                )
                            is FastSyncEffect.MetaData.LoadSyncModes ->
                                MetaDataEffect.LoadWalletModes
                        }
                    }
                    .transform(Connectable<MetaDataEffect, MetaDataEvent> { consumer ->
                        MetaDataEffectHandler(consumer, direct.instance(), direct.instance())
                    })
                    .mapNotNull { event ->
                        when (event) {
                            is MetaDataEvent.OnWalletModesUpdated ->
                                FastSyncEvent.OnSyncModesUpdated(
                                    event.modeMap
                                        .filterKeys { currencyId -> currencyId.isNotBlank() }
                                        .mapValues { entry ->
                                            SyncMode.fromWalletManagerMode(entry.value)
                                        }
                                )
                            else -> null
                        }
                    }
            }

        }

    override fun bindView(modelFlow: Flow<FastSyncModel>): Flow<FastSyncEvent> {
        modelFlow
            .mapLatest { it.fastSyncEnable }
            .distinctUntilChanged()
            .onEach { isChecked ->
                switch_fast_sync.isChecked = isChecked
            }
            .launchIn(uiBindScope)
        return merge(
            back_btn.clicks().map { FastSyncEvent.OnBackClicked },
            switch_fast_sync.checked().map { FastSyncEvent.OnFastSyncChanged(it) },
            bindLearnMoreLink()
        )
    }

    override fun onPositiveClicked(dialogId: String, controller: AlertDialogController) {
        eventConsumer.accept(FastSyncEvent.OnDisableFastSyncConfirmed)
    }

    override fun onNegativeClicked(dialogId: String, controller: AlertDialogController) {
        eventConsumer.accept(FastSyncEvent.OnDisableFastSyncCanceled)
    }

    override fun onDismissed(dialogId: String, controller: AlertDialogController) {
        eventConsumer.accept(FastSyncEvent.OnDisableFastSyncCanceled)
    }

    private fun showDisableFastSyncDialog() {
        val act = checkNotNull(activity)
        val controller = AlertDialogController(
            message = act.getString(R.string.FastSync_disableConfirmationDialog),
            positiveText = act.getString(R.string.FastSync_turnOff),
            negativeText = act.getString(R.string.Button_cancel)
        )
        controller.targetController = this
        router.pushController(RouterTransaction.with(controller))
    }

    private fun bindLearnMoreLink() = callbackFlow<FastSyncEvent> {
        val act = checkNotNull(activity)
        val message: String = act.getString(R.string.FastSync_description)
        val clickableText = act.getString(R.string.FastSync_learnMoreLink)
        val linkPos = message.lastIndexOf(clickableText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                offer(FastSyncEvent.OnLearnMoreClicked)
            }
        }
        description.text = SpannableString(message).apply {
            setSpan(
                clickableSpan,
                linkPos,
                linkPos + clickableText.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        description.movementMethod = LinkMovementMethod.getInstance()
        awaitClose()
    }
}
