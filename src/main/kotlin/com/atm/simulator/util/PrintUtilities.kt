package com.atm.simulator.util

import com.atm.simulator.api.Response
import com.atm.simulator.enums.ResponseCode
import mu.KotlinLogging

object PrintUtilities {

    private val LOGGER = KotlinLogging.logger { }

    fun print(response: Response) {
        with(response) {
            val message = messages.joinToString(NEXT_LINE)
            if (code == ResponseCode.SUCCESS) printSuccess(message).also {
                LOGGER.info("Response: $response.")
            }
            else printError(message).also {
                LOGGER.error("Response: $response.")
            }
        }
    }

    fun printSuccess(message: String) = println(message)
    fun printError(message: String) = println("\u001B[31m$message\u001B[0m")
}