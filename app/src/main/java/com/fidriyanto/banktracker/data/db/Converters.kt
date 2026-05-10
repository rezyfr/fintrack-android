package com.fidriyanto.banktracker.data.db

import androidx.room.TypeConverter
import com.fidriyanto.banktracker.data.model.SheetTab
import com.fidriyanto.banktracker.data.model.TransactionStatus

class Converters {
    @TypeConverter fun fromSheetTab(v: SheetTab): String = v.name
    @TypeConverter fun toSheetTab(v: String): SheetTab = SheetTab.valueOf(v)
    @TypeConverter fun fromStatus(v: TransactionStatus): String = v.name
    @TypeConverter fun toStatus(v: String): TransactionStatus = TransactionStatus.valueOf(v)
}
