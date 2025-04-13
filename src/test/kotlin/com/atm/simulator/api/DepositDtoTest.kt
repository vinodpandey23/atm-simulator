package com.atm.simulator.api

import com.atm.simulator.util.EMPTY
import com.atm.simulator.util.SOURCE_USER
import com.atm.simulator.util.VALIDATOR
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DepositDtoTest {

    @Test
    fun `valid DepositDtoTest`() {
        assertValidDepositDtoTest(
            // min length name and non-decimal amount
            DepositDto(name = SOURCE_USER, amount = BigDecimal("10")),
            // max length name and decimal amount
            DepositDto(name = SOURCE_USER.repeat(64), amount = BigDecimal("10.20"))
        )
    }

    @Test
    fun `invalid DepositDtoTest`() {
        assertInvalidDepositDtoTest(
            // missing name
            DepositDto(amount = BigDecimal.TEN),
            // missing amount
            DepositDto(name = SOURCE_USER),
            // empty name
            DepositDto(name = EMPTY, amount = BigDecimal.TEN),
            // max length exceed for name
            DepositDto(name = SOURCE_USER.repeat(65), amount = BigDecimal.TEN),
            // zero deposit amount
            DepositDto(name = SOURCE_USER, amount = BigDecimal.ZERO),
            // negative deposit amount
            DepositDto(name = SOURCE_USER, amount = BigDecimal("-1")),
            // deposit amount with more than 15 digits before decimal places
            DepositDto(name = SOURCE_USER, amount = BigDecimal("${"1".repeat(16)}.50")),
            // deposit amount with more than 2 decimal places
            DepositDto(name = SOURCE_USER, amount = BigDecimal("10.345")),
            // ControllerAspect.validateAndHandleException have logic to handle invalid deposit amount
        )
    }

    private fun assertInvalidDepositDtoTest(vararg depositDto: DepositDto) {
        depositDto.forEach { assertTrue(VALIDATOR.validate(it).size == 1) }
    }

    private fun assertValidDepositDtoTest(vararg depositDto: DepositDto) {
        depositDto.forEach { assertTrue(VALIDATOR.validate(it).isNullOrEmpty()) }
    }
}