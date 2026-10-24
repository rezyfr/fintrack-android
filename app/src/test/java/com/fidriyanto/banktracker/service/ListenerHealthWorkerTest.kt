package com.fidriyanto.banktracker.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// ac: notification-listener-rebinds — the rule the periodic check applies. The live repair could not
// be reproduced on an emulator, because a stock Android rebinds the listener at once; the fault
// shows on a phone whose maker stops the app and leaves it unbound.
class ListenerHealthWorkerTest {
    @Test fun `repairs a granted listener that is not bound`() {
        assertTrue(ListenerHealthWorker.needsRebind(granted = true, connected = false))
    }

    @Test fun `leaves a bound listener alone`() {
        // A needless rebind unbinds first, which is itself a gap in cover.
        assertFalse(ListenerHealthWorker.needsRebind(granted = true, connected = true))
    }

    @Test fun `never asks to rebind a listener the user did not grant`() {
        assertFalse(ListenerHealthWorker.needsRebind(granted = false, connected = false))
        assertFalse(ListenerHealthWorker.needsRebind(granted = false, connected = true))
    }
}
