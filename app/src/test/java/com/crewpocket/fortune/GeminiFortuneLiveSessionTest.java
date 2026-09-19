package com.crewpocket.fortune;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GeminiFortuneLiveSessionTest {
    @Test public void normalCaptureWaitsWhileTeacherIsSpeaking() {
        assertFalse(GeminiFortuneLiveSession.shouldSendCapturedAudio(
                false, true, 0L, 1000L));
    }

    @Test public void normalCaptureHonorsEchoSuppression() {
        assertFalse(GeminiFortuneLiveSession.shouldSendCapturedAudio(
                false, false, 2000L, 1000L));
        assertTrue(GeminiFortuneLiveSession.shouldSendCapturedAudio(
                false, false, 500L, 1000L));
    }

    @Test public void manualBargeInAlwaysLetsUserAudioThrough() {
        assertTrue(GeminiFortuneLiveSession.shouldSendCapturedAudio(
                true, true, Long.MAX_VALUE, 1000L));
    }
}
