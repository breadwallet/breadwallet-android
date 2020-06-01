# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
## [4.3.0 (4028001)]
### Fixed - Victor feedback
- change third page of onboarding to "Buy bitcoin at thousands of ATMs" and let only one button to be continue
- zoom on the map Texas by default
- change the wording paper key to recovery phrase
- update play store images
- recover the ability to add tokens while the default tokens is only BTC

## [4.2.8 (4028001)]
### Fixed - Kal feedback
- change onboarding title to Sell your Bitcoin and Receive Cash at an ATM!
- change description to Our wallet is open source and fully compatible with any other wallet.
- remove BRD token, now only BTC will be shown by default
- capitalize Out in the Am Cash out action
- position of coinsquare logo aligned with total assets 0.00
- map bubbles info, change Atm to ATM
- change enter the phone number to enter your phone number
- change confirmation token to confirmation number, remove token word
- awaiting funds screen is cut off a bit, the bitcoin address is cut off, can-t see it fully.

## [4.2.7 (4026001)]
### Added
- Cash out status view checker
- Persist the latest request in the local storage
- Cash out request list
- Added empty states for cash out list

## Fixed
- Improved the experience in request form by using IME_ACTION_NEXT (reported by Todd)

## [4.2.6 (4026001)]
### Fixed
- The wac atm server used for main net was wrong.
- I fixed the position of the $ sign in the request view.
### Added
- Moved the cash out tab button to the middle
- Added activity tab button to the left.

## [4.2.5 (4025001)]
### Added
- Add a gray badge for testNet app icon.
- Changing package name and cert for MainNet App.
- Setting up Sonar

## [4.2.4 (4024001)]
### Fixed
- Fixing the issue with amount being same as min (added some unit tests)
- Fixing the message about email / sms being sent.

## [4.2.3 (4023001)]
### Added
- Adding the ability to choose TEST_NET vs MAIN_NET
- Customizing the sendSheetController to be used in the cash out atm flow.

## [4.2.1 (4021001)]
### Added
- Removing third page of onboarding.
- Adding a new asset / text for onboarding page.
- Disabling email verification and making phone the only method (@kal feedback to improve KYC process).
- Improved the style of the buttons in request verification code (color and rounded buttons).
- Adding the ability to navigate from cash code creation to the sendController.
- Filled the information on send controller, address and BTC value for cash out flow.
- Disabling the fee selection and adding the regular one for cash out flow.
- An easter egg was dropped in the requestCodeController.

### Fixed
- Fixed some issues provided by @anasoid about having brd urls in about, 
atmcoin/comm/issues/720
atmcoin/comm/issues/721
- Disabled buy, trade and reward features.
atmcoin/comm/issues/723
atmcoin/comm/issues/724
- Fixed the request where email and phone number where misplaced.


## [4.1.4-preview (4014002)]
### Added
- Added new assets and messages for onboarding pages.
- Fixed the verification code request email / phone.
- Wired up the send controller to the last step of cash code.

## [4.1.4-preview (4014001)]
### Added
- Added the mapController
- Added the RequestCashCodeController
- Displaying the list of ATMs in the map.
- Adding the form to request code verification.
- Adding the form to request secure code.
- Adding the AtmSharedPreferencesManager to store the existing cash out request.

## [4.1.3 (4013001)]
### Fixed
- (CI) fixing some issues shown in detekt `Ruleset: complexity` 
- AppIcon changed to Coinsquare.

## [4.1.2 (4012001)] - 2020-04-17
### Changed
- Changed the link to WAC from map to the home page.
- Changed all strings that contains BRD to Coinsquare.
- Changed the theme of the app, primary, secondary and tertiary to different blacks.
- Changed the colors of the pin screen to white and black.
### Added
- Adding a link to the WAC map in a new menu item.

## [4.1.1 (4011003)] - 2020-04-07
### Added
- Created the first signed builds with coinsquare certificate keys.
- Coinsquare vector images svg and vector drawable.
### Changed
- Renaming package in all google-service.json files.
- Changing app name and package.

[Unreleased]: https://github.com/olivierlacan/keep-a-changelog/compare/v1.0.0...HEAD
[0.0.2]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1

