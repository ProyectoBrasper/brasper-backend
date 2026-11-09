package com.brasper.platform.accounting.application.eventhandlers;

import com.brasper.platform.accounting.application.commandservices.LedgerCommandFailure;
import com.brasper.platform.shared.application.result.Result;

final class PostingResults {

    private PostingResults() { }

    /** A repeated source means the entry is already there, which is what we want; anything else rolls back. */
    static void check(Result<?, LedgerCommandFailure> result) {
        if (result.isFailure() && result.failure().orElseThrow() != LedgerCommandFailure.DUPLICATE_SOURCE) {
            throw new IllegalStateException("The ledger rejected the entry: " + result.failure().orElseThrow());
        }
    }
}
