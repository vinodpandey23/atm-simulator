package com.atm.simulator.model

import jakarta.persistence.*
import java.time.Instant

@Entity
class UserData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    val name: String,
    var createdDateTime: Instant? = null,
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "userData", cascade = [CascadeType.ALL], orphanRemoval = true)
    var activeUser: ActiveUser? = null,
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "userData", cascade = [CascadeType.ALL], orphanRemoval = true)
    var accountBalance: AccountBalance? = null
) {
    fun logRelatedEntities() {
        activeUser = ActiveUser(userData = this)
        accountBalance = AccountBalance(userData = this)
    }

    @PrePersist
    fun prePersist() {
        createdDateTime = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserData) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (createdDateTime != other.createdDateTime) return false
        if (activeUser != other.activeUser) return false
        if (accountBalance != other.accountBalance) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + (createdDateTime?.hashCode() ?: 0)
        result = 31 * result + (activeUser?.hashCode() ?: 0)
        result = 31 * result + (accountBalance?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "UserData(id=$id, name='$name', createdDateTime=$createdDateTime, activeUser=${activeUser?.id}, " +
                "accountBalance=${accountBalance?.balance})"
    }
}