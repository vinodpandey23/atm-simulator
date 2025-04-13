package com.atm.simulator.integration

import com.atm.simulator.api.DepositDto
import com.atm.simulator.api.TransferDto
import com.atm.simulator.api.UserDto
import com.atm.simulator.controller.AtmController
import com.atm.simulator.enums.ResponseCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class AtmControllerEndToEndTest1 {

    @Autowired
    private lateinit var atmController: AtmController

    @Test
    fun `should perform deposit and transfer operation between two users and include owe amount scenario also`() {
        with(atmController.loginUser(UserDto(name = "Alice"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, Alice!", "Your balance is $0.00"), messages)
        }

        with(atmController.depositAmount(DepositDto(name = "Alice", amount = BigDecimal(100)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Your balance is $100.00"), messages)
        }

        with(atmController.logoutUser(UserDto(name = "Alice"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, Alice!"), messages)
        }

        with(atmController.loginUser(UserDto(name = "Bob"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, Bob!", "Your balance is $0.00"), messages)
        }

        with(atmController.depositAmount(DepositDto(name = "Bob", amount = BigDecimal(80)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Your balance is $80.00"), messages)
        }

        with(atmController.transferAmount(TransferDto(name = "Bob", target = "Alice", amount = BigDecimal(50)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Transferred $50.00 to Alice", "Your balance is $30.00"), messages)
        }

        with(atmController.transferAmount(TransferDto(name = "Bob", target = "Alice", amount = BigDecimal(100)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(
                listOf("Transferred $30.00 to Alice", "Your balance is $0.00", "Owed $70.00 to Alice"),
                messages
            )
        }

        with(atmController.depositAmount(DepositDto(name = "Bob", amount = BigDecimal(30)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(
                listOf("Transferred $30.00 to Alice", "Your balance is $0.00", "Owed $40.00 to Alice"),
                messages
            )
        }

        with(atmController.logoutUser(UserDto(name = "Bob"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, Bob!"), messages)
        }

        with(atmController.loginUser(UserDto(name = "Alice"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, Alice!", "Your balance is $210.00", "Owed $40.00 from Bob"), messages)
        }

        with(atmController.transferAmount(TransferDto(name = "Alice", target = "Bob", amount = BigDecimal(30)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(
                listOf("Your balance is $210.00", "Owed $10.00 from Bob"),
                messages
            )
        }

        with(atmController.logoutUser(UserDto(name = "Alice"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, Alice!"), messages)
        }

        with(atmController.loginUser(UserDto(name = "Bob"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Hello, Bob!", "Your balance is $0.00", "Owed $10.00 to Alice"), messages)
        }

        with(atmController.depositAmount(DepositDto(name = "Bob", amount = BigDecimal(100)))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(
                listOf("Transferred $10.00 to Alice", "Your balance is $90.00"),
                messages
            )
        }

        with(atmController.logoutUser(UserDto(name = "Bob"))) {
            assertEquals(ResponseCode.SUCCESS, code)
            assertEquals(listOf("Goodbye, Bob!"), messages)
        }
    }
}