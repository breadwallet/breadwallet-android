package com.breadwallet.ui

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.agoda.kakao.screen.Screen.Companion.onScreen
import com.breadwallet.R
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.util.APPLICATION_PIN
import com.breadwallet.util.BaseTestCase
import com.breadwallet.util.KConfirmationScreen
import com.breadwallet.util.KHomeScreen
import com.breadwallet.util.KHomeScreen.KWalletItem
import com.breadwallet.util.KIntroRecoveryScreen
import com.breadwallet.util.KIntroScreen
import com.breadwallet.util.KPinAuthScreen
import com.breadwallet.util.KRecoveryKeyScreen
import com.breadwallet.util.KSendScreen
import com.breadwallet.util.KSignalScreen
import com.breadwallet.util.KTxDetailsScreen
import com.breadwallet.util.KWalletScreen
import com.breadwallet.util.KWalletScreen.KTransactionItem
import com.breadwallet.util.setApplicationPin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

private const val PHRASE1 = "evolve total dial kidney select proof library cream rocket tortoise add young"
private const val PHRASE1_ADDRESS = "n12EiHcv31us8xtehaBUxApcb4e6U77KJm"

private const val PHRASE2 = "under chief october surface cause ivory visa wreck fall caution taxi genius"
private const val PHRASE2_ADDRESS = "n1Jef1PheBHaZ5yLF45uhEFp56B7GWKz3B"

private const val PHRASE3 = "soldier west guide drum estate inflict love embody year poem pluck train"
private const val PHRASE3_ADDRESS = "mxzt7QhmixUPvs7VcPLxYmTzKwK4QszBLA"

private const val PHRASE4 = "grunt kick suggest cycle permit enhance crumble master cactus coil orient wash"
private const val PHRASE4_ADDRESS = "mmec58RVYpqvXBFJLLPi2rt3y5UM4uy1Pz"

@LargeTest
@RunWith(Theories::class)
@Ignore("Not ready for continuous use.")
class SendTransactionTest : BaseTestCase() {

    companion object {
        @JvmStatic
        @DataPoint
        fun wallet() = listOf(
            PHRASE1 to PHRASE1_ADDRESS,
            PHRASE2 to PHRASE2_ADDRESS,
            PHRASE3 to PHRASE3_ADDRESS,
            PHRASE4 to PHRASE4_ADDRESS
        ).run {
            val source = random()
            val target = filter { it != source }.random()
            source.first to target.second
        }
    }

    @Theory
    fun testBtcFastsyncSend(wallet: Pair<String, String>) {
        println("Running theory with source '${wallet.first}' and target '${wallet.second}'")
        val (phrase, address) = wallet
        return before {
            launchActivity()
        }.after { clearData() }.run {
            step("Open Recovery") {
                onScreen<KIntroScreen> {
                    recover { click() }
                }
            }

            step("Go to phrase input") {
                onScreen<KIntroRecoveryScreen> {
                    next.click()
                }
            }

            step("Enter wallet phrase") {
                onScreen<KRecoveryKeyScreen> {
                    enterPhrase(phrase)
                    next.click()
                    loading.isVisible()
                }
            }

            flakySafely {
                setApplicationPin()
            }

            step("Open Bitcoin Wallet") {
                onScreen<KHomeScreen> {
                    wallets {
                        childWith<KWalletItem> {
                            withDescendant {
                                withText("Bitcoin")
                            }
                        } perform {
                            flakySafely(15_000L, 100L) {
                                progress.isNotDisplayed()
                            }
                            click()
                        }
                    }
                }
            }

            step("Open Send Sheet") {
                onScreen<KWalletScreen> {
                    send.click()
                }
            }

            step("Send some bitcoin") {
                onScreen<KSendScreen> {
                    val context = InstrumentationRegistry.getInstrumentation().targetContext
                    GlobalScope.launch(Dispatchers.Main) {
                        BRClipboardManager.putClipboard(address)
                    }

                    paste.click()
                    amount.click()
                    keyboard.input("0.000001")
                    send.click()
                }
            }

            step("Confirm Transaction") {
                onScreen<KConfirmationScreen> {
                    send.click()
                }
            }

            step("Authorize Transaction") {
                onScreen<KPinAuthScreen> {
                    title.hasText(R.string.VerifyPin_touchIdMessage)
                    keyboard.input(APPLICATION_PIN)
                }
            }

            step("Wait for confirmation") {
                flakySafely(5_000L, 50L) {
                    onScreen<KSignalScreen> {
                        title.hasText(R.string.Alerts_sendSuccess)
                        flakySafely {
                            rootView!!.doesNotExist()
                        }
                    }
                }
            }

            step("Open new transaction") {
                onScreen<KWalletScreen> {
                    transactions {
                        firstChild<KTransactionItem> {
                            isCompletelyDisplayed()
                            click()
                        }
                    }
                }
            }

            step("View transaction details") {
                onScreen<KTxDetailsScreen> {
                    action.hasText(R.string.TransactionDetails_titleSent)
                }
            }
        }
    }
}
