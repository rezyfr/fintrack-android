package com.fidriyanto.banktracker.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionStatus

class Converters {
    // A list of text, such as the accounts of a budget line, is stored as JSON.
    @TypeConverter fun fromTextList(v: List<String>?): String? = v?.let { listGson.toJson(it) }
    @TypeConverter fun toTextList(v: String?): List<String>? = v?.let { listGson.fromJson<List<String>>(it, textListType) }

    @TypeConverter fun fromLedgerTab(v: LedgerTab): String = v.name
    @TypeConverter fun toLedgerTab(v: String): LedgerTab = LedgerTab.valueOf(v)
    @TypeConverter fun fromStatus(v: TransactionStatus): String = v.name
    @TypeConverter fun toStatus(v: String): TransactionStatus = TransactionStatus.valueOf(v)
}

private val listGson = Gson()
private val textListType = object : TypeToken<List<String>>() {}.type
