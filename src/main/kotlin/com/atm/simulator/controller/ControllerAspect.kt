package com.atm.simulator.controller

import com.atm.simulator.api.Response
import com.atm.simulator.enums.ResponseCode
import com.atm.simulator.exception.InvalidRequestException
import com.atm.simulator.util.INTERNAL_SERVER_ERROR_MSG
import com.atm.simulator.util.PrintUtilities
import jakarta.validation.Validator
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Aspect
@Component
class ControllerAspect(private val validator: Validator) {

    @Around("execution(* com.atm.simulator.controller.*.*(..))")
    fun validateAndHandleException(joinPoint: ProceedingJoinPoint): Any? {
        return try {
            // validate dto for each controller class and return error if any violations
            validator.validate(joinPoint.args[0]).let { violations ->
                if (violations.isNotEmpty()) {
                    return Response(ResponseCode.ERROR, violations.map { it.message })
                }
            }
            joinPoint.proceed()
        } catch (ex: Exception) {
            LOGGER.error(ex) { "Processing error: ${ex.message}." }
            when (ex) {
                // exception thrown by service classes
                is InvalidRequestException -> Response(ResponseCode.ERROR, listOf(ex.message!!))
                // if amount fields are not valid number
                is NumberFormatException -> Response(ResponseCode.ERROR, listOf(ex.message!!))
                else -> PrintUtilities.printError(INTERNAL_SERVER_ERROR_MSG).also {
                    exitProcess(0)
                }
            }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}