package com.fidriyanto.banktracker.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ListenerStatusTest {
    @Test fun noAccessIsNoAccessWhateverTheListenerDoes() {
        assertEquals(ListenerStatus.NO_ACCESS, ListenerStatus.of(hasAccess = false, running = true))
        assertEquals(ListenerStatus.NO_ACCESS, ListenerStatus.of(hasAccess = false, running = false))
    }

    @Test fun accessWithARunningListenerIsRunning() {
        assertEquals(ListenerStatus.RUNNING, ListenerStatus.of(hasAccess = true, running = true))
    }

    @Test fun accessWithAStoppedListenerIsStopped() {
        assertEquals(ListenerStatus.STOPPED, ListenerStatus.of(hasAccess = true, running = false))
    }
}
