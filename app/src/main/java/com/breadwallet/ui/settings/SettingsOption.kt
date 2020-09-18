/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/17/19.
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
package com.breadwallet.ui.settings

enum class SettingsOption {
    // HOME
    SCAN_QR,
    PREFERENCES,
    SECURITY_SETTINGS,
    SUPPORT,
    SUBMIT_REVIEW,
    REWARDS,
    ABOUT,
    ATM_FINDER,
    DEVELOPER_OPTIONS,

    // PREFERENCES
    CURRENCY,
    BTC_MENU,
    BCH_MENU,
    SHARE_ANONYMOUS_DATA,
    RESET_DEFAULT_CURRENCIES,
    NOTIFICATIONS,

    // SECURITY SETTINGS
    FINGERPRINT_AUTH,
    UPDATE_PIN,
    PAPER_KEY,
    WIPE,

    // DEVELOPER OPTIONS
    SEND_LOGS,
    API_SERVER,
    ONBOARDING_FLOW,
    WEB_PLAT_DEBUG_URL,
    WEB_PLAT_BUNDLE,
    TOKEN_BUNDLE,
    NATIVE_API_EXPLORER,
    WIPE_NO_PROMPT,
    ENABLE_ALL_WALLETS,
    TOGGLE_RATE_APP_PROMPT,

    // BTC
    REDEEM_PRIVATE_KEY,
    SYNC_BLOCKCHAIN_BTC,
    SYNC_BLOCKCHAIN_BCH,
    BTC_NODES,
    ENABLE_SEG_WIT,
    VIEW_LEGACY_ADDRESS,
    FAST_SYNC_BTC,
    // Hidden
    CLEAR_BLOCKCHAIN_DATA,
    REFRESH_TOKENS
}

