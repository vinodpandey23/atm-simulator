package com.atm.simulator.service

import com.atm.simulator.api.WithdrawDto
import com.atm.simulator.exception.InvalidRequestException
import com.atm.simulator.model.AccountBalance
import com.atm.simulator.model.UserData
import com.atm.simulator.repository.AccountBalanceRepository
import com.atm.simulator.repository.UserDataRepository
import com.atm.simulator.util.INSUFFICIENT_BALANCE_MSG
import com.atm.simulator.util.SOURCE_USER
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertFailsWith

class WithdrawServiceTest {

    private lateinit var accountBalanceRepository: AccountBalanceRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var withdrawService: WithdrawService

    @BeforeEach
    fun setUp() {
        accountBalanceRepository = mock {
            on { save(any()) } doAnswer { invocationOnMock -> invocationOnMock.getArgument<AccountBalance?>(0) }
        }
        userDataRepository = mock()
        withdrawService = WithdrawService(accountBalanceRepository, userDataRepository)
    }

    @Test
    fun `should deduct withdrawal amount from balance and return updated account balance when account balance is sufficient`() {
        val initialAccountBalance = BigDecimal("100.00")
        val withdrawAmount = BigDecimal("30.00")
        val remainingAmount = BigDecimal("70.00")

        val userData = UserData(name = SOURCE_USER).apply {
            accountBalance = AccountBalance(balance = initialAccountBalance, userData = this)
        }
        val withdrawDto = WithdrawDto(name = SOURCE_USER, amount = withdrawAmount)

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(userData)

        withdrawService.processWithdraw(withdrawDto).also {
            assertEquals(remainingAmount, it.balance)
        }

        verify(userDataRepository).findByName(SOURCE_USER)
        argumentCaptor<AccountBalance>().apply {
            verify(accountBalanceRepository).save(capture())
            with(firstValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(remainingAmount, balance)
            }
        }
    }

    @Test
    fun `should throw InvalidRequestException when account balance is insufficient`() {
        val initialAccountBalance = BigDecimal("30.00")
        val withdrawAmount = BigDecimal("50.00")

        val userData = UserData(name = SOURCE_USER).apply {
            accountBalance = AccountBalance(balance = initialAccountBalance, userData = this)
        }
        val withdrawDto = WithdrawDto(name = SOURCE_USER, amount = withdrawAmount)

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(userData)

        assertFailsWith<InvalidRequestException>(message = INSUFFICIENT_BALANCE_MSG) {
            withdrawService.processWithdraw(withdrawDto)
        }

        verify(userDataRepository).findByName(SOURCE_USER)
    }

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(accountBalanceRepository, userDataRepository)
    }
}
