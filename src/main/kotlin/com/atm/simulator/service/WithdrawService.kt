package com.atm.simulator.service

import com.atm.simulator.api.WithdrawDto
import com.atm.simulator.exception.InvalidRequestException
import com.atm.simulator.model.AccountBalance
import com.atm.simulator.repository.AccountBalanceRepository
import com.atm.simulator.repository.UserDataRepository
import com.atm.simulator.util.INSUFFICIENT_BALANCE_MSG
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class WithdrawService(
    private val accountBalanceRepository: AccountBalanceRepository,
    private val userDataRepository: UserDataRepository
) {
    @Transactional
    fun processWithdraw(withdrawDto: WithdrawDto): AccountBalance {
        return with(userDataRepository.findByName(withdrawDto.name!!)!!.accountBalance!!) {
            LOGGER.info("User details for withdraw request: $this.")
            val withdrawAmount = withdrawDto.amount!!
            if (balance.compareTo(withdrawAmount) == -1) throw InvalidRequestException(INSUFFICIENT_BALANCE_MSG)
            balance = balance.minus(withdrawAmount)
            accountBalanceRepository.save(this).also {
                LOGGER.info("Saved accountBalance: $it.")
            }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}