package com.fidriyanto.banktracker.data.db

import androidx.room.TypeConverter
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionStatus

class Converters {
    @TypeConverter fun fromLedgerTab(v: LedgerTab): String = v.name
    @TypeConverter fun toLedgerTab(v: String): LedgerTab = LedgerTab.valueOf(v)
    @TypeConverter fun fromStatus(v: TransactionStatus): String = v.name
    @TypeConverter fun toStatus(v: String): TransactionStatus = TransactionStatus.valueOf(v)
}
