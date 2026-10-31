package com.fidriyanto.banktracker.ui.settings

// ac: listener-repair - what the capture status card says. The access can be on while the listener is not running.
enum class ListenerStatus {
    RUNNING,
    STOPPED,
    NO_ACCESS;

    companion object {
        fun of(hasAccess: Boolean, running: Boolean): ListenerStatus = when {
            !hasAccess -> NO_ACCESS
            running -> RUNNING
            else -> STOPPED
        }
    }
}
