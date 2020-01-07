package com.breadwallet.ui.settings.currency

import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect

object DisplayCurrency {

    data class M(val currencies: List<FiatCurrency>, val selectedCurrency: String) {
        companion object {
            fun createDefault(): M = M(emptyList(), "")
        }
    }

    sealed class E {
        object OnBackClicked : E()
        object OnFaqClicked : E()
        data class OnCurrencySelected(val currencyCode: String) : E()
        data class OnCurrenciesLoaded(
            val selectedCurrencyCode: String,
            val currencies: List<FiatCurrency>
        ) : E()

        data class OnSelectedCurrencyUpdated(val currencyCode: String) : E()
    }

    sealed class F {
        object LoadCurrencies : F()
        data class SetDisplayCurrency(val currencyCode: String) : F()
        sealed class Nav : F(), NavEffectHolder {
            object GoBack : Nav() {
                override val navigationEffect = NavigationEffect.GoBack
            }

            object GoToFaq : Nav() {
                override val navigationEffect =
                    NavigationEffect.GoToFaq(BRConstants.FAQ_DISPLAY_CURRENCY)
            }
        }
    }
}