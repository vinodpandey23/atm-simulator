package com.atm.simulator.service

import com.atm.simulator.api.UserDto
import com.atm.simulator.exception.InvalidRequestException
import com.atm.simulator.model.AccountBalance
import com.atm.simulator.model.ActiveUser
import com.atm.simulator.model.OwedBalance
import com.atm.simulator.model.UserData
import com.atm.simulator.repository.ActiveUserRepository
import com.atm.simulator.repository.OwedBalanceRepository
import com.atm.simulator.repository.UserDataRepository
import com.atm.simulator.util.SOURCE_USER
import com.atm.simulator.util.SOURCE_USER_ID
import com.atm.simulator.util.USER_ALREADY_LOGGED_IN_MSG
import com.atm.simulator.util.USER_NOT_LOGGED_IN_MSG
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertFailsWith

class UserServiceTest {

    private lateinit var activeUserRepository: ActiveUserRepository
    private lateinit var owedBalanceRepository: OwedBalanceRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        activeUserRepository = mock()
        owedBalanceRepository = mock()
        userDataRepository = mock {
            on { save(any()) } doAnswer { invocationOnMock ->
                invocationOnMock.getArgument<UserData?>(0).apply { id = SOURCE_USER_ID }
            }
        }
        userService = UserService(activeUserRepository, owedBalanceRepository, userDataRepository)
    }

    @Test
    fun `should create user and record active user when existing user not found - login`() {
        val userDto = UserDto(name = SOURCE_USER)

        whenever(userDataRepository.findByName(userDto.name!!)).thenReturn(null)
        whenever(owedBalanceRepository.findBySourceUserDataId(SOURCE_USER_ID)).thenReturn(emptyList())
        whenever(owedBalanceRepository.findByTargetUserDataId(SOURCE_USER_ID)).thenReturn(emptyList())

        userService.processLogin(userDto).also {
            assertEquals(SOURCE_USER, it.name)
            assertEquals(BigDecimal.ZERO, it.accountBalance!!.balance)
        }

        verify(userDataRepository).findByName(SOURCE_USER)
        argumentCaptor<UserData>().apply {
            verify(userDataRepository).save(capture())
            with(firstValue) {
                assertEquals(SOURCE_USER, name)
                assertEquals(BigDecimal.ZERO, accountBalance?.balance)
                assertNotNull(activeUser)
            }
        }
        verify(owedBalanceRepository).findBySourceUserDataId(SOURCE_USER_ID)
        verify(owedBalanceRepository).findByTargetUserDataId(SOURCE_USER_ID)
    }

    @Test
    fun `should record active user when existing user found - login`() {
        val balance = BigDecimal("50.00")
        val userDto = UserDto(name = SOURCE_USER)
        val existingUser = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = balance)
        }

        whenever(userDataRepository.findByName(userDto.name!!)).thenReturn(existingUser)
        whenever(owedBalanceRepository.findBySourceUserDataId(SOURCE_USER_ID)).thenReturn(emptyList())
        whenever(owedBalanceRepository.findByTargetUserDataId(SOURCE_USER_ID)).thenReturn(emptyList())

        userService.processLogin(userDto).also {
            assertEquals(SOURCE_USER, it.name)
            assertEquals(balance, it.accountBalance!!.balance)
        }

        verify(userDataRepository).findByName(SOURCE_USER)
        argumentCaptor<ActiveUser>().apply {
            verify(activeUserRepository).save(capture())
            with(firstValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(balance, userData.accountBalance?.balance)
            }
        }
        verify(owedBalanceRepository).findBySourceUserDataId(SOURCE_USER_ID)
        verify(owedBalanceRepository).findByTargetUserDataId(SOURCE_USER_ID)
    }

    @Test
    fun `should throw InvalidRequestException when user already recorded as active - login`() {
        val userDto = UserDto(name = SOURCE_USER)
        val existingUser = UserData(name = SOURCE_USER).apply {
            activeUser = ActiveUser(userData = this)
        }

        whenever(userDataRepository.findByName(userDto.name!!)).thenReturn(existingUser)

        assertFailsWith<InvalidRequestException>(message = USER_ALREADY_LOGGED_IN_MSG) {
            userService.processLogin(userDto)
        }

        verify(userDataRepository).findByName(SOURCE_USER)
    }

    @Test
    fun `should set owedTo details and owedFrom details when existing user owes to and from others - login`() {
        val userDto = UserDto(name = SOURCE_USER)
        val existingUser = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = BigDecimal.ZERO)
        }

        val oweToDetails = listOf(
            OwedBalance(
                sourceUserData = existingUser,
                targetUserData = UserData(id = 11, name = "USER1"),
                amount = BigDecimal("11.00")
            ),
            OwedBalance(
                sourceUserData = existingUser,
                targetUserData = UserData(id = 12, name = "USER2"),
                amount = BigDecimal("12.00")
            )
        )

        val oweFromDetails = listOf(
            OwedBalance(
                sourceUserData = UserData(id = 13, name = "USER3"),
                targetUserData = existingUser,
                amount = BigDecimal("13.00")
            ),
            OwedBalance(
                sourceUserData = UserData(id = 14, name = "USER4"),
                targetUserData = existingUser,
                amount = BigDecimal("14.00")
            )
        )

        whenever(userDataRepository.findByName(userDto.name!!)).thenReturn(existingUser)
        whenever(owedBalanceRepository.findBySourceUserDataId(SOURCE_USER_ID)).thenReturn(oweToDetails)
        whenever(owedBalanceRepository.findByTargetUserDataId(SOURCE_USER_ID)).thenReturn(oweFromDetails)

        val oweToDetailsMap = mapOf("USER1" to BigDecimal("11.00"), "USER2" to BigDecimal("12.00"))
        val oweFromDetailsMap = mapOf("USER3" to BigDecimal("13.00"), "USER4" to BigDecimal("14.00"))

        userService.processLogin(userDto).also {
            assertEquals(SOURCE_USER, it.name)
            assertEquals(BigDecimal.ZERO, it.accountBalance!!.balance)
            assertEquals(oweToDetailsMap, it.accountBalance!!.owedToDetails)
            assertEquals(oweFromDetailsMap, it.accountBalance!!.owedFromDetails)
        }

        verify(userDataRepository).findByName(SOURCE_USER)
        argumentCaptor<ActiveUser>().apply {
            verify(activeUserRepository).save(capture())
            with(firstValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(BigDecimal.ZERO, userData.accountBalance?.balance)
            }
        }
        verify(owedBalanceRepository).findBySourceUserDataId(SOURCE_USER_ID)
        verify(owedBalanceRepository).findByTargetUserDataId(SOURCE_USER_ID)
    }

    @Test
    fun `should delete from active user when user is logged in - logout`() {
        val userDto = UserDto(name = SOURCE_USER)
        val existingUser = UserData(name = SOURCE_USER).apply {
            activeUser = ActiveUser(userData = this)
        }

        whenever(userDataRepository.findByName(userDto.name!!)).thenReturn(existingUser)

        userService.processLogout(userDto).also {
            assertEquals(SOURCE_USER, it.name)
        }

        verify(userDataRepository).findByName(SOURCE_USER)
        verify(activeUserRepository).delete(existingUser.activeUser!!)
    }

    @Test
    fun `should throw InvalidRequestException when user is not logged in - logout`() {
        val userDto = UserDto(name = SOURCE_USER)

        whenever(userDataRepository.findByName(userDto.name!!)).thenReturn(null)

        assertFailsWith<InvalidRequestException>(message = USER_NOT_LOGGED_IN_MSG) {
            userService.processLogout(userDto)
        }

        verify(userDataRepository).findByName(SOURCE_USER)
    }

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(activeUserRepository, owedBalanceRepository, userDataRepository)
    }
}
