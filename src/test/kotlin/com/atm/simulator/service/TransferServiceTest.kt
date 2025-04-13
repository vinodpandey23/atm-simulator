import com.atm.simulator.api.TransferDto
import com.atm.simulator.exception.InvalidRequestException
import com.atm.simulator.model.AccountBalance
import com.atm.simulator.model.OwedBalance
import com.atm.simulator.model.UserData
import com.atm.simulator.repository.AccountBalanceRepository
import com.atm.simulator.repository.OwedBalanceRepository
import com.atm.simulator.repository.UserDataRepository
import com.atm.simulator.service.TransferService
import com.atm.simulator.util.*
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import kotlin.test.assertFailsWith
import kotlin.test.assertNull


class TransferServiceTest {
    private lateinit var accountBalanceRepository: AccountBalanceRepository
    private lateinit var owedBalanceRepository: OwedBalanceRepository
    private lateinit var userDataRepository: UserDataRepository

    private lateinit var transferService: TransferService

    @BeforeEach
    fun setup() {
        accountBalanceRepository = mock {
            on { save(any()) } doAnswer { invocationOnMock -> invocationOnMock.getArgument<AccountBalance>(0) }
        }
        owedBalanceRepository = mock {
            on { save(any()) } doAnswer { invocationOnMock -> invocationOnMock.getArgument<OwedBalance>(0) }
        }
        userDataRepository = mock()
        transferService = TransferService(accountBalanceRepository, owedBalanceRepository, userDataRepository)
    }

    @Test
    fun `should transfer amount when source has sufficient balance and target also does not owe to source`() {
        val initialSourceAccountBalance = BigDecimal(500)
        val initialTargetAccountBalance = BigDecimal(200)

        val transferAmount = BigDecimal(100)

        val finalSourceAccountBalance = BigDecimal(400)
        val finalTargetAccountBalance = BigDecimal(300)

        val sourceUserData = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = initialSourceAccountBalance)
        }
        val targetUserData = UserData(id = TARGET_USER_ID, name = TARGET_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = initialTargetAccountBalance)
        }

        val transferDto = TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = transferAmount)

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(sourceUserData)
        whenever(userDataRepository.findByName(TARGET_USER)).thenReturn(targetUserData)
        whenever(
            owedBalanceRepository.findBySourceUserDataIdAndTargetUserDataId(
                TARGET_USER_ID,
                SOURCE_USER_ID
            )
        ).thenReturn(null)
        whenever(owedBalanceRepository.findByTargetUserDataId(SOURCE_USER_ID)).thenReturn(emptyList())

        transferService.processTransfer(transferDto).also {
            assertEquals(finalSourceAccountBalance, it.balance)
            assertNull(it.owedToDetails)
        }

        argumentCaptor<String>().apply {
            verify(userDataRepository, times(2)).findByName(capture())
            assertEquals(SOURCE_USER, firstValue)
            assertEquals(TARGET_USER, secondValue)
        }
        argumentCaptor<AccountBalance>().apply {
            verify(accountBalanceRepository, times(2)).save(capture())
            // target account balance update
            with(firstValue) {
                assertEquals(TARGET_USER, userData.name)
                assertEquals(finalTargetAccountBalance, balance)
            }
            // source account balance update
            with(secondValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(finalSourceAccountBalance, balance)
            }
        }
        verify(owedBalanceRepository).findBySourceUserDataIdAndTargetUserDataId(TARGET_USER_ID, SOURCE_USER_ID)
        verify(owedBalanceRepository).findByTargetUserDataId(SOURCE_USER_ID)
    }

    @Test
    fun `should transfer amount when source has sufficient balance and target owes to source (less than transfer amount)`() {
        val initialSourceAccountBalance = BigDecimal(500)
        val initialTargetAccountBalance = BigDecimal(0)
        val initialTargetOwedAmount = BigDecimal(50)

        val transferAmount = BigDecimal(100)

        val finalSourceAccountBalance = BigDecimal(450)
        val finalTargetAccountBalance = BigDecimal(50)
        val finalTargetOwedAmount = BigDecimal(0)

        val sourceUserData = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = initialSourceAccountBalance)
        }
        val targetUserData = UserData(id = TARGET_USER_ID, name = TARGET_USER).apply {
            accountBalance = AccountBalance(
                userData = this,
                balance = initialTargetAccountBalance,
                owedAmount = initialTargetOwedAmount
            )
        }

        val transferDto = TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = transferAmount)

        val targetOwedBalance = OwedBalance(
            id = 1,
            sourceUserData = targetUserData,
            targetUserData = sourceUserData,
            amount = initialTargetOwedAmount
        )

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(sourceUserData)
        whenever(userDataRepository.findByName(TARGET_USER)).thenReturn(targetUserData)
        whenever(
            owedBalanceRepository.findBySourceUserDataIdAndTargetUserDataId(
                TARGET_USER_ID,
                SOURCE_USER_ID
            )
        ).thenReturn(targetOwedBalance)
        whenever(owedBalanceRepository.findByTargetUserDataId(SOURCE_USER_ID)).thenReturn(emptyList())

        transferService.processTransfer(transferDto).also {
            assertEquals(finalSourceAccountBalance, it.balance)
            assertNull(it.owedToDetails)
        }

        argumentCaptor<String>().apply {
            verify(userDataRepository, times(2)).findByName(capture())
            assertEquals(SOURCE_USER, firstValue)
            assertEquals(TARGET_USER, secondValue)
        }
        argumentCaptor<AccountBalance>().apply {
            verify(accountBalanceRepository, times(2)).save(capture())
            // target account balance update
            with(firstValue) {
                assertEquals(TARGET_USER, userData.name)
                assertEquals(finalTargetAccountBalance, balance)
                assertEquals(finalTargetOwedAmount, owedAmount)
            }
            // source account balance update
            with(secondValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(finalSourceAccountBalance, balance)
            }
        }
        argumentCaptor<OwedBalance>().apply {
            verify(owedBalanceRepository).delete(capture())
            assertEquals(TARGET_USER_ID, firstValue.sourceUserData.id)
            assertEquals(SOURCE_USER_ID, firstValue.targetUserData.id)
        }
        verify(owedBalanceRepository).findBySourceUserDataIdAndTargetUserDataId(TARGET_USER_ID, SOURCE_USER_ID)
        verify(owedBalanceRepository).findByTargetUserDataId(SOURCE_USER_ID)
    }

    @Test
    fun `should transfer amount when source has sufficient balance and target owes to source (more than transfer amount)`() {
        val initialSourceAccountBalance = BigDecimal(500)
        val initialTargetAccountBalance = BigDecimal(0)
        val initialTargetOwedAmount = BigDecimal(200)

        val transferAmount = BigDecimal(100)

        val finalSourceAccountBalance = BigDecimal(500)
        val finalTargetAccountBalance = BigDecimal(0)
        val finalTargetOwedAmount = BigDecimal(100)

        val sourceUserData = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = initialSourceAccountBalance)
        }
        val targetUserData = UserData(id = TARGET_USER_ID, name = TARGET_USER).apply {
            accountBalance = AccountBalance(
                userData = this,
                balance = initialTargetAccountBalance,
                owedAmount = initialTargetOwedAmount
            )
        }

        val transferDto = TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = transferAmount)

        val targetOwedBalance = OwedBalance(
            id = 1,
            sourceUserData = targetUserData,
            targetUserData = sourceUserData,
            amount = initialTargetOwedAmount
        )

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(sourceUserData)
        whenever(userDataRepository.findByName(TARGET_USER)).thenReturn(targetUserData)
        whenever(
            owedBalanceRepository.findBySourceUserDataIdAndTargetUserDataId(
                TARGET_USER_ID,
                SOURCE_USER_ID
            )
        ).thenReturn(targetOwedBalance)
        whenever(owedBalanceRepository.findByTargetUserDataId(SOURCE_USER_ID)).thenReturn(emptyList())

        transferService.processTransfer(transferDto).also {
            assertEquals(finalSourceAccountBalance, it.balance)
            assertNull(it.owedToDetails)
        }

        argumentCaptor<String>().apply {
            verify(userDataRepository, times(2)).findByName(capture())
            assertEquals(SOURCE_USER, firstValue)
            assertEquals(TARGET_USER, secondValue)
        }
        argumentCaptor<AccountBalance>().apply {
            verify(accountBalanceRepository).save(capture())
            // source account balance update
            with(firstValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(finalSourceAccountBalance, balance)
            }
        }
        argumentCaptor<OwedBalance>().apply {
            verify(owedBalanceRepository).save(capture())
            assertEquals(TARGET_USER_ID, firstValue.sourceUserData.id)
            assertEquals(SOURCE_USER_ID, firstValue.targetUserData.id)
            assertEquals(finalTargetOwedAmount, firstValue.amount)
        }
        verify(owedBalanceRepository).findBySourceUserDataIdAndTargetUserDataId(TARGET_USER_ID, SOURCE_USER_ID)
        verify(owedBalanceRepository).findByTargetUserDataId(SOURCE_USER_ID)
        verify(owedBalanceRepository).findBySourceUserDataId(TARGET_USER_ID)
    }

    @Test
    fun `should transfer partial amount and track owed amount for source to target when source has sufficient balance and source also owes from some other user`() {
        val initialSourceAccountBalance = BigDecimal(100)
        val initialTargetAccountBalance = BigDecimal(100)

        val transferAmount = BigDecimal(200)
        val sourceOwedAmountToTarget = BigDecimal(100)

        val finalSourceAccountBalance = BigDecimal(0)
        val finalTargetAccountBalance = BigDecimal(200)

        val sourceUserData = UserData(id = SOURCE_USER_ID, name = SOURCE_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = initialSourceAccountBalance)
        }
        val targetUserData = UserData(id = TARGET_USER_ID, name = TARGET_USER).apply {
            accountBalance = AccountBalance(userData = this, balance = initialTargetAccountBalance)
        }

        val someOtherUserOwedAmount = BigDecimal.TEN
        val someOtherUserData = UserData(id = 3, name = "some user").apply {
            accountBalance = AccountBalance(userData = this, owedAmount = someOtherUserOwedAmount)
        }

        val transferDto = TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = transferAmount)

        val owedBalance = OwedBalance(
            id = 1,
            sourceUserData = someOtherUserData,
            targetUserData = sourceUserData,
            amount = someOtherUserOwedAmount
        )

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(sourceUserData)
        whenever(userDataRepository.findByName(TARGET_USER)).thenReturn(targetUserData)
        whenever(owedBalanceRepository.findByTargetUserDataId(SOURCE_USER_ID)).thenReturn(listOf(owedBalance))

        transferService.processTransfer(transferDto).also {
            assertEquals(finalSourceAccountBalance, it.balance)
            assertEquals(mutableMapOf(someOtherUserData.name to someOtherUserOwedAmount), it.owedFromDetails)
        }

        argumentCaptor<String>().apply {
            verify(userDataRepository, times(2)).findByName(capture())
            assertEquals(SOURCE_USER, firstValue)
            assertEquals(TARGET_USER, secondValue)
        }
        argumentCaptor<AccountBalance>().apply {
            verify(accountBalanceRepository, times(2)).save(capture())
            // target account balance update
            with(firstValue) {
                assertEquals(TARGET_USER, userData.name)
                assertEquals(finalTargetAccountBalance, balance)
            }
            // source account balance update
            with(secondValue) {
                assertEquals(SOURCE_USER, userData.name)
                assertEquals(finalSourceAccountBalance, balance)
                assertEquals(sourceOwedAmountToTarget, owedAmount)
            }
        }
        argumentCaptor<OwedBalance>().apply {
            verify(owedBalanceRepository).save(capture())
            with(firstValue) {
                assertEquals(SOURCE_USER_ID, sourceUserData.id)
                assertEquals(TARGET_USER_ID, targetUserData.id)
                assertEquals(sourceOwedAmountToTarget, amount)
            }
        }
        verify(owedBalanceRepository).findByTargetUserDataId(SOURCE_USER_ID)
    }

    @Test
    fun `should throw InvalidRequestException when target user does not exist`() {
        val sourceUserData = mock<UserData>()
        val transferDto = TransferDto(name = SOURCE_USER, target = TARGET_USER, amount = BigDecimal.TEN)

        whenever(userDataRepository.findByName(SOURCE_USER)).thenReturn(sourceUserData)
        whenever(userDataRepository.findByName(TARGET_USER)).thenReturn(null)

        assertFailsWith<InvalidRequestException>(message = TARGET_NOT_EXIST_MSG) {
            transferService.processTransfer(transferDto)
        }
        argumentCaptor<String>().apply {
            verify(userDataRepository, times(2)).findByName(capture())
            assertEquals(SOURCE_USER, firstValue)
            assertEquals(TARGET_USER, secondValue)
        }
    }

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(accountBalanceRepository, owedBalanceRepository, userDataRepository)
    }
}
