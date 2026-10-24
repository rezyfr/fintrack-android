package com.fidriyanto.banktracker.domain.model

// The result of a backup export or import.
// NOT_A_BACKUP: the file is not a FinTrack backup. TOO_NEW: a newer version of the app made the file.
enum class BackupOutcome { DONE, NOT_A_BACKUP, TOO_NEW, FAILED }
