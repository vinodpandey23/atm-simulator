package com.atm.simulator.repository

import com.atm.simulator.model.UserData
import org.springframework.data.jpa.repository.JpaRepository

interface UserDataRepository : JpaRepository<UserData, Long> {
    fun findByName(name: String): UserData?
}