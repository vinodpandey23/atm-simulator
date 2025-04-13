package com.atm.simulator.repository

import com.atm.simulator.model.AccountBalance
import org.springframework.data.jpa.repository.JpaRepository

interface AccountBalanceRepository : JpaRepository<AccountBalance, Long>