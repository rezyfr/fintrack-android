package com.fidriyanto.banktracker.ui.add

import com.fidriyanto.banktracker.domain.model.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TxFormValuesTest {
    private val bca = Account("BCA", "BCA", "IDR", isDefault = true)
    private val mandiri = Account("MANDIRI", "Mandiri", "IDR", isDefault = true)
    private val bbl = Account("BBL", "Bangkok Bank", "THB", isDefault = true)
    private val valid = TxFormValues(wallet = bca, amount = "220000", description = "Groceries")

    @Test fun completeExpenseIsValid() = assertNull(valid.validate())

    @Test fun amountMustBeAboveZero() {
        assertEquals(TxFormError.AMOUNT, valid.copy(amount = "").validate())
        assertEquals(TxFormError.AMOUNT, valid.copy(amount = "0").validate())
    }

    @Test fun descriptionIsRequired() = assertEquals(TxFormError.DESCRIPTION, valid.copy(description = "  ").validate())

    @Test fun transferNeedsADestination() =
        assertEquals(TxFormError.TO_WALLET, valid.copy(txType = TxType.TRANSFER).validate())

    @Test fun crossCurrencyTransferNeedsTheReceivedAmount() {
        val transfer = valid.copy(txType = TxType.TRANSFER, toWallet = bbl)
        assertEquals(TxFormError.RECEIVED, transfer.validate())
        assertNull(transfer.copy(toAmount = "400").validate())
        assertNull(valid.copy(txType = TxType.TRANSFER, toWallet = mandiri).validate())
    }
}
