package com.atm.simulator.api

import com.atm.simulator.enums.ResponseCode

data class Response(
    val code: ResponseCode,
    val messages: List<String>
)