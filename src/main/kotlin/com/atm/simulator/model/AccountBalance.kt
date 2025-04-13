package com.atm.simulator.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
class AccountBalance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val userData: UserData,
    var balance: BigDecimal = BigDecimal.ZERO,
    var owedAmount: BigDecimal = BigDecimal.ZERO,
    var updatedDateTime: Instant? = null,
    @Transient
    var transferDetails: Map<String, BigDecimal>? = null,
    @Transient
    var owedFromDetails: Map<String, BigDecimal>? = null,
    @Transient
    var owedToDetails: Map<String, BigDecimal>? = null
) {
    @PrePersist
    @PreUpdate
    fun prePersist() {
        updatedDateTime = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountBalance) return false

        if (id != other.id) return false
        if (userData != other.userData) return false
        if (balance != other.balance) return false
        if (owedAmount != other.owedAmount) return false
        if (updatedDateTime != other.updatedDateTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + userData.hashCode()
        result = 31 * result + balance.hashCode()
        result = 31 * result + owedAmount.hashCode()
        result = 31 * result + (updatedDateTime?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "AccountBalance(id=$id, userData=${userData.id}, balance=$balance, owedAmount=$owedAmount, " +
                "updatedDateTime=$updatedDateTime, transferDetails=$transferDetails, " +
                "owedFromDetails=$owedFromDetails, owedToDetails=$owedToDetails)"
    }
}