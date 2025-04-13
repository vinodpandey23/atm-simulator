package com.atm.simulator.util

import com.atm.simulator.api.DepositDto
import com.atm.simulator.api.TransferDto
import com.atm.simulator.api.UserDto
import com.atm.simulator.api.WithdrawDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class RequestCreator {

    fun createUserDto(commandArguments: List<String>) =
        UserDto(commandArguments[1])

    fun createDepositDto(userDto: UserDto, commandArguments: List<String>) =
        DepositDto(name = userDto.name!!, amount = BigDecimal(commandArguments[1]))

    fun createWithdrawDto(userDto: UserDto, commandArguments: List<String>) =
        WithdrawDto(name = userDto.name!!, amount = BigDecimal(commandArguments[1]))

    fun createTransferDto(userDto: UserDto, commandArguments: List<String>) =
        TransferDto(name = userDto.name!!, target = commandArguments[1], amount = BigDecimal(commandArguments[2]))
}