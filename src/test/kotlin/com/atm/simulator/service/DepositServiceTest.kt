package com.atm.simulator.service

import com.atm.simulator.api.DepositDto
import com.atm.simulator.model.AccountBalance
import com.atm.simulator.model.OwedBalance
import com.atm.simulator.model.UserData
import com.atm.simulator.repository.AccountBalanceRepository
import com.atm.simulator.repository.OwedBalanceRepository
import com.atm.simulator.repository.UserDataRepository
import com.atm.simulator.util.SOURCE_USER
import com.atm.simulator.util.SOURCE_USER_ID
import com.atm.simulator.util.TARGET_USER
import com.atm.simulator.util.TARGET_USER_ID
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DepositServiceTest {

    private lateinit var accountBalanceRepository: AccountBalanceRepository
    private lateinit var owedBalanceRepository: OwedBalanceRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var depositService: DepositService

    @BeforeEach
    fun setUp() {
        accountBalanceRepository = mock {
            on { save(any()) } doAnswer { invocationOnMock -> invocationOnMock.getArgument<AccountBalance?>(0) }
        }
        owedBalanceRepository = mock()
        userDataRepository = mock()
        depositService = DepositService(
            accountBalanceRepository, owedBalanceRepository, userDataRepository
        )
    }

    @Test
    fun `should deposit full amount to source when source does not owe anything to other target`() {
        val initialSourceAccountBalance = BigDecimal("100.25")
        val depositAmount = BigDecimal("50.25")
        val finalSourceAccountBalance = BigDecimal("150.50")

        val userData = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = initialSourceAccountBalance)
        }
        val depositDto = DepositDto(name = SOURCE_USER, amount = depositAmount)

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(userData)

        depositService.processDeposit(depositDto).also {
            assertEquals(finalSourceAccountBalance, it.balance)
            assertNull(it.transferDetails)
            assertNull(it.owedToDetails)
        }

        argumentCaptor<AccountBalance>().apply {
            verify(accountBalanceRepository).save(capture())
            with(firstValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(finalSourceAccountBalance, balance)
                assertEquals(BigDecimal.ZERO, owedAmount)
            }
        }
        verify(userDataRepository).findByName(SOURCE_USER)
    }

    @Test
    fun `should deposit zero amount to source and settle partially to target when source owe amount more than the deposit amount`() {
        val initialTargetAccountBalance = BigDecimal("100.00")
        val initialAndFinalSourceAccountBalance = BigDecimal("0.00")
        val initialSourceOwedAmount = BigDecimal("70.00")

        val depositAmount = BigDecimal("50.00")

        val finalSourceOwedAmount = BigDecimal("20.00")
        val finalTargetAccountBalance = BigDecimal("150.00")

        val sourceUserData = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(
                userData = this,
                balance = initialAndFinalSourceAccountBalance,
                owedAmount = initialSourceOwedAmount
            )
        }

        val targetUserData = UserData(id = TARGET_USER_ID, name = TARGET_USER).apply {
            accountBalance = AccountBalance(
                userData = this,
                balance = initialTargetAccountBalance,
                owedAmount = BigDecimal.ZERO
            )
        }

        val depositDto = DepositDto(name = SOURCE_USER, amount = depositAmount)

        val owedBalance = OwedBalance(
            sourceUserData = sourceUserData,
            targetUserData = targetUserData,
            amount = initialSourceOwedAmount
        )

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(sourceUserData)
        whenever(owedBalanceRepository.findBySourceUserDataId(SOURCE_USER_ID)).thenReturn(listOf(owedBalance))

        depositService.processDeposit(depositDto).also {
            assertEquals(initialAndFinalSourceAccountBalance, it.balance)
            assertEquals(finalSourceOwedAmount, it.owedAmount)
            assertEquals(mapOf(TARGET_USER to depositAmount), it.transferDetails)
            assertEquals(mapOf(TARGET_USER to finalSourceOwedAmount), it.owedToDetails)
        }

        argumentCaptor<AccountBalance>().apply {
            verify(accountBalanceRepository, times(2)).save(capture())
            // target account balance update
            with(firstValue) {
                assertEquals(TARGET_USER, userData.name)
                assertEquals(finalTargetAccountBalance, balance)
                assertEquals(BigDecimal.ZERO, owedAmount)
            }
            // source account balance update
            with(secondValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(initialAndFinalSourceAccountBalance, balance)
                assertEquals(finalSourceOwedAmount, owedAmount)
            }
        }
        argumentCaptor<OwedBalance>().apply {
            verify(owedBalanceRepository).save(capture())
            assertEquals(SOURCE_USER_ID, firstValue.sourceUserData.id)
            assertEquals(TARGET_USER_ID, firstValue.targetUserData.id)
            assertEquals(finalSourceOwedAmount, firstValue.amount)
        }
        verify(userDataRepository).findByName(SOURCE_USER)
        verify(owedBalanceRepository, times(2)).findBySourceUserDataId(SOURCE_USER_ID)
    }

    @Test
    fun `should deposit partial amount to source and settle fully to target when source owe amount less than the deposit amount`() {
        val initialTargetAccountBalance = BigDecimal("100.00")
        val initialSourceOwedAmount = BigDecimal("50.00")

        val depositAmount = BigDecimal("70.00")

        val finalSourceOwedAmount = BigDecimal("0.00")
        val finalSourceAccountBalance = BigDecimal("20.00")
        val finalTargetAccountBalance = BigDecimal("150.00")

        val sourceUserData = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(
                userData = this,
                balance = BigDecimal.ZERO,
                owedAmount = initialSourceOwedAmount
            )
        }

        val targetUserData = UserData(id = TARGET_USER_ID, name = TARGET_USER).apply {
            accountBalance = AccountBalance(
                userData = this,
                balance = initialTargetAccountBalance,
                owedAmount = BigDecimal.ZERO
            )
        }

        val depositDto = DepositDto(name = SOURCE_USER, amount = depositAmount)

        val owedBalance = OwedBalance(
            id = 1,
            sourceUserData = sourceUserData,
            targetUserData = targetUserData,
            amount = initialSourceOwedAmount
        )

        whenever(userDataRepository.findByName(depositDto.name!!)).thenReturn(sourceUserData)
        whenever(owedBalanceRepository.findBySourceUserDataId(sourceUserData.id!!)).thenReturn(listOf(owedBalance))

        depositService.processDeposit(depositDto).also {
            assertEquals(finalSourceAccountBalance, it.balance)
            assertEquals(finalSourceOwedAmount, it.owedAmount)
            assertEquals(mapOf(TARGET_USER to initialSourceOwedAmount), it.transferDetails)
            assertEquals(mapOf(TARGET_USER to finalSourceOwedAmount), it.owedToDetails)
        }

        argumentCaptor<AccountBalance>().apply {
            verify(accountBalanceRepository, times(2)).save(capture())
            // target account balance update
            with(firstValue) {
                assertEquals(TARGET_USER, userData.name)
                assertEquals(finalTargetAccountBalance, balance)
                assertEquals(BigDecimal.ZERO, owedAmount)
            }
            // source account balance update
            with(secondValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(finalSourceAccountBalance, balance)
                assertEquals(finalSourceOwedAmount, owedAmount)
            }
        }
        argumentCaptor<OwedBalance>().apply {
            verify(owedBalanceRepository).delete(capture())
            assertEquals(SOURCE_USER_ID, firstValue.sourceUserData.id)
            assertEquals(TARGET_USER_ID, firstValue.targetUserData.id)
        }
        verify(userDataRepository).findByName(SOURCE_USER)
        verify(owedBalanceRepository, times(2)).findBySourceUserDataId(SOURCE_USER_ID)
    }

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(accountBalanceRepository, owedBalanceRepository, userDataRepository)
    }
}
