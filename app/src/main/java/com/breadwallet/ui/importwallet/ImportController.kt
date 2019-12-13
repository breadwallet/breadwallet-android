package com.breadwallet.ui.importwallet

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.tools.util.Link
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.importwallet.Import.M.LoadingState
import com.breadwallet.ui.scanner.ScannerController
import com.spotify.mobius.flow.subtypeEffectHandler
import kotlinx.android.synthetic.main.controller_import_wallet.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.singleton

private const val PRIVATE_KEY = "private_key"
private const val PASSWORD_PROTECTED = "password_protected"

private const val CONFIRM_IMPORT_DIALOG = "confirm_import"
private const val IMPORT_SUCCESS_DIALOG = "import_success"

@UseExperimental(ExperimentalCoroutinesApi::class)
class ImportController(
    args: Bundle? = null
) : BaseMobiusController<Import.M, Import.E, Import.F>(args),
    ScannerController.Listener,
    AlertDialogController.Listener,
    PasswordController.Listener {

    constructor(
        privateKey: String,
        isPasswordProtected: Boolean
    ) : this(
        bundleOf(
            PRIVATE_KEY to privateKey,
            PASSWORD_PROTECTED to isPasswordProtected
        )
    )

    override val layoutId = R.layout.controller_import_wallet

    override val init = ImportInit
    override val update = ImportUpdate
    override val defaultModel = Import.M.createDefault(
        privateKey = argOptional(PRIVATE_KEY),
        isPasswordProtected = arg(PASSWORD_PROTECTED, false)
    )

    override val kodein by Kodein.lazy {
        extend(super.kodein)

        bind<WalletImporter>() with singleton { WalletImporter() }
    }

    override val flowEffectHandler = subtypeEffectHandler<Import.F, Import.E> {
        addTransformer<Import.F.Nav>(handleNavEffects())
        addTransformer<Import.F.ValidateKey> { effects ->
            effects.handleValidateKey(direct.instance())
        }
        addTransformer<Import.F.EstimateImport> { effects ->
            effects.handleEstimateImport(direct.instance(), direct.instance())
        }
        addTransformer<Import.F.SubmitImport> { effects ->
            effects.handleSubmitTransfer(direct.instance())
        }

        addActionSync<Import.F.ShowNoWalletsEnabled>(Main) {
            router.popController(this@ImportController)
        }

        addConsumerSync<Import.F.ShowConfirmImport>(Main, ::showConfirmImport)

        addActionSync<Import.F.ShowNoBalance>(Main, ::showNoBalance)
        addActionSync<Import.F.ShowKeyInvalid>(Main, ::showKeyInvalid)
        addActionSync<Import.F.ShowPasswordInvalid>(Main, ::showPasswordInvalid)
        addActionSync<Import.F.ShowImportFailed>(Main, ::showImportFailed)
        addActionSync<Import.F.ShowPasswordInput>(Main, ::showPasswordInput)
        addActionSync<Import.F.ShowBalanceTooLow>(Main, ::showBalanceTooLow)
        addActionSync<Import.F.ShowImportSuccess>(Main, ::showImportSuccess)
    }

    override fun bindView(modelFlow: Flow<Import.M>): Flow<Import.E> {
        modelFlow
            .map { it.loadingState }
            .distinctUntilChanged()
            .onEach { state ->
                val isLoading = state != LoadingState.IDLE
                // Disable navigation
                scan_button.isEnabled = !isLoading
                faq_button.isEnabled = !isLoading
                close_button.isEnabled = !isLoading

                // Set loading visibility
                scan_button.isGone = isLoading
                progressBar.isVisible = isLoading
                label_import_status.isVisible = isLoading

                // Set loading message
                val messageId = when (state) {
                    LoadingState.ESTIMATING,
                    LoadingState.VALIDATING -> R.string.Import_checking
                    LoadingState.SUBMITTING -> R.string.Import_importing
                    else -> null
                }
                messageId?.let(label_import_status::setText)
            }
            .launchIn(uiBindScope)

        return merge(
            close_button.clicks().map { Import.E.OnCloseClicked },
            faq_button.clicks().map { Import.E.OnFaqClicked },
            scan_button.clicks().map { Import.E.OnScanClicked }
        )
    }

    override fun handleBack(): Boolean {
        return currentModel.isLoading
    }

    override fun onPasswordConfirmed(password: String) {
        Import.E.OnPasswordEntered(password)
            .run(eventConsumer::accept)
    }

    override fun onPasswordCancelled() {
        Import.E.OnImportCancel
            .run(eventConsumer::accept)
    }

    override fun onLinkScanned(link: Link) {
        if (link is Link.ImportWallet) {
            Import.E.OnKeyScanned(link.privateKey, link.passwordProtected)
                .run(eventConsumer::accept)
        }
    }

    override fun onPositiveClicked(dialogId: String, controller: AlertDialogController) {
        when (dialogId) {
            CONFIRM_IMPORT_DIALOG -> Import.E.OnImportConfirm
            IMPORT_SUCCESS_DIALOG -> Import.E.OnCloseClicked
            else -> null
        }?.run(eventConsumer::accept)
    }

    override fun onDismissed(dialogId: String, controller: AlertDialogController) {
        when (dialogId) {
            CONFIRM_IMPORT_DIALOG -> Import.E.OnImportCancel
            IMPORT_SUCCESS_DIALOG -> Import.E.OnCloseClicked
            else -> null
        }?.run(eventConsumer::accept)
    }

    private fun showKeyInvalid() {
        val res = checkNotNull(resources)
        val controller = AlertDialogController(
            message = res.getString(R.string.Import_Error_notValid),
            positiveText = res.getString(R.string.Button_ok)
        )
        router.pushController(RouterTransaction.with(controller))
    }

    private fun showPasswordInvalid() {
        val res = checkNotNull(resources)
        val controller = AlertDialogController(
            message = res.getString(R.string.Import_wrongPassword),
            positiveText = res.getString(R.string.Button_ok)
        )
        router.pushController(RouterTransaction.with(controller))
    }

    private fun showNoBalance() {
        val res = checkNotNull(resources)
        val controller = AlertDialogController(
            title = res.getString(R.string.Import_title),
            message = res.getString(R.string.Import_Error_empty),
            positiveText = res.getString(R.string.Button_ok)
        )
        router.pushController(RouterTransaction.with(controller))
    }

    private fun showBalanceTooLow() {
        val res = checkNotNull(resources)
        val controller = AlertDialogController(
            title = res.getString(R.string.Import_title),
            message = res.getString(R.string.Import_Error_highFees),
            positiveText = res.getString(R.string.Button_ok)
        )
        router.pushController(RouterTransaction.with(controller))
    }

    private fun showConfirmImport(effect: Import.F.ShowConfirmImport) {
        val res = checkNotNull(resources)
        val controller = AlertDialogController(
            title = res.getString(R.string.Import_title),
            message = res.getString(
                R.string.Import_confirm,
                effect.receiveAmount,
                effect.feeAmount
            ),
            positiveText = res.getString(R.string.Import_importButton),
            dialogId = CONFIRM_IMPORT_DIALOG
        )
        controller.targetController = this@ImportController
        router.pushController(RouterTransaction.with(controller))
    }

    private fun showImportSuccess() {
        val res = checkNotNull(resources)
        val controller = AlertDialogController(
            title = res.getString(R.string.Import_success),
            message = res.getString(R.string.Import_SuccessBody),
            positiveText = res.getString(R.string.Button_ok),
            dialogId = IMPORT_SUCCESS_DIALOG
        )
        controller.targetController = this@ImportController
        router.pushController(RouterTransaction.with(controller))
    }

    private fun showImportFailed() {
        val res = checkNotNull(resources)
        val controller = AlertDialogController(
            title = res.getString(R.string.Import_title),
            message = res.getString(R.string.Import_Error_signing),
            positiveText = res.getString(R.string.Button_ok)
        )
        router.pushController(RouterTransaction.with(controller))
    }

    private fun showPasswordInput() {
        val controller = PasswordController()
        controller.targetController = this@ImportController
        router.pushController(RouterTransaction.with(controller))
    }
}
