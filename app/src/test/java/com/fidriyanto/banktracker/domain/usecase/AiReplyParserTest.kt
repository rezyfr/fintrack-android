package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiReplyParserTest {
    private val cats = listOf(
        Category("Food & Drink", listOf("Coffee", "Restaurant"), true),
        Category("Groceries", emptyList(), true),
        Category("Other", emptyList(), true),
    )

    @Test fun amounts_with_thousands_separators() {
        assertEquals(125000.0, AiReplyParser.parseAmount("125.000")!!, 0.0)
        assertEquals(1500000.0, AiReplyParser.parseAmount("1,500,000")!!, 0.0)
        assertEquals(2000000.0, AiReplyParser.parseAmount("Rp 2.000.000")!!, 0.0)
    }

    @Test fun amounts_with_decimals() {
        assertEquals(85.0, AiReplyParser.parseAmount("85.00")!!, 0.0)
        assertEquals(245.5, AiReplyParser.parseAmount("245.50")!!, 0.0)
        assertEquals(1500.5, AiReplyParser.parseAmount("1,500.50")!!, 0.0)
        assertEquals(1500.5, AiReplyParser.parseAmount("1.500,50")!!, 0.0)
        assertNull(AiReplyParser.parseAmount("none"))
    }

    // ac: ai-amount-copied-verbatim — the prompt asks the model to copy the amount as written, so
    // these are the shapes a notification actually carries. An Indonesian amount uses . for
    // thousands and , for decimals, which is the opposite of the English one.
    @Test fun amounts_copied_from_the_notification() {
        assertEquals(134199.0, AiReplyParser.parseAmount("134.199,00")!!, 0.0)
        assertEquals(134199.0, AiReplyParser.parseAmount("IDR134.199,00")!!, 0.0)
        assertEquals(134199.0, AiReplyParser.parseAmount("Rp 134.199")!!, 0.0)
        assertEquals(134199.0, AiReplyParser.parseAmount("134,199.00")!!, 0.0)
        assertEquals(1234567.89, AiReplyParser.parseAmount("1.234.567,89")!!, 0.0)
    }

    // ac: ai-amount-copied-verbatim — a whole reply line carrying the amount as the notification wrote it
    @Test fun reads_an_indonesian_amount_from_a_reply() {
        val r = AiReplyParser.parseNotification(
            "expense|134.199,00|IDR|ASTRO Jakarta Barat|Groceries|",
            "transaksi Mandiri Kartu Kredit sejumlah IDR134.199,00",
            cats,
        )!!
        assertEquals(134199.0, r.amount, 0.0)
        assertEquals("IDR", r.currency)
        assertEquals("ASTRO Jakarta Barat", r.merchant)
    }

    @Test fun currencies_are_normalized() {
        assertEquals("IDR", AiReplyParser.normalizeCurrency("Rp"))
        assertEquals("THB", AiReplyParser.normalizeCurrency("บาท"))
        assertEquals("THB", AiReplyParser.normalizeCurrency("thb"))
        assertEquals("", AiReplyParser.normalizeCurrency("none"))
        assertEquals("IDR", AiReplyParser.detectCurrency("Transfer masuk Rp 2.000.000 dari BUDI"))
        assertEquals("THB", AiReplyParser.detectCurrency("ชำระเงิน 320.00 บาท ที่ TOPS"))
    }

    @Test fun reads_an_expense_line() {
        val r = AiReplyParser.parseNotification("expense|45,000|IDR|GRAB|Transport|", "x", cats)!!
        assertEquals(false, r.isIncome)
        assertEquals(45000.0, r.amount, 0.0)
        assertEquals("IDR", r.currency)
        assertEquals("GRAB", r.merchant)
        // Transport is not in this user's list, so it falls back to Other.
        assertEquals("Other", r.category)
    }

    @Test fun reads_category_and_subcategory_ignoring_case() {
        val r = AiReplyParser.parseNotification("expense|85.00|THB|7-ELEVEN|food & drink|coffee", "x", cats)!!
        assertEquals("Food & Drink", r.category)
        assertEquals("Coffee", r.subcategory)
    }

    @Test fun missing_currency_comes_from_the_text() {
        val r = AiReplyParser.parseNotification("income|2000000|none|BUDI||", "Transfer masuk Rp 2.000.000 dari BUDI", cats)!!
        assertEquals(true, r.isIncome)
        assertEquals("IDR", r.currency)
    }

    @Test fun rejects_non_payments_and_bad_lines() {
        assertNull(AiReplyParser.parseNotification("none|0|none|||", "Your OTP is 482913", cats))
        assertNull(AiReplyParser.parseNotification("expense|0|THB|x||", "x", cats))
        assertNull(AiReplyParser.parseNotification("I think this is a payment", "x", cats))
        assertNull(AiReplyParser.parseNotification("expense|10|none|x||", "no currency here", cats))
    }

    @Test fun suggestion_must_exist_in_the_list() {
        val s = AiReplyParser.parseSuggestion("Food & Drink|Coffee", cats)!!
        assertEquals("Food & Drink", s.category)
        assertEquals("Coffee", s.subcategory)
        assertNull(AiReplyParser.parseSuggestion("Travel|Hotel", cats))
        assertNull(AiReplyParser.parseSuggestion("Groceries|Milk", cats)!!.subcategory)
    }

    @Test fun any_currency_is_read_not_only_two() {
        val eur = AiReplyParser.parseNotification("expense|12,50|€|Cafe||", "x", cats)!!
        assertEquals("EUR", eur.currency)
        assertEquals(12.5, eur.amount, 0.0)
        val jpy = AiReplyParser.parseNotification("expense|3000|none|Store||", "Card used JPY 3,000 at Store", cats)!!
        assertEquals("JPY", jpy.currency)
    }
}
