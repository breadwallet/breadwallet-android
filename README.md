## The easy and secure bitcoin (BTC), Bit mainland (BCH), and multi-asset wallet

Elephant is the best way to get started with cryptocurrency management like bitcoin, bit mainland. Our simple, streamlined design is easy for beginners, yet powerful enough for experienced users.

### Completely decentralized

Unlike other iOS bitcoin wallets, Elephant is a standalone bitcoin client. It connects directly to the bitcoin network using [SPV](https://en.bitcoin.it/wiki/Thin_Client_Security#Header-Only_Clients) mode, and doesn't rely on servers that can be hacked or disabled. Even if Elephant the company (Elastos) disappears, the app will continue to function, allowing users to access their money at any time.

### Cutting-edge security

Elephant utilizes AES hardware encryption, app sandboxing, and the latest iOS security features to protect users from malware, browser security holes, and even physical theft. Private keys are stored only in the secure enclave of the user's phone, inaccessible to anyone other than the user.

### Designed with new users in mind

Simplicity and ease-of-use is Elephant's core design principle. A simple recovery phrase (which we call a paper key) is all that is needed to restore the user's wallet if they ever lose or replace their device. Elephant is [deterministic](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki), which means the user's balance and transaction history can be recovered just from the paper key.

### Features

- [Simplified payment verification](https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki) for fast mobile performance
- No server to get hacked or go down for BTC and BCH
- Supports BTC, BCH, ELA (Elastos), and all ERC-20 tokens
- Single paper key is all that's needed to backup your wallet
- Private keys never leave your device
- Save a memo for each transaction (on-chain)
- Supports importing [password protected](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki) paper wallets

### Localization

Elephant is available in the following languages:

- Chinese (Simplified)
- English


### WARNING:

***Installation on jailbroken devices is strongly discouraged.***

Any jailbreak app can grant itself access to every other app's keychain data. This means it can access your wallet and steal your bitcoin by self-signing as described [here](http://www.saurik.com/id/8) and including `<key>application-identifier</key><string>*</string>` in its .entitlements file.

---

Elephant is open source and available under the terms of the MIT license.

Source code is available at https://github.com/elastos/Elastos.App.Wallet.Elephant.iOS and https://github.com/elastos/Elastos.App.Wallet.Elephant.Android


## How to set up the development environment
1. Download and install Java 7 or up
2. Download and Install the latest Android studio
3. Download and install the latest NDK https://developer.android.com/ndk/downloads/index.html or download it in android studio by "choosing the NDK" and press "download"(Ndk Version must be 16)
4. Go to https://github.com/elastos/Elastos.App.Wallet.Elephant.Android
5. Open the project with Android Studio and let the project sync
6. Go to SDK Manager and download all the SDK Platforms and SDK Tools
7. Build -> Rebuild Project
