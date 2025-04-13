package com.atm.simulator.api

import com.atm.simulator.util.EMPTY
import com.atm.simulator.util.SOURCE_USER
import com.atm.simulator.util.VALIDATOR
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class WithdrawDtoTest {

    @Test
    fun `valid WithdrawDtoTest`() {
        assertValidWithdrawDtoTest(
            // min length name and non-decimal amount
            WithdrawDto(name = SOURCE_USER, amount = BigDecimal("10")),
            // max length name and decimal amount
            WithdrawDto(name = SOURCE_USER.repeat(64), amount = BigDecimal("10.20"))
        )
    }

    @Test
    fun `invalid WithdrawDtoTest`() {
        assertInvalidWithdrawDtoTest(
            // missing name
            WithdrawDto(amount = BigDecimal.TEN),
            // missing amount
            WithdrawDto(name = SOURCE_USER),
            // empty name
            WithdrawDto(name = EMPTY, amount = BigDecimal.TEN),
            // max length exceed for name
            WithdrawDto(name = SOURCE_USER.repeat(65), amount = BigDecimal.TEN),
            // zero withdraw amount
            WithdrawDto(name = SOURCE_USER, amount = BigDecimal.ZERO),
            // negative withdraw amount
            WithdrawDto(name = SOURCE_USER, amount = BigDecimal("-1")),
            // withdraw amount with more than 15 digits before decimal places
            WithdrawDto(name = SOURCE_USER, amount = BigDecimal("${"1".repeat(16)}.50")),
            // withdraw amount with more than 2 decimal places
            WithdrawDto(name = SOURCE_USER, amount = BigDecimal("10.345")),
            // ControllerAspect.validateAndHandleException have logic to handle invalid withdraw amount
        )
    }

    private fun assertInvalidWithdrawDtoTest(vararg withdrawDto: WithdrawDto) {
        withdrawDto.forEach { assertTrue(VALIDATOR.validate(it).size == 1) }
    }

    private fun assertValidWithdrawDtoTest(vararg withdrawDto: WithdrawDto) {
        withdrawDto.forEach { assertTrue(VALIDATOR.validate(it).isNullOrEmpty()) }
    }
}