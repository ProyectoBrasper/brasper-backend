package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum FixedAssetCommandFailure implements CommandFailure {
    FIXED_ASSET_NOT_FOUND("accounting.error.fixed-asset-not-found"),
    INVALID_USEFUL_LIFE("accounting.error.invalid-useful-life"),
    ALREADY_DISPOSED("accounting.error.fixed-asset-already-disposed"),
    INVALID_DISPOSAL_DATE("accounting.error.invalid-disposal-date"),
    NOTHING_TO_DEPRECIATE("accounting.error.nothing-to-depreciate"),
    DEPRECIATION_ALREADY_RUN("accounting.error.depreciation-already-run"),
    ACCOUNTS_NOT_CONFIGURED("accounting.error.accounting-accounts-not-configured");

    private final String messageKey;

    FixedAssetCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
