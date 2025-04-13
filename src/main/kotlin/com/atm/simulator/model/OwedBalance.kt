package com.atm.simulator.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
class OwedBalance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_user_id", nullable = false, updatable = false)
    val sourceUserData: UserData,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false, updatable = false)
    val targetUserData: UserData,
    var amount: BigDecimal,
    var updatedDateTime: Instant? = null
) {
    @PrePersist
    @PreUpdate
    fun prePersist() {
        updatedDateTime = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OwedBalance) return false

        if (id != other.id) return false
        if (sourceUserData != other.sourceUserData) return false
        if (targetUserData != other.targetUserData) return false
        if (amount != other.amount) return false
        if (updatedDateTime != other.updatedDateTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + sourceUserData.hashCode()
        result = 31 * result + targetUserData.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + (updatedDateTime?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "OwedBalance(id=$id, sourceUserData=${sourceUserData.id}, targetUserData=${targetUserData.id}, " +
                "amount=$amount, updatedDateTime=$updatedDateTime)"
    }
}