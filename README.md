![ƀ](/images/icon.png) breadwallet for Android
----------------------------------

[![Get it on Google Play](/images/icon-google-play.png)](https://play.google.com/store/apps/details?id=com.breadwallet)

### bitcoin done right

This is the Android port of the breadwallet iOS app, which can be found [here](https://github.com/breadwallet/breadwallet/).

### a completely standalone bitcoin wallet

Unlike many other bitcoin wallets, breadwallet is a real standalone bitcoin client. There is no server to get hacked or go down, so you can always access your money. Using [SPV](https://en.bitcoin.it/wiki/Thin_Client_Security#Header-Only_Clients) mode, breadwallet connects directly to the bitcoin network with the fast performance you need on a mobile device.

### the next step in wallet security

breadwallet is designed to protect you from malware, browser security holes, *even physical theft*. With AES hardware encryption, app sandboxing, and verified boot, breadwallet represents a significant security advance over web and desktop wallets.

### beautiful simplicity

Simplicity is breadwallet's core design principle. A simple backup phrase is all you need to restore your wallet on another device if yours is ever lost or broken.  Because breadwallet is  [deterministic](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki), your balance and transaction history can be recovered from just your backup phrase.

## features

- ["simplified payment verification"](https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki) for fast mobile performance
- no server to get hacked or go down
- single backup phrase that works forever
- private keys never leave your device
- import [password protected](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki) paper wallets
- ["payment protocol"](https://github.com/bitcoin/bips/blob/master/bip-0070.mediawiki) payee identity certification

## Development setup

1. Clone the repo: `git clone git@github.com:breadwallet/breadwallet-android.git`
2. Open `breadwallet-android` in [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) or [Android Studio](https://developer.android.com/studio)
3. Click Build > Build Project

## Advanced setup

### Blockset Client Token

The [Blockset client token](https://blockset.com/docs/v1/tools/authentication) can be set in [gradle.properties](gradle.properties) or by using `-PBDB_CLIENT_TOKEN="<client token>"`.
A default token is available for testing.

### Firebase

To enable Firebase services like Crashlytics, add the `google-services.json` file into the `app` directory.
Without this file, runtime Firebase dependencies are still used but do not start and the Google Services gradle plugin is disabled so builds will succeed.
