package com.atm.simulator.repository

import com.atm.simulator.model.OwedBalance
import org.springframework.data.jpa.repository.JpaRepository

interface OwedBalanceRepository : JpaRepository<OwedBalance, Long> {
    fun findBySourceUserDataId(sourceUserId: Long): List<OwedBalance>
    fun findByTargetUserDataId(targetUserId: Long): List<OwedBalance>
    fun findBySourceUserDataIdAndTargetUserDataId(sourceUserId: Long, targetUserId: Long): OwedBalance?
}