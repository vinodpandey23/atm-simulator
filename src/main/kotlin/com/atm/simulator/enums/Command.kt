package com.atm.simulator.enums

import com.atm.simulator.util.DEPOSIT_USAGE
import com.atm.simulator.util.LOGIN_USAGE
import com.atm.simulator.util.TRANSFER_USAGE
import com.atm.simulator.util.WITHDRAW_USAGE

enum class Command(val argCount: Int, val usageMsg: String? = null) {
    LOGIN(2, LOGIN_USAGE),
    DEPOSIT(2, DEPOSIT_USAGE),
    WITHDRAW(2, WITHDRAW_USAGE),
    TRANSFER(3, TRANSFER_USAGE),
    LOGOUT(1),
    EXIT(1);

    companion object {
        fun getCommands() = entries.map { it.name.lowercase() }
    }
}