package com.atm.simulator.util

import jakarta.validation.Validation

// common
internal const val EMPTY = ""

internal val VALIDATOR = Validation.buildDefaultValidatorFactory().validator

// DTO
internal const val SOURCE_USER = "A"
internal const val SOURCE_USER_ID: Long = 1
internal const val TARGET_USER = "B"
internal const val TARGET_USER_ID: Long = 2