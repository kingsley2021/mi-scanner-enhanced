# Mi Scanner Enhanced

An LSPosed module that fixes two annoyances in Xiaomi Scanner.

[中文说明](README_zh.md)

## What it fixes

**Text scan results blocked by censorship.** Xiaomi Scanner sends text scan results to a remote server for content review. If the content is flagged, or the request fails, or there is no network, the result is hidden and removed from scan history. This module removes the server check so results are shown directly.

**Location permission required for WiFi QR codes.** The app requires location permission before connecting to a WiFi network from a scanned QR code. The actual connection does not need it. This module skips the location check while leaving other permissions like camera untouched.

**FIDO QR codes not handled.** The scanner classifies FIDO cross-device authentication QR codes (`FIDO:/...`) as plain text and sends them through content review, which blocks or hides the result. The scanner does contain FIDO handling code, but it is hardcoded to a single Xiaomi package (`com.fido.asm`) and is unreachable from the text classification path. This module intercepts `FIDO:` URIs in the text result flow and dispatches them as a standard `org.fidoalliance.intent.QRCodeAuthenticator` intent without a package restriction, so any registered FIDO authenticator (Google Password Manager, 1Password, Xiaomi's own handler, etc.) can respond.

## Installation

1. Download the APK from the [Releases](../../releases) page and install it.
2. In LSPosed, enable the module and make sure Xiaomi Scanner is checked as the target app.
3. Open Xiaomi Scanner. The module takes effect immediately.

## Compatibility

Tested on Xiaomi Scanner 15.9.77. Other versions should also work. If a Scanner update breaks something, please [open an issue](../../issues).

## License

[MIT](LICENSE)
