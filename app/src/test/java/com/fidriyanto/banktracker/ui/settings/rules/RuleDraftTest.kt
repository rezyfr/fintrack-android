package com.fidriyanto.banktracker.ui.settings.rules

import com.fidriyanto.banktracker.domain.model.TextMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleDraftTest {
    private val base = RuleDraft(text = "grab", category = "Transport")

    @Test fun the_text_is_required() {
        assertEquals(WhenError.TEXT, RuleDraft().whenError())
        assertNull(base.whenError())
    }

    @Test fun the_text_is_optional_but_a_rule_needs_one_condition() {
        val noText = RuleDraft(textOn = false, category = "Transport")
        assertEquals(WhenError.NONE, noText.whenError())
        assertNull(noText.copy(amountOn = true, amountMin = "10").whenError())
        assertEquals(WhenError.TEXT, RuleDraft(textOn = true, text = " ").whenError())
        assertNull(noText.copy(amountOn = true, amountMin = "10").toRule("r").conditions.text)
    }

    @Test fun a_range_of_days_needs_a_first_day() {
        assertEquals(WhenError.DATE, base.copy(dateOn = true, dayTo = "5").whenError())
        assertEquals(WhenError.DATE, base.copy(dateOn = true, day = "2", dayTo = "40").whenError())
        assertNull(base.copy(dateOn = true, day = "2", dayTo = "5").whenError())
        assertEquals(5, base.copy(dateOn = true, day = "2", dayTo = "5").toRule("r").conditions.date?.dayTo)
        assertNull(base.copy(dateOn = true, day = "2", dayTo = "2").toRule("r").conditions.date?.dayTo)
        assertEquals("5", RuleDraft.from(base.copy(dateOn = true, day = "2", dayTo = "5").toRule("r")).dayTo)
    }

    @Test fun an_optional_part_is_checked_only_when_it_is_on() {
        assertNull(base.copy(amountOn = false, amountMin = "x").whenError())
        assertEquals(WhenError.AMOUNT, base.copy(amountOn = true).whenError())
        assertEquals(WhenError.AMOUNT, base.copy(amountOn = true, amountMin = "50", amountMax = "10").whenError())
        assertNull(base.copy(amountOn = true, amountMax = "10").whenError())
    }

    @Test fun times_need_the_form_hh_mm() {
        assertEquals(WhenError.TIME, base.copy(timeOn = true, timeFrom = "12:30", timeTo = "1pm").whenError())
        assertNull(base.copy(timeOn = true, timeFrom = "12:30", timeTo = "13:30").whenError())
        assertEquals(750, parseMinutes("12:30"))
        assertEquals(545, parseMinutes("9:05"))
        assertNull(parseMinutes("24:00"))
        assertEquals("09:05", formatMinutes(545))
    }

    @Test fun a_date_needs_a_valid_part_or_a_weekday() {
        assertEquals(WhenError.DATE, base.copy(dateOn = true).whenError())
        assertEquals(WhenError.DATE, base.copy(dateOn = true, day = "32").whenError())
        assertNull(base.copy(dateOn = true, day = "25", month = "12").whenError())
        assertNull(base.copy(dateOn = true, weekdays = setOf(5)).whenError())
    }

    @Test fun an_account_rule_needs_an_account() {
        assertEquals(WhenError.ACCOUNT, base.copy(accountOn = true).whenError())
    }

    @Test fun the_action_needs_a_category_unless_the_rule_ignores() {
        assertEquals(ThenError.CATEGORY, RuleDraft(text = "x").thenError())
        assertNull(RuleDraft(text = "x", ignore = true).thenError())
        assertEquals(ThenError.DESCRIPTION, base.copy(descOn = true).thenError())
    }

    @Test fun a_draft_turns_into_a_rule_and_back() {
        val d = base.copy(
            name = " Rides ", textMode = TextMode.STARTS_WITH, amountOn = true, amountMin = "20", amountMax = "80.5",
            timeOn = true, timeFrom = "18:00", timeTo = "23:00", dateOn = true, day = "25", month = "12", weekdays = setOf(5, 6),
            accountOn = true, accounts = setOf("MAIN"), typeOn = true, type = "income", subOn = true, subcategory = "Taxi", descOn = true, description = "Late taxi",
        )
        val rule = d.toRule("new")
        assertEquals("Rides", rule.name)
        assertEquals(6, d.detail)
        val back = RuleDraft.from(rule)
        assertEquals(d.copy(name = "Rides", id = "new", weekdays = setOf(5, 6)), back)
    }

    @Test fun an_ignore_draft_keeps_no_other_action() {
        val rule = base.copy(ignore = true, descOn = true, description = "x").toRule("r")
        assertEquals(true, rule.action.ignore)
        assertNull(rule.action.category)
        assertNull(rule.action.description)
    }
}
