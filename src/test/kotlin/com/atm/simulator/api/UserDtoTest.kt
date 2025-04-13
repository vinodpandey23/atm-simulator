package com.atm.simulator.api

import com.atm.simulator.util.EMPTY
import com.atm.simulator.util.SOURCE_USER
import com.atm.simulator.util.VALIDATOR
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserDtoTest {

    @Test
    fun `valid UserDtoTest`() {
        assertValidUserDtoTest(
            // min length name
            UserDto(name = SOURCE_USER),
            // max length name
            UserDto(name = SOURCE_USER.repeat(64))
        )
    }

    @Test
    fun `invalid UserDtoTest`() {
        assertInvalidUserDtoTest(
            // missing name
            UserDto(),
            // empty name
            UserDto(name = EMPTY),
            // max length exceed for name
            UserDto(name = SOURCE_USER.repeat(65)),
        )
    }

    private fun assertInvalidUserDtoTest(vararg userDto: UserDto) {
        userDto.forEach { assertTrue(VALIDATOR.validate(it).size == 1) }
    }

    private fun assertValidUserDtoTest(vararg userDto: UserDto) {
        userDto.forEach { assertTrue(VALIDATOR.validate(it).isNullOrEmpty()) }
    }
}