package com.breadwallet.ui.util

import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.wallet.configs.WalletUiConfiguration
import java.math.BigDecimal

// TODO: Collecting wallet display methods from Wallet Managers here for now -- class organization (whether hierarchy or not) is TBD, once we figure out what's needed
class WalletDisplayUtils {
    companion object {
        private const val MAX_DECIMAL_PLACES_FOR_UI = 5
        private const val ETHER_WEI = "1000000000000000000"
        private val ONE_ETH = BigDecimal(ETHER_WEI)
        private const val SCALE_ETH = 8

        fun getUIConfiguration(currencyCode : CurrencyCode) : WalletUiConfiguration? {
            return when {
                currencyCode.isBitcoin() -> WalletUiConfiguration("#f29500", null, true, MAX_DECIMAL_PLACES_FOR_UI)
                currencyCode.isBitcash() -> WalletUiConfiguration("#478559", null, true, MAX_DECIMAL_PLACES_FOR_UI)
                currencyCode.isEth() -> WalletUiConfiguration("#5e6fa5", null, true, MAX_DECIMAL_PLACES_FOR_UI)
                currencyCode.isBrd() -> WalletUiConfiguration("#5e6fa5", null, true, MAX_DECIMAL_PLACES_FOR_UI) // TODO: Revert to above once Token File issue is resolved
                else -> null
                //else -> WalletUiConfiguration(TokenUtil.getTokenStartColor(currencyCode), TokenUtil.getTokenEndColor(currencyCode), false, MAX_DECIMAL_PLACES_FOR_UI)
            }
        }

        // TODO: I think this should come from model (originating in wallet effect handler)
        fun getCryptoForSmallestCrypto(currencyCode: CurrencyCode, amountInSmallestUnit: BigDecimal) : BigDecimal? {
            return when {
                amountInSmallestUnit == BigDecimal.ZERO -> amountInSmallestUnit
                currencyCode.isBitcoin() || currencyCode.isBitcash() -> {
                    return when (BRSharedPrefs.getCryptoDenomination(null, currencyCode)) {
                        BRConstants.CURRENT_UNIT_BITS -> amountInSmallestUnit.divide(BigDecimal("100"), 2, BRConstants.ROUNDING_MODE)
                        BRConstants.CURRENT_UNIT_MBITS -> amountInSmallestUnit.divide(BigDecimal("100000"), 5, BRConstants.ROUNDING_MODE)
                        BRConstants.CURRENT_UNIT_BITCOINS -> amountInSmallestUnit.divide(BigDecimal("100000000"), 8, BRConstants.ROUNDING_MODE)
                        else -> null
                    }
                }
                currencyCode.isEth() -> amountInSmallestUnit.divide(ONE_ETH, SCALE_ETH, BRConstants.ROUNDING_MODE)
                currencyCode.isBrd() -> amountInSmallestUnit.divide(BigDecimal.TEN.pow(5), 5, BRConstants.ROUNDING_MODE)
                //else -> amountInSmallestUnit.divide(BigDecimal(BigDecimal.TEN.pow(mWalletToken.getToken().getDecimals()).toPlainString()), mWalletToken.getToken().getDecimals(), BRConstants.ROUNDING_MODE)
                else -> null
            }
        }

        // TODO: I think this should come from model (originating in wallet effect handler)
        fun getMaxDecimalPlaces(currencyCode: CurrencyCode) : Int {
            return MAX_DECIMAL_PLACES_FOR_UI
        }

        fun decorateAddress(currencyCode: CurrencyCode, address: String) : String {
            return address
            /*
                Need to re-create BCH logic?

                    String result = BRCoreAddress.bcashEncodeBitcoin(addr);
                    if (Utils.isNullOrEmpty(result)) return null;
                    if (result.contains(":")) return result.split(":")[1]; //bitcoincash:q24fs34....
                    return result;
             */
        }
    }
}