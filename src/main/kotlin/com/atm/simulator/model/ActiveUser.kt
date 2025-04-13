package com.atm.simulator.model

import jakarta.persistence.*
import java.time.Instant

@Entity
class ActiveUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val userData: UserData,
    var createdDateTime: Instant? = null
) {
    @PrePersist
    fun prePersist() {
        createdDateTime = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActiveUser) return false

        if (id != other.id) return false
        if (userData != other.userData) return false
        if (createdDateTime != other.createdDateTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + userData.hashCode()
        result = 31 * result + (createdDateTime?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ActiveUser(id=$id, userData=${userData.id}, createdDateTime=$createdDateTime)"
    }
}