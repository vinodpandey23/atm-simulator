package com.atm.simulator.api

import com.atm.simulator.util.EMPTY
import com.atm.simulator.util.SOURCE_USER
import com.atm.simulator.util.TARGET_USER
import com.atm.simulator.util.VALIDATOR
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TransferDtoTest {

    @Test
    fun `valid TransferDtoTest`() {
        assertValidTransferDtoTest(
            // min length name, min length target and non-decimal amount
            TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal("10")),
            // max length name, max length target and decimal amount
            TransferDto(name = SOURCE_USER.repeat(64), target = TARGET_USER.repeat(64), amount = BigDecimal("10.20"))
        )
    }

    @Test
    fun `invalid TransferDtoTest`() {
        assertInvalidTransferDtoTest(
            // missing name
            TransferDto(target = TARGET_USER, amount = BigDecimal.TEN),
            // missing target
            TransferDto(name = SOURCE_USER, amount = BigDecimal.TEN),
            // missing amount
            TransferDto(target = TARGET_USER, name = SOURCE_USER),
            // empty name
            TransferDto(name = EMPTY, target = TARGET_USER, amount = BigDecimal.TEN),
            // max length exceed for name
            TransferDto(name = SOURCE_USER.repeat(65), target = TARGET_USER, amount = BigDecimal.TEN),
            // empty target
            TransferDto(name = SOURCE_USER, target = EMPTY, amount = BigDecimal.TEN),
            // max length exceed for target
            TransferDto(name = SOURCE_USER, target = TARGET_USER.repeat(65), amount = BigDecimal.TEN),
            // zero transfer amount
            TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal.ZERO),
            // negative transfer amount
            TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal("-1")),
            // transfer amount with more than 15 digits before decimal places
            TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal("${"1".repeat(16)}.50")),
            // transfer amount with more than 2 decimal places
            TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal("10.345")),
            // ControllerAspect.validateAndHandleException have logic to handle invalid transfer amount
        )
    }

    private fun assertInvalidTransferDtoTest(vararg transferDto: TransferDto) {
        transferDto.forEach { assertTrue(VALIDATOR.validate(it).size == 1) }
    }

    private fun assertValidTransferDtoTest(vararg transferDto: TransferDto) {
        transferDto.forEach { assertTrue(VALIDATOR.validate(it).isNullOrEmpty()) }
    }
}