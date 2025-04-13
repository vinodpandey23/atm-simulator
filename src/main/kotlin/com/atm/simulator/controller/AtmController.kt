package com.atm.simulator.controller

import com.atm.simulator.api.*
import com.atm.simulator.enums.ResponseCode
import com.atm.simulator.service.DepositService
import com.atm.simulator.service.TransferService
import com.atm.simulator.service.UserService
import com.atm.simulator.service.WithdrawService
import mu.KotlinLogging
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode

@Controller
class AtmController(
    private val depositService: DepositService,
    private val transferService: TransferService,
    private val userService: UserService,
    private val withdrawService: WithdrawService
) {
    fun loginUser(userDto: UserDto): Response {
        LOGGER.info { "Login requested for: $userDto." }
        return userService.processLogin(userDto).let {
            val messages = listOfNotNull(
                listOf(
                    "Hello, ${it.name}!",
                    "Your balance is $${scale(it.accountBalance!!.balance)}"
                ),
                it.accountBalance!!.owedFromDetails?.getOwedFromDetails(),
                it.accountBalance!!.owedToDetails?.getOwedToDetails()
            ).flatten()
            Response(ResponseCode.SUCCESS, messages)
        }
    }

    fun logoutUser(userDto: UserDto): Response {
        LOGGER.info { "Logout requested for: $userDto." }
        return Response(ResponseCode.SUCCESS, listOf("Goodbye, ${userService.processLogout(userDto).name}!"))
    }

    fun depositAmount(depositDto: DepositDto): Response {
        LOGGER.info { "Deposit requested for: $depositDto." }
        return depositService.processDeposit(depositDto).let {
            val messages = listOfNotNull(
                it.transferDetails?.getTransferDetails(),
                listOf("Your balance is $${scale(it.balance)}"),
                it.owedToDetails?.getOwedToDetails()
            ).flatten()
            Response(ResponseCode.SUCCESS, messages)
        }
    }

    fun withdrawAmount(withdrawDto: WithdrawDto): Response {
        LOGGER.info { "Withdraw requested for: $withdrawDto." }
        return Response(
            ResponseCode.SUCCESS,
            listOf(
                "Collect cash",
                "Your balance is $${scale(withdrawService.processWithdraw(withdrawDto).balance)}"
            )
        )
    }

    fun transferAmount(transferDto: TransferDto): Response {
        LOGGER.info { "Transfer requested for: $transferDto." }
        return transferService.processTransfer(transferDto).let {
            val messages = listOfNotNull(
                it.transferDetails?.getTransferDetails(),
                listOf("Your balance is $${scale(it.balance)}"),
                if (it.owedAmount > BigDecimal.ZERO) listOf("Owed $${scale(it.owedAmount)} to ${transferDto.target}") else null,
                it.owedFromDetails?.getOwedFromDetails()
            ).flatten()
            Response(ResponseCode.SUCCESS, messages)
        }
    }

    private fun Map<String, BigDecimal>.getOwedFromDetails() = map { "Owed $${scale(it.value)} from ${it.key}" }
    private fun Map<String, BigDecimal>.getOwedToDetails() = map { "Owed $${scale(it.value)} to ${it.key}" }
    private fun Map<String, BigDecimal>.getTransferDetails() = map { "Transferred $${scale(it.value)} to ${it.key}" }
    private fun scale(amount: BigDecimal) = amount.setScale(2, RoundingMode.HALF_UP)

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}