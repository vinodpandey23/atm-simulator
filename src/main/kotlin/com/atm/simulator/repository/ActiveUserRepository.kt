package com.atm.simulator.repository

import com.atm.simulator.model.ActiveUser
import org.springframework.data.jpa.repository.JpaRepository

interface ActiveUserRepository : JpaRepository<ActiveUser, Long>