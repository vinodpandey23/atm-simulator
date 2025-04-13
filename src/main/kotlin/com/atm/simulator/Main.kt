package com.atm.simulator

import com.atm.simulator.util.INTERNAL_SERVER_ERROR_MSG
import com.atm.simulator.util.PrintUtilities
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import kotlin.system.exitProcess

@SpringBootApplication
@EnableAspectJAutoProxy
open class Main

fun main(args: Array<String>) {
    try {
        runApplication<Main>(*args).getBean(AtmSimulatorApp::class.java).start()
    } catch (ex: Exception) {
        PrintUtilities.printError(INTERNAL_SERVER_ERROR_MSG).also {
            exitProcess(0)
        }
    }
}