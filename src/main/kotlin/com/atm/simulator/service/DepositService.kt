package com.atm.simulator.service

import com.atm.simulator.api.DepositDto
import com.atm.simulator.model.AccountBalance
import com.atm.simulator.model.OwedBalance
import com.atm.simulator.model.UserData
import com.atm.simulator.repository.AccountBalanceRepository
import com.atm.simulator.repository.OwedBalanceRepository
import com.atm.simulator.repository.UserDataRepository
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class DepositService(
    private val accountBalanceRepository: AccountBalanceRepository,
    private val owedBalanceRepository: OwedBalanceRepository,
    private val userDataRepository: UserDataRepository
) {
    @Transactional
    fun processDeposit(depositDto: DepositDto): AccountBalance {
        val user = userDataRepository.findByName(depositDto.name!!)
        return with(user!!.accountBalance!!) {
            var amountToDeposit = depositDto.amount!!

            // if user owes to other users then process settlement and use remaining amount to deposit
            if (owedAmount > BigDecimal.ZERO) {
                LOGGER.info("User owes amount: $owedAmount to others. Process settlement first.")
                val amountToSettle = amountToDeposit.min(owedAmount)
                settle(user, amountToSettle)
                // set owed amount after settlement
                owedAmount = owedAmount.minus(amountToSettle)
                // set amount to deposit
                amountToDeposit = amountToDeposit.minus(amountToSettle)
            }

            updateAndSave(amountToDeposit)
        }
    }

    private fun settle(user: UserData, amountToSettle: BigDecimal) {
        var runningAmountToSettle = amountToSettle
        // map to track settlement related transfer details
        val transferDetails = mutableMapOf<String, BigDecimal>()
        // fetch all owed amount for the user
        owedBalanceRepository.findBySourceUserDataId(user.id!!).forEach {
            with(it) {
                if (runningAmountToSettle > BigDecimal.ZERO) {
                    LOGGER.info("Settle for owedBalance: $this.")
                    val targetSettleAmount = amount.min(runningAmountToSettle)

                    // if owe amount cannot be settled completely then update remaining owe amount
                    // otherwise delete entry
                    amount = amount.minus(targetSettleAmount)
                    saveOrDelete()

                    // update balance of target balance as per settled amount calculation
                    targetUserData.accountBalance!!.updateAndSave(targetSettleAmount)
                    transferDetails[targetUserData.name] = targetSettleAmount

                    // update to track remaining amount that can be used for further settlements
                    runningAmountToSettle = runningAmountToSettle.minus(targetSettleAmount)
                }
            }
        }

        // set transfer details related with owed amount settlement
        user.accountBalance!!.transferDetails = transferDetails
        // set user and amount details that current user still owes to others even after settlement
        user.accountBalance!!.owedToDetails = owedBalanceRepository.findBySourceUserDataId(user.id!!).associate {
            it.targetUserData.name to it.amount
        }
    }

    private fun OwedBalance.saveOrDelete() {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            LOGGER.info("Owed amount will be settled fully.")
            owedBalanceRepository.delete(this).also {
                LOGGER.info("Deleted owedBalance: $this.")
            }
        } else {
            LOGGER.info("Owed amount will be settled partially.")
            owedBalanceRepository.save(this).also {
                LOGGER.info("Saved owedBalance: $this.")
            }
        }
    }

    private fun AccountBalance.updateAndSave(amount: BigDecimal): AccountBalance {
        balance = balance.plus(amount)
        return accountBalanceRepository.save(this).let {
            LOGGER.info("Saved accountBalance: $it.")
            it
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}