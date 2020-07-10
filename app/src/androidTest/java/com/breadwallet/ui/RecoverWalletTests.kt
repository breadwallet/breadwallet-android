package com.breadwallet.ui

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen.Companion.onScreen
import com.breadwallet.R
import com.breadwallet.util.BaseTestCase
import com.breadwallet.util.InputPinScreen
import com.breadwallet.util.KIntroRecoveryScreen
import com.breadwallet.util.KIntroScreen
import com.breadwallet.util.KRecoveryKeyScreen
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@LargeTest
@RunWith(Theories::class)
class RecoverWalletTests : BaseTestCase() {

    companion object {
        @JvmField
        @DataPoints
        val phrases = arrayOf(
            "blind forum sunset width come shrug hamster drop connect ridge expire clump",
            "ausente fobia sodio vinagre célula reparto guion dibujo certeza potencia espuma casero",
            "うもう ざつがく へいねつ るすばん かんけい はんこ しやくしょ げすと きいろ ねまわし こぜん かぶか",
            "仰 隐 着 欧 柬 销 还 井 代 钻 震 说",
            "駐 愈 役 亡 乎 拌 惜 場 不 障 麼 紡",
            "badge étanche séjour vigueur chance rasage flatteur déranger chéquier pinceau éligible cellule",
            "solubile pupazzo smottato lineare vimini vicinanza soffitto premere rintocco sogno pavone brodo",
            "분필 직업 비바람 미팅 경력 애인 형수 대규모 장점 실패 사냥 미역"
        )
    }

    @Theory
    fun testRecovery(phrase: String) {
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
                }
            }

            step("Confirm recovery is successful") {
                onScreen<InputPinScreen> {
                    flakySafely(timeoutMs = 15_000L, intervalMs = 100L) {
                        title.hasText(R.string.UpdatePin_createTitle)
                    }
                }
            }
        }
    }
}