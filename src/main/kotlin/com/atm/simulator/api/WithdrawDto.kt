package com.atm.simulator.api

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class WithdrawDto(
    @field:NotNull(message = "Field 'name' is mandatory.")
    @field:Size(min = 1, max = 64, message = "Length of 'name' must be between {min} and {max} characters.")
    val name: String? = null,
    @field:NotNull(message = "Field 'amount' is mandatory.")
    @get:Positive(message = "Value of 'amount' should be greater than 0.")
    @field:Digits(
        integer = 15,
        fraction = 2,
        message = "Value of 'amount' should have up to {integer} integer digits and {fraction} decimal places."
    )
    val amount: BigDecimal? = null
)