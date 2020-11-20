[![Bread](/images/top-logo.png)](https://play.google.com/store/apps/details?id=com.breadwallet)

BRD is the best way to get started with bitcoin. Our simple, streamlined design is easy for beginners, yet powerful enough for experienced users.

### Fastsync
[Fastsync](https://brd.com/blog/fastsync-explained) is a new feature in the BRD app that makes Bitcoin wallets sync in seconds, while also keeping BRD technology ahead of the curve as SPV slowly phases out. When Fastsync is enabled the BRD wallet uses our server technology, [Blockset](https://docs.blockset.com/) to sync, send and receive instantly!

### Your Decentralized Bitcoin Wallet

Unlike other Android bitcoin wallets, **BRD** users have the option to disable Fastsync converting the wallet into a standalone bitcoin client. It connects directly to the bitcoin network using [SPV](https://en.bitcoin.it/wiki/Thin_Client_Security#Header-Only_Clients) mode, and doesn't rely on servers that can be hacked or disabled. If BRD the company disappears, your private key can still be derived from the recovery phrase to recover your funds since your funds exist on the blockchain.

### Cutting-edge security

**BRD** utilizes the latest Android security features to protect users from malware, browser security holes, and even physical theft. The user’s private key is encrypted using the Android Keystore, inaccessible to anyone other than the user.

### Designed with New Users in Mind

Simplicity and ease-of-use is **BRD**'s core design principle. A simple recovery phrase (which we call a recovery key) is all that is needed to restore the user's wallet if they ever lose or replace their device. **BRD** is [deterministic](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki), which means the user's balance and transaction history can be recovered just from the recovery key.

### Features

- Supports wallets for Bitcoin, Bitcoin Cash, Ethereum and ERC-20 tokens, Ripple, Hedera, Tezos
- Single recovery key is all that's needed to backup your wallet
- Private keys never leave your device and are end-to-end encrypted when using iCloud backup
- Save a memo for each transaction (off-chain)

### Bitcoin Specific Features
- Supports importing [password protected](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki) paper wallets
- Supports [JSON payment protocol](https://bitpay.com/docs/payment-protocol)
- Supports SegWit and bech32 addresses

### Localization

**BRD** is available in the following languages:

- Chinese (Simplified and traditional)
- Danish
- Dutch
- English
- French
- German
- Italian
- Japanese
- Korean
- Portuguese
- Russian
- Spanish
- Swedish

## Development Setup

1. Clone the repo: `git clone git@github.com:breadwallet/breadwallet-android.git`
2. Open `breadwallet-android` in [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) or [Android Studio](https://developer.android.com/studio)
3. Click Build > Build Project

## Advanced Setup

### Blockset Client Token

The [Blockset client token](https://blockset.com/docs/v1/tools/authentication) can be set in [gradle.properties](gradle.properties) or by using `-PBDB_CLIENT_TOKEN="<client token>"`.
A default token is available for testing.

### Firebase

To enable Firebase services like Crashlytics, add the `google-services.json` file into the `app` directory.
Without this file, runtime Firebase dependencies are still used but do not start and the Google Services gradle plugin is disabled so builds will succeed.

---

**BRD** is open source and available under the terms of the MIT license.

Source code is available at https://github.com/breadwallet
