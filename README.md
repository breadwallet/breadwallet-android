[![Litewallet](/images/header-android.png)](https://play.google.com/store/apps/details?id=com.loafwallet&hl=en_US)
======================
[![Release](https://img.shields.io/github/v/release/litecoin-foundation/loafwallet-android?style=plastic)](https://img.shields.io/github/v/release/litecoin-foundation/loafwallet-android)
[![MIT License](https://img.shields.io/github/license/litecoin-foundation/loafwallet-android?style=plastic)](https://img.shields.io/github/license/litecoin-foundation/loafwallet-android?style=plastic)

## Easy and secure
Litewallet is the best way to get started with Litecoin. Our simple, streamlined design is easy for beginners, yet powerful enough for experienced users. This is a free app produced by the Litecoin Foundation.

## Donations
The Litewallet Team is a group of global volunteers & part of the Litecoin Foundation that work hard to promote the use of Litecoin. Litewallet takes alot of time and resources to improve and test features but we need your help.  Please consider donating to one of our addresses:
|                                   Hardware Campaign                                   	|                              General Litecoin Foundation                              	|
|:-------------------------------------------------------------------------------------:	|:-------------------------------------------------------------------------------------:	|
| [QR Code](https://blockchair.com/litecoin/address/MJ4W7NZya4SzE7R6xpEVdamGCimaQYPiWu) 	| [QR Code](https://blockchair.com/litecoin/address/MVZj7gBRwcVpa9AAWdJm8A3HqTst112eJe) 	|

## Completely decentralized

Unlike other Android Litecoin wallets, **Litewallet** is a standalone Litecoin client. It connects directly to the Litecoin network using [SPV](https://en.bitcoin.it/wiki/Thin_Client_Security#Header-Only_Clients) mode, and doesn't rely on servers that can be hacked or disabled. Even if Litewallet is removed from the App Store, the app will continue to function, allowing users to access their valuable Litecoin at any time.

## Cutting-edge security

**Litewallet** utilizes AES hardware encryption, app sandboxing, and the latest Android security features to protect users from malware, browser security holes, and even physical theft. Private keys are stored only in the secure enclave of the user's phone, inaccessible to anyone other than the user.

## Designed with new users in mind

Simplicity and ease-of-use is **Litewallet**'s core design principle. A simple recovery phrase (which we call a paper key) is all that is needed to restore the user's wallet if they ever lose or replace their device. **Litewallet** is [deterministic](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki), which means the user's balance and transaction history can be recovered just from the paper key.

## Features:

- ["simplified payment verification"](https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki) for fast mobile performance
- no server to get hacked or go down
- single backup phrase that works forever
- private keys never leave your device
- import [password protected](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki) paper wallets
- ["payment protocol"](https://github.com/bitcoin/bips/blob/master/bip-0070.mediawiki) payee identity certification


## Localization

**Litewallet** is available in the following languages:

- Chinese (Simplified and traditional)
- Danish
- Dutch
- English
- French
- German
- Indonesian
- Italian
- Japanese
- Korean
- Portuguese
- Russian
- Spanish
- Swedish

---
## Litewallet Development:
[![GitHub issues](https://img.shields.io/github/issues/litecoin-foundation/loafwallet-android?style=plastic)](https://github.com/litecoin-foundation/loafwallet-android/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/litecoin-foundation/loafwallet-android?color=00ff00&style=plastic)](https://github.com/litecoin-foundation/loafwallet-android/pulls)

### Building & Developing Litewallet for Android:

1. Download and install Java 7 or up
2. Download and install the latest Android studio
3. Download NDK r15c from the [NDK Archives](https://developer.android.com/ndk/downloads/older_releases.html)
4. Clone this repo & init submodules
```bash
$ git clone https://github.com/litecoin-foundation/loafwallet-android
$ git submodule init
$ git submodule update
```
5. Open the project with Android Studio, navigate to `File > Project Structure > SDK Location`
6. Change `Android NDK Location` with the path to NDK r15c that you downloaded earlier
7. Go to SDK Manager and download all the SDK Platforms and SDK Tools
9. Build -> Rebuild Project

### Litewallet Team:
* [Development Code of Conduct](https://github.com/litecoin-foundation/litewallet/blob/master/DEVELOPMENT.md)
---
**Litecoin** source code is available at https://github.com/litecoin-project/litecoin

