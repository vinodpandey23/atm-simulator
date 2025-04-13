package com.atm.simulator.controller

import com.atm.simulator.api.*
import com.atm.simulator.enums.ResponseCode
import com.atm.simulator.model.AccountBalance
import com.atm.simulator.model.UserData
import com.atm.simulator.service.DepositService
import com.atm.simulator.service.TransferService
import com.atm.simulator.service.UserService
import com.atm.simulator.service.WithdrawService
import com.atm.simulator.util.SOURCE_USER
import com.atm.simulator.util.TARGET_USER
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AtmControllerTest {

    private lateinit var depositService: DepositService
    private lateinit var transferService: TransferService
    private lateinit var userService: UserService
    private lateinit var withdrawService: WithdrawService

    private lateinit var atmController: AtmController

    @BeforeEach
    fun setUp() {
        depositService = mock()
        transferService = mock()
        userService = mock()
        withdrawService = mock()
        atmController = AtmController(depositService, transferService, userService, withdrawService)
    }

    @Test
    fun `should return success with balance only when user does not owe from or to others - login`() {
        val userDto = UserDto(name = SOURCE_USER)
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("10.50"))
        }
        whenever(userService.processLogin(userDto)).thenReturn(userData)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf("Hello, $SOURCE_USER!", "Your balance is $10.50")
        )
        val actual = atmController.loginUser(userDto)

        assertEquals(expected, actual)

        verify(userService).processLogin(userDto)
    }

    @Test
    fun `should return success with balance and owedFrom details when user owes from others - login`() {
        val owesFrom1 = TARGET_USER
        val owesFrom2 = TARGET_USER.repeat(2)

        val userDto = UserDto(name = SOURCE_USER)
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("10.50"))
        }
        userData.accountBalance?.apply {
            owedFromDetails = mutableMapOf(owesFrom1 to BigDecimal("35.20"), owesFrom2 to BigDecimal("10.70"))
        }

        whenever(userService.processLogin(userDto)).thenReturn(userData)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf(
                "Hello, $SOURCE_USER!",
                "Your balance is $10.50",
                "Owed $35.20 from $owesFrom1",
                "Owed $10.70 from $owesFrom2"
            )
        )
        val actual = atmController.loginUser(userDto)

        assertEquals(expected, actual)

        verify(userService).processLogin(userDto)
    }

    @Test
    fun `should return success with balance and owedTo details when user owes to others - login`() {
        val owesTo1 = TARGET_USER
        val owesTo2 = TARGET_USER.repeat(2)

        val userDto = UserDto(name = SOURCE_USER)
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal.ZERO)
        }
        userData.accountBalance?.apply {
            owedToDetails = mutableMapOf(owesTo1 to BigDecimal("35.20"), owesTo2 to BigDecimal("10.70"))
        }

        whenever(userService.processLogin(userDto)).thenReturn(userData)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf(
                "Hello, $SOURCE_USER!",
                "Your balance is $0.00",
                "Owed $35.20 to $owesTo1",
                "Owed $10.70 to $owesTo2"
            )
        )
        val actual = atmController.loginUser(userDto)

        assertEquals(expected, actual)

        verify(userService).processLogin(userDto)
    }

    @Test
    fun `should return success when user logs out`() {
        val userDto = UserDto(name = SOURCE_USER)
        val userData = UserData(name = SOURCE_USER)
        whenever(userService.processLogout(userDto)).thenReturn(userData)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf("Goodbye, $SOURCE_USER!")
        )
        val actual = atmController.logoutUser(userDto)

        assertEquals(expected, actual)

        verify(userService).processLogout(userDto)
    }

    @Test
    fun `should return success with balance only when user does not owe from or to others - deposit`() {
        val depositDto = DepositDto(name = SOURCE_USER, amount = BigDecimal("10.50"))
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("50.50"))
        }

        whenever(depositService.processDeposit(depositDto)).thenReturn(userData.accountBalance)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf("Your balance is $50.50")
        )
        val actual = atmController.depositAmount(depositDto)

        assertEquals(expected, actual)

        verify(depositService).processDeposit(depositDto)
    }

    @Test
    fun `should return success with balance and transferred details when user owes to others and deposit amount is enough to settle - deposit`() {
        val owesTo1 = TARGET_USER
        val owesTo2 = TARGET_USER.repeat(2)

        val depositDto = DepositDto(name = SOURCE_USER, amount = BigDecimal("50.50"))
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("10.50"))
        }
        userData.accountBalance?.apply {
            transferDetails = mutableMapOf(owesTo1 to BigDecimal("30.00"), owesTo2 to BigDecimal("10.00"))
        }

        whenever(depositService.processDeposit(depositDto)).thenReturn(userData.accountBalance)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf(
                "Transferred $30.00 to $owesTo1",
                "Transferred $10.00 to $owesTo2",
                "Your balance is $10.50"
            )
        )
        val actual = atmController.depositAmount(depositDto)

        assertEquals(expected, actual)

        verify(depositService).processDeposit(depositDto)
    }

    @Test
    fun `should return success with balance, transferred details and owedTo details when user owes to others and deposit amount is not enough to settle - deposit`() {
        val owesTo1 = TARGET_USER
        val owesTo2 = TARGET_USER.repeat(2)

        val depositDto = DepositDto(name = SOURCE_USER, amount = BigDecimal("50.50"))
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("00.00"))
        }
        userData.accountBalance?.apply {
            transferDetails = mutableMapOf(owesTo1 to BigDecimal("30.00"), owesTo2 to BigDecimal("20.50"))
            owedToDetails = mutableMapOf(owesTo2 to BigDecimal("10.50"))
        }

        whenever(depositService.processDeposit(depositDto)).thenReturn(userData.accountBalance)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf(
                "Transferred $30.00 to $owesTo1",
                "Transferred $20.50 to $owesTo2",
                "Your balance is $0.00",
                "Owed $10.50 to $owesTo2"
            )
        )
        val actual = atmController.depositAmount(depositDto)

        assertEquals(expected, actual)

        verify(depositService).processDeposit(depositDto)
    }

    @Test
    fun `should return success with balance only when balance is more than requested withdrawal amount - withdraw`() {
        val withdrawDto = WithdrawDto(name = SOURCE_USER, amount = BigDecimal.TEN)
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("50.79"))
        }
        whenever(withdrawService.processWithdraw(withdrawDto)).thenReturn(userData.accountBalance)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf("Collect cash", "Your balance is $50.79")
        )
        val actual = atmController.withdrawAmount(withdrawDto)

        assertEquals(expected, actual)

        verify(withdrawService).processWithdraw(withdrawDto)
    }

    @Test
    fun `should return success with balance and transfer details when balance is enough for full transfer - transfer`() {
        val transferDto = TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal("10.50"))
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("50.50"))
        }
        userData.accountBalance?.apply {
            transferDetails = mutableMapOf(TARGET_USER to BigDecimal("10.50"))
        }

        whenever(transferService.processTransfer(transferDto)).thenReturn(userData.accountBalance)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf("Transferred $10.50 to $TARGET_USER", "Your balance is $50.50")
        )
        val actual = atmController.transferAmount(transferDto)

        assertEquals(expected, actual)

        verify(transferService).processTransfer(transferDto)
    }

    @Test
    fun `should return success with balance, transfer details and remaining owedTo details when balance is not enough for full amount - transfer`() {
        val transferDto = TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal("50.50"))
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("0.00"))
        }
        userData.accountBalance?.apply {
            transferDetails = mutableMapOf(TARGET_USER to BigDecimal("50.50"))
            owedAmount = BigDecimal("10.20")
        }

        whenever(transferService.processTransfer(transferDto)).thenReturn(userData.accountBalance)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf(
                "Transferred $50.50 to $TARGET_USER",
                "Your balance is $0.00",
                "Owed $10.20 to $TARGET_USER"
            )
        )
        val actual = atmController.transferAmount(transferDto)

        assertEquals(expected, actual)

        verify(transferService).processTransfer(transferDto)
    }

    @Test
    fun `should return success with balance, transfer details and owedFrom details when user owed from others - transfer`() {
        val owesFrom1 = TARGET_USER.repeat(2)
        val owesFrom2 = TARGET_USER.repeat(3)

        val transferDto = TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal("50.50"))
        val userData = UserData(name = SOURCE_USER)
        userData.apply {
            accountBalance = AccountBalance(userData = userData, balance = BigDecimal("10.80"))
        }
        userData.accountBalance?.apply {
            transferDetails = mutableMapOf(TARGET_USER to BigDecimal("50.50"))
            owedFromDetails = mutableMapOf(owesFrom1 to BigDecimal("10.50"), owesFrom2 to BigDecimal("20.00"))
        }

        whenever(transferService.processTransfer(transferDto)).thenReturn(userData.accountBalance)

        val expected = Response(
            code = ResponseCode.SUCCESS,
            messages = listOf(
                "Transferred $50.50 to $TARGET_USER",
                "Your balance is $10.80",
                "Owed $10.50 from $owesFrom1",
                "Owed $20.00 from $owesFrom2",
            )
        )
        val actual = atmController.transferAmount(transferDto)

        assertEquals(expected, actual)

        verify(transferService).processTransfer(transferDto)
    }

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(depositService, transferService, userService, withdrawService)
    }
}