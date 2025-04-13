package com.atm.simulator.service

import com.atm.simulator.api.TransferDto
import com.atm.simulator.exception.InvalidRequestException
import com.atm.simulator.model.AccountBalance
import com.atm.simulator.model.OwedBalance
import com.atm.simulator.model.UserData
import com.atm.simulator.repository.AccountBalanceRepository
import com.atm.simulator.repository.OwedBalanceRepository
import com.atm.simulator.repository.UserDataRepository
import com.atm.simulator.util.TARGET_NOT_EXIST_MSG
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class TransferService(
    private val accountBalanceRepository: AccountBalanceRepository,
    private val owedBalanceRepository: OwedBalanceRepository,
    private val userDataRepository: UserDataRepository
) {
    @Transactional
    fun processTransfer(transferDto: TransferDto): AccountBalance {
        val source = userDataRepository.findByName(transferDto.name!!)!!
        val target = userDataRepository.findByName(transferDto.target!!)
            ?: throw InvalidRequestException(TARGET_NOT_EXIST_MSG)

        LOGGER.info("Source: $source.")
        LOGGER.info("Target: $target.")

        val sourceAccountBalance = source.accountBalance!!
        val targetAccountBalance = target.accountBalance!!

        val transferAmount = transferDto.amount!!

        if (sourceAccountBalance.balance.compareTo(transferAmount) == -1) {
            LOGGER.info("Source account balance is less than requested transfer amount.")
            transferForInsufficientBalance(source, target, transferAmount)
        } else {
            LOGGER.info("Source account balance is more than or equal to requested transfer amount.")
            transferForSufficientBalance(source, target, transferAmount)
        }

        // set transfer details related with owed amount settlement
        sourceAccountBalance.owedFromDetails = owedBalanceRepository.findByTargetUserDataId(source.id!!)
            .associate { item ->
                item.sourceUserData.name to item.amount
            }

        // settled owed amounts after balance update of target account
        if (targetAccountBalance.owedAmount > BigDecimal.ZERO) {
            settle(sourceAccountBalance, targetAccountBalance)
        }

        return sourceAccountBalance
    }

    private fun transferForInsufficientBalance(source: UserData, target: UserData, transferAmount: BigDecimal) {
        val sourceAccountBalance = source.accountBalance!!
        val targetAccountBalance = target.accountBalance!!

        // add target balance with whatever source have balance
        targetAccountBalance.updateAndSave(sourceAccountBalance.balance)

        // Update source's owed amount to target and deduct whole balance from source's account
        val owedAmount = transferAmount - sourceAccountBalance.balance
        sourceAccountBalance.owedAmount = sourceAccountBalance.owedAmount.plus(owedAmount)
        sourceAccountBalance.transferDetails = mutableMapOf(target.name to sourceAccountBalance.balance)
        sourceAccountBalance.updateAndSave(sourceAccountBalance.balance.negate())

        // add record for source to target own details
        OwedBalance(sourceUserData = source, targetUserData = target, amount = owedAmount).saveOrDelete()
    }

    private fun transferForSufficientBalance(source: UserData, target: UserData, transferAmount: BigDecimal) {
        val sourceAccountBalance = source.accountBalance!!
        val targetAccountBalance = target.accountBalance!!

        // check if target owes to source so that source can consider settlement and transfer less to target
        val targetOwedBalance = owedBalanceRepository.findBySourceUserDataIdAndTargetUserDataId(
            target.id!!, source.id!!
        )

        var targetOwedAmount = targetOwedBalance?.amount ?: BigDecimal.ZERO

        var amountToDeductFromSource = BigDecimal.ZERO
        var remainingAmount = transferAmount

        // if target owes to source then calculate amount to transfer from source to target and remaining owed amount
        if (targetOwedAmount > BigDecimal.ZERO) {
            LOGGER.info("Target account owes amount: $targetOwedAmount to source. Start settlement...")
            val amountToSettle = targetOwedAmount.min(remainingAmount)
            targetOwedAmount = targetOwedAmount.minus(amountToSettle)
            remainingAmount = remainingAmount.minus(amountToSettle)

            targetAccountBalance.owedAmount = targetOwedAmount
        }

        // if amount still remaining after settlement of above scenario, then add amount to target
        if (remainingAmount > BigDecimal.ZERO) {
            targetAccountBalance.updateAndSave(remainingAmount)
            amountToDeductFromSource = remainingAmount
            // update to track actual transfer details
            sourceAccountBalance.transferDetails = mutableMapOf(target.name to transferAmount)
        }

        // finally deduct amount from source
        sourceAccountBalance.updateAndSave(amountToDeductFromSource.negate())

        // update or delete own balance record based on remaining owed amount to source
        targetOwedBalance?.also {
            it.amount = targetOwedAmount
            it.saveOrDelete()
        }
    }

    private fun settle(sourceAccountBalance: AccountBalance, targetAccountBalance: AccountBalance) {
        var runningAmountToSettle = targetAccountBalance.owedAmount
        owedBalanceRepository.findBySourceUserDataId(targetAccountBalance.userData.id!!).forEach {
            with(it) {
                // source and target of transfer operation are already settled. No need to settle again.
                if (targetUserData.id == sourceAccountBalance.userData.id) {
                    return@forEach
                }
                if (runningAmountToSettle > BigDecimal.ZERO) {
                    LOGGER.info("Settle for owedBalance: $this.")
                    val targetSettleAmount = amount.min(runningAmountToSettle)

                    // if owe amount cannot be settled completely then update remaining owe amount
                    // otherwise delete entry
                    amount = amount.minus(targetSettleAmount)
                    saveOrDelete()

                    // deduct balance and owed amount of transfer request's target account
                    targetAccountBalance.owedAmount = targetAccountBalance.owedAmount.minus(targetSettleAmount)
                    targetAccountBalance.updateAndSave(targetSettleAmount.negate())

                    // increase balance corresponding owing account of transfer request's target account
                    targetUserData.accountBalance!!.updateAndSave(targetSettleAmount)

                    // update to track remaining amount that can be used for further settlements
                    runningAmountToSettle = runningAmountToSettle.minus(targetSettleAmount)
                }
            }
        }
    }

    private fun OwedBalance.saveOrDelete() {
        if (this.amount.compareTo(BigDecimal.ZERO) == 0) {
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