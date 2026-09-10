# VietQR fullscreen flow

## Purpose

The VietQR wallet configuration is a two-step fullscreen flow used by both Bill Split and Debtor Detail.

1. Select and search a supported bank by name, short name, or code.
2. Enter the receiving account number and normalized account-holder name, then save through the existing wallet update action.

## Interaction rules

- Existing VietQR configuration opens directly at the account step; the user can return to change the bank.
- Bank selection moves forward with a slide-and-fade transition.
- The save action remains disabled until a bank, account number, and account name are available.
- The flow is local UI state only. It does not read the clipboard, make a network request, or generate a payment QR while configuring.

## Compatibility

Saving preserves the existing `updateWalletForQr` path. Generated VietQR payment URLs continue to use the wallet data after it has been saved.