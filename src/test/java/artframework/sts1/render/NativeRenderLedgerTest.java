package artframework.sts1.render;

import artframework.component.Rect;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeRenderLedgerTest {
    private NativeRenderInvocation invocation(long id, long frame) {
        return new NativeRenderInvocation(id, frame, "combat", "owner", "Native", "render",
                "surface", "source", Rect.ZERO);
    }

    @Test
    public void nativeContinuationClosesInvocation() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.pass(1L, "off"));

        assertFalse(ledger.isOpen(1L));
        assertEquals(Integer.valueOf(0), ledger.strictReport().get("openInvocation"));
    }

    @Test
    public void delegatedEvidenceMustMatchInvocationFrame() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));

        try {
            ledger.recordEvidence(new PresentationDrawEvidence(1L, "entity", 5L, 1, "active"));
        } catch (IllegalStateException expected) {
            assertTrue(ledger.isOpen(1L));
            return;
        }
        throw new AssertionError("frame mismatch was accepted");
    }

    @Test
    public void delegatedEvidenceClosesInvocationExactlyOnce() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));
        ledger.recordEvidence(new PresentationDrawEvidence(1L, "entity", 4L, 1, "active"));

        assertFalse(ledger.isOpen(1L));
        try {
            ledger.recordEvidence(new PresentationDrawEvidence(1L, "entity", 4L, 1, "active"));
        } catch (IllegalStateException expected) {
            assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.evidenceCount()));
            return;
        }
        throw new AssertionError("duplicate evidence was accepted");
    }

    @Test
    public void undecidedInvocationIsReported() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        Map<String, Object> report = ledger.strictReport();

        assertEquals(Integer.valueOf(1), report.get("runtimeUNDECIDED"));
        assertEquals(Integer.valueOf(1), report.get("openInvocation"));
    }

    @Test
    public void delegatedFallbackIsReportedAsMismatchAndClosed() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));

        ledger.recordDelegatedFallback(1L);

        Map<String, Object> report = ledger.strictReport();
        assertEquals(Integer.valueOf(1), report.get("dispositionMismatch"));
        assertEquals(Integer.valueOf(0), report.get("openInvocation"));
        assertEquals(Integer.valueOf(1), report.get("delegatedWithoutEvidence"));
    }

    @Test
    public void recoveryRecordsFailOpenForUndecidedInvocation() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));

        ledger.closeForRecovery("panic");

        Map<String, Object> report = ledger.strictReport();
        assertEquals(Integer.valueOf(0), report.get("runtimeUNDECIDED"));
        assertEquals(Integer.valueOf(1), report.get("recoveryFailOpen"));
        assertEquals(Integer.valueOf(1), report.get("unrecordedFAIL_OPEN"));
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, ledger.disposition(1L).mode);
    }

    @Test
    public void recoveryClosesDelegatedInvocationAsMismatchWithoutEvidence() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));

        ledger.closeForRecovery("host_recreated");

        Map<String, Object> report = ledger.strictReport();
        assertEquals(Integer.valueOf(0), report.get("openInvocation"));
        assertEquals(Integer.valueOf(1), report.get("dispositionMismatch"));
        assertEquals(Integer.valueOf(1), report.get("delegatedWithoutEvidence"));
    }
}
