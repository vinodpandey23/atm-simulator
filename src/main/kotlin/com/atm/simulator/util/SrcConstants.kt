package com.atm.simulator.util

import com.atm.simulator.enums.Command

// common
internal const val COMMAND_INPUT = "$ "
internal const val NEXT_LINE = "\n"
internal const val SPACE = " "

// info messages
internal const val EXIT_MSG = "Thank you, See you later!"
internal const val WELCOME_MSG = "Welcome to the ATM Simulator!"

// command usage messages
internal const val DEPOSIT_USAGE = "Usage: deposit [amount]"
internal const val LOGIN_USAGE = "Usage: login [name]"
internal const val TRANSFER_USAGE = "Usage: transfer [target] [amount]"
internal const val WITHDRAW_USAGE = "Usage: withdraw [amount]"

// error messages
internal const val AMOUNT_NOT_A_NUMBER_MSG = "Amount must be a number."
internal const val INTERNAL_SERVER_ERROR_MSG = "Internal server error. Please contact support team or try again."
internal const val INSUFFICIENT_BALANCE_MSG = "Insufficient balance."
internal const val TARGET_NOT_EXIST_MSG = "Target does not exist."
internal const val USER_ALREADY_LOGGED_IN_MSG = "User already logged in."
internal const val USER_NOT_LOGGED_IN_MSG = "User is not logged in."

internal val INVALID_COMMAND_MSG = "Invalid command. Supported commands: ${Command.getCommands()}."



