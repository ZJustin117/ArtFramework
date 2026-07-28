package artframework.sts1.input;

import artframework.context.IntentResult;
import artframework.context.UiIntent;

/**
 * Host-side intent execution for full-present surfaces (milestone 16.5). Pure contract; STS1
 * adapter implements against the live engine only when present level is FULL.
 */
public interface IntentExecutor {

    /** @return null to decline handling (caller may fall through); non-null is final. */
    IntentResult execute(UiIntent intent);
}
