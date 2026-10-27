# Third-party assets and trademarks

## Release blocker for an open-source publication

The project source is now published under the GNU General Public License v3.0, but that license covers the code and original assets only, not the third-party marks described below.

The repository currently bundles bank logos and VietQR bank metadata in `app/src/main/assets/vietqr/` (`banks.json` plus `logos/*.png`), synced by `scripts/sync-vietqr-banks.ps1`. The standalone NAPAS and VietQR wordmarks that used to sit in `app/src/main/res/drawable/` (`logo_napas.png`, `logo_vietqr.png`) are no longer referenced by any screen and were removed. No provenance, license, permission record, or attribution file accompanies the remaining assets in this repository.

Do not assume that finding a logo online grants redistribution rights. Bank names, NAPAS, VietQR, and their logos may be protected trademarks. This is not legal advice; obtain written permission or a confirmed license from each rights holder before shipping or publishing those image files.

## Recommended safe path

1. Before making the repository public, remove the bundled third-party logo PNGs from Git history and current sources unless their provenance is documented.
2. Replace them in the open-source default build with generic Material icons and plain bank names. Keep any optional proprietary branding in a separate, untracked distribution overlay only after permission.
3. Add a `NOTICE` or attribution record for every retained third-party asset: filename, source URL, copyright owner, license/permission, version/date, and required attribution.
4. Do not imply endorsement, partnership, certification, or affiliation with banks, NAPAS, or VietQR.

## QR implementation

`VietQrGenerator.kt` creates an EMV-style payload locally; it does not call a VietQR service and does not use a third-party SDK. That code can remain open source, but the product must not claim official VietQR compatibility or certification unless that status is verified with the relevant operator. The generated QR should be presented as a payment request and users must verify bank, account, amount, and memo in their banking app.

## App icon

The launcher vectors in `res/drawable/` are project assets, covered by the repository's GPL-3.0 license (see [LICENSE](../LICENSE)). Keep them original.
