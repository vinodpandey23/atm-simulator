package com.atm.simulator.api

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UserDto(
    @field:NotNull(message = "Field 'name' is mandatory.")
    @field:Size(min = 1, max = 64, message = "Length of 'name' must be between {min} and {max} characters.")
    val name: String? = null
)