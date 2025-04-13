package com.atm.simulator.integration

import com.atm.simulator.api.DepositDto
import com.atm.simulator.api.TransferDto
import com.atm.simulator.api.UserDto
import com.atm.simulator.api.WithdrawDto
import com.atm.simulator.controller.AtmController
import com.atm.simulator.enums.ResponseCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class AtmControllerEndToEndTest2 {

    @Autowired
    private lateinit var atmController: AtmController

    @Test
    fun `should perform deposit, transfer, withdraw operations among three users with owed amount scenarios`() {
        // David logs in and deposits $100
        with(atmController.loginUser(UserDto(name = "David"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, David!", "Your balance is $0.00"), messages)
        }
        with(atmController.depositAmount(DepositDto(name = "David", amount = BigDecimal(100)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Your balance is $100.00"), messages)
        }
        with(atmController.logoutUser(UserDto(name = "David"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, David!"), messages)
        }

        // Eve logs in and deposits $50
        with(atmController.loginUser(UserDto(name = "Eve"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, Eve!", "Your balance is $0.00"), messages)
        }
        with(atmController.depositAmount(DepositDto(name = "Eve", amount = BigDecimal(50)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Your balance is $50.00"), messages)
        }

        // Eve tries to transfer $80 to David (exceeds balance, so he owes David)
        with(atmController.transferAmount(TransferDto(name = "Eve", target = "David", amount = BigDecimal(80)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(
                listOf("Transferred $50.00 to David", "Your balance is $0.00", "Owed $30.00 to David"),
                messages
            )
        }
        with(atmController.logoutUser(UserDto(name = "Eve"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, Eve!"), messages)
        }

        // Frank logs in and deposits $120
        with(atmController.loginUser(UserDto(name = "Frank"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, Frank!", "Your balance is $0.00"), messages)
        }
        with(atmController.depositAmount(DepositDto(name = "Frank", amount = BigDecimal(120)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Your balance is $120.00"), messages)
        }

        // Frank transfers $50 to Eve
        with(atmController.transferAmount(TransferDto(name = "Frank", target = "Eve", amount = BigDecimal(50)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Transferred $50.00 to Eve", "Your balance is $70.00"), messages)
        }
        with(atmController.logoutUser(UserDto(name = "Frank"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, Frank!"), messages)
        }

        // David logs in and withdraws $70 (try insufficient balance error also)
        with(atmController.loginUser(UserDto(name = "David"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, David!", "Your balance is $180.00"), messages)
        }
        with(atmController.withdrawAmount(WithdrawDto(name = "David", amount = BigDecimal(70)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Collect cash", "Your balance is $110.00"), messages)
        }
        with(atmController.withdrawAmount(WithdrawDto(name = "David", amount = BigDecimal(200)))) {
            assertEquals(ResponseCode.ERROR, code)
            assertEquals(listOf("Insufficient balance."), messages)
        }
        with(atmController.logoutUser(UserDto(name = "David"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, David!"), messages)
        }

        // Eve logs in and deposits $40
        with(atmController.loginUser(UserDto(name = "Eve"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, Eve!", "Your balance is $20.00"), messages)
        }
        with(atmController.depositAmount(DepositDto(name = "Eve", amount = BigDecimal(40)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(
                listOf("Your balance is $60.00"),
                messages
            )
        }

        // Eve withdraws $30
        with(atmController.withdrawAmount(WithdrawDto(name = "Eve", amount = BigDecimal(30)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Collect cash", "Your balance is $30.00"), messages)
        }
        with(atmController.logoutUser(UserDto(name = "Eve"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, Eve!"), messages)
        }

        // Frank logs in and transfers another $20 to David
        with(atmController.loginUser(UserDto(name = "Frank"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, Frank!", "Your balance is $70.00"), messages)
        }
        with(atmController.transferAmount(TransferDto(name = "Frank", target = "David", amount = BigDecimal(20)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Transferred $20.00 to David", "Your balance is $50.00"), messages)
        }
        with(atmController.logoutUser(UserDto(name = "Frank"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, Frank!"), messages)
        }
    }
}