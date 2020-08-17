# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
## Known limitation
- The theme in the cashUI is not handled properly.
## [4.4.9-beta]
## Added
- Upgraded CashUI 1.10-beta
- Added onActivityForResult callback in MainActivity to send the user to sendController

## [4.4.8-beta]
## Added
- Added the CS branding to the AlertDialogFragment
- Added the helper button to open in the alert dialog the CashSupport page (1.5.1) about troubleshooting the error: failed to submit transaction.
- Added the CashUI library and added the main hooks: Map, Request and RequestList.
## Changed
- Changed all the supportUI pages to use the new interface in CashUI.
## Remove
- Removed mapController and AtmRequestController, StatusListController and StatusController.
- Removed showIn extension for bottomDialogManager and added router extension to get fragmentManager.

## [4.4.7]
## Fixed
- Create recovery button in still says paper key
- Change text with `recovery key` to `recovery phrase`
- Dismissing dialog after atm cash out request [Details][Send], goes back to verification.
- Location link from awaiting screen does not work
- Purchase only atm are able to be selected using list
- Keyboard should be numeric in the verification form
- Enable the security flags to prevent screenshots
- Change `Redeem Private Key` to `Sweep Private key`
## Added
- Purchase only item in the list should should be grayout
- Added bitcoin icon (purchase) and cash icon (redemption) in the atm list

## [4.4.6]
## Update
- Removing circle_pro font.

## [4.4.5]
## Updated
- CashUI was updated to the 1.4.0, with minor fixes and the support of indexPage and detailsPage.
- Added FAQ about ATM (ported from coinsquareatm.com)
## Improved
- MapController has been improved adding `show all` / `show redemption only`, list vs map, search to filter in the list.
- Map performance has been improved (before the zoom was modified at every marker) and added a loading view 
## Fixed
- Funded state not showing
- Funded state has a wrong cash code

## [4.4.3 (40428001)]
## Changed
- BRDClient token has been changed.
## Added
- Added the supportPages (CashUI 1.0.3) for RECEIVE and SEND tips.
## Updated
- CashSDK was updated to the 1.3

## [4.4.2 (40428001)]
## Fixed
- Changed the text on the text screen in onboarding to "Welcome to Coinsquare networks"
- Changed the term paper key in the recovery flow
- Changed the title of the first onboarding page.

## [4.4.1 (40418001)]
## Fixed
- Fixed the problem where the map was not showing the pins the first time due during its initialization.
- Starting the session when the app is created instead of doing in lazy way when was needed.
## Added
- White app icon instead of black.
- Adding new icons and label in the home's bottom actions
- Adding `scan qr code` action.

## [4.4.0 (40408001)]
## Added
- Added the flags to show redemption vs purchase only atm.
## Security
- Adding session to the headers using the new CashSDK 2.0
## Fixed
- Crash in status.
 
## [4.3.1 (40318001)]
### Fixed
- I fixed the crash `Canvas: trying to draw too large` in Samsung S7

## [4.3.0 (40408001)]
### Fixed - Victor feedback
- change third page of onBoarding to "Buy bitcoin at thousands of ATMs" and let only one button to be continue
- zoom on the map Texas by default
- change the wording paper key to recovery phrase
- update play store images
- recover the ability to add tokens while the default tokens is only BTC

## Added
- improved navigation flow removing some ui controllers from the back stack.
- retry action in awaitingFundsStatus and in the statusListController
- added a loading state to the awaitingFundsStatus
- added a loading state to the status ListController
- better experience for lastName form field and enter code
## Changed
- Added code instead of token
- Provided a different helper text for code when is sms

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
## [4.2.6 (4025001)]
## Removed
- Disabling the clustered pins.

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

