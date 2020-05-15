# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
## [TODO]
- Add a badge for testnet app icon.

## [4.2.3 (4023001)]
- Adding the ability to choose TEST_NET vs MAIN_NET
- Customizing the sendSheetController to be used in the cash out atm flow.

## [4.2.1 (4021001)]
- Removing third page of onboarding.
- Adding a new asset / text for onboarding page.
- Disabling email verification and making phone the only method (@kal feedback to improve KYC process).
- Fixed some issues provided by @anasoid about having brd urls in about, 
atmcoin/comm/issues/720
atmcoin/comm/issues/721
- Disabled buy, trade and reward features.
atmcoin/comm/issues/723
atmcoin/comm/issues/724
- Fixed the request where email and phone number where misplaced.
- Improved the style of the buttons in request verification code (color and rounded buttons).
- Adding the ability to navigate from cash code creation to the sendController.
- Filled the information on send controller, address and BTC value for cash out flow.
- Disabling the fee selection and adding the regular one for cash out flow.
- An easter egg was dropped in the requestCodeController.

## [4.1.4-preview (4014002)]
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

