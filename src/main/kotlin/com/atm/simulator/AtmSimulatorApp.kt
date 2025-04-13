package com.atm.simulator

import com.atm.simulator.api.UserDto
import com.atm.simulator.controller.AtmController
import com.atm.simulator.enums.Command
import com.atm.simulator.util.RequestCreator
import com.atm.simulator.util.*
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import kotlin.system.exitProcess

@Component
class AtmSimulatorApp(
    private val atmController: AtmController,
    private val requestCreator: RequestCreator
) {
    fun start() {
        println(WELCOME_MSG)

        // tracked the current logged-in user
        var loggedInUser: UserDto? = null

        while (true) {
            print(COMMAND_INPUT)
            val command = readlnOrNull()?.trim() ?: continue
            val commandArguments = command.split(SPACE)

            LOGGER.info("Input command: $command.")

            // validate invalid commands
            val operation = try {
                Command.valueOf(commandArguments[0].uppercase())
            } catch (e: IllegalArgumentException) {
                PrintUtilities.printError(INVALID_COMMAND_MSG)
                LOGGER.error(INVALID_COMMAND_MSG)
                continue
            }

            if (!operation.isValidArguments(commandArguments, loggedInUser)) continue

            // create request dto and execute each command
            val response = when (operation) {
                Command.LOGIN -> requestCreator.createUserDto(commandArguments).let {
                    loggedInUser = it
                    atmController.loginUser(it)
                }

                Command.DEPOSIT -> requestCreator.createDepositDto(loggedInUser!!, commandArguments).let {
                    atmController.depositAmount(it)
                }

                Command.WITHDRAW -> requestCreator.createWithdrawDto(loggedInUser!!, commandArguments).let {
                    atmController.withdrawAmount(it)
                }

                Command.TRANSFER -> requestCreator.createTransferDto(loggedInUser!!, commandArguments).let {
                    atmController.transferAmount(it)
                }

                Command.LOGOUT -> atmController.logoutUser(loggedInUser!!).also {
                    loggedInUser = null
                }

                Command.EXIT -> {
                    PrintUtilities.printSuccess(EXIT_MSG).also {
                        LOGGER.info(EXIT_MSG)
                        // logout on exit without logout command
                        if (loggedInUser != null) atmController.logoutUser(loggedInUser!!)
                    }
                    exitProcess(0)
                }
            }

            PrintUtilities.print(response)
        }
    }

    private fun Command.isValidArguments(commandArguments: List<String>, loggedInUser: UserDto?): Boolean {
        return when {
            // validate number of arguments for valid commands
            argCount > 1 && commandArguments.size != argCount -> usageMsg!!
            // user must be logged-in for all operation except login and exit
            this !in arrayOf(Command.LOGIN, Command.EXIT) && loggedInUser == null -> USER_NOT_LOGGED_IN_MSG
            // user must not be allowed to log-in is previous user is not logged-out
            this == Command.LOGIN && loggedInUser != null -> USER_ALREADY_LOGGED_IN_MSG
            // validate amount type for transactional commands
            this in arrayOf(Command.DEPOSIT, Command.WITHDRAW, Command.TRANSFER)
                    && runCatching { BigDecimal(commandArguments.lastOrNull()) }.isFailure -> AMOUNT_NOT_A_NUMBER_MSG

            else -> null
        }?.let {
            // print error message and return
            PrintUtilities.printError(it)
            LOGGER.error("Validation error: $it.")
            false
        } ?: true
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}