package com.fidriyanto.banktracker.data.datasource.remote.mapper

import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.google.gson.JsonParser
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetLineMapperTest {
    private fun json(body: RequestBody) = JsonParser.parseString(Buffer().also { body.writeTo(it) }.readUtf8()).asJsonObject

    private val categoryLine = BudgetLine(0, "Cafes", "flex", "AAA", 800.0, null, null, null, 4, "Food", "Cafe", null)

    @Test fun theCreateBodyHasTheFieldsOfANewLine() {
        val o = json(categoryLine.copy(dueDay = 9, kind = "fixed").toCreateBody())
        assertEquals("Cafes", o["name"].asString)
        assertEquals("fixed", o["kind"].asString)
        assertEquals("AAA", o["currency"].asString)
        assertEquals(800.0, o["target"].asDouble, 0.0)
        assertEquals(4, o["sort_order"].asInt)
        assertEquals("Food", o["category"].asString)
        assertEquals("Cafe", o["subcategory"].asString)
        assertEquals(9, o["due_day"].asInt)
        assertTrue(o["active"].asBoolean)
    }

    @Test fun theCreateBodyLeavesOutAnUnsetOption() {
        val o = json(categoryLine.copy(subcategory = null).toCreateBody())
        assertFalse(o.has("subcategory"))
        assertFalse(o.has("due_day"))
    }

    @Test fun theUpdateBodySendsNullToClearTheSubcategoryAndTheDueDay() {
        val o = json(categoryLine.copy(id = 3, subcategory = null).toUpdateBody())
        assertTrue(o.has("subcategory") && o["subcategory"].isJsonNull)
        assertTrue(o.has("due_day") && o["due_day"].isJsonNull)
        assertEquals("Food", o["category"].asString)
    }

    @Test fun theUpdateBodyOfARuleLineSendsNoCategoryAndNoMatchFields() {
        val o = json(categoryLine.copy(id = 3, category = null, subcategory = null, matchPattern = "Rent").toUpdateBody())
        assertFalse(o.has("category"))
        assertFalse(o.has("subcategory"))
        assertFalse(o.has("match_pattern"))
        assertEquals("Cafes", o["name"].asString)
    }
}
