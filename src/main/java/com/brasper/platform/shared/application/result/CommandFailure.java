package com.brasper.platform.shared.application.result;

/** An expected failure of a command service. Each context declares its own sealed set of these. */
public interface CommandFailure {

    /** Key in messages*.properties used to explain the failure to the API caller. */
    String messageKey();
}
