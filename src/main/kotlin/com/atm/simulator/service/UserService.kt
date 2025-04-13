package com.atm.simulator.service

import com.atm.simulator.api.UserDto
import com.atm.simulator.exception.InvalidRequestException
import com.atm.simulator.model.ActiveUser
import com.atm.simulator.model.UserData
import com.atm.simulator.repository.ActiveUserRepository
import com.atm.simulator.repository.OwedBalanceRepository
import com.atm.simulator.repository.UserDataRepository
import com.atm.simulator.util.USER_ALREADY_LOGGED_IN_MSG
import com.atm.simulator.util.USER_NOT_LOGGED_IN_MSG
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class UserService(
    private val activeUserRepository: ActiveUserRepository,
    private val owedBalanceRepository: OwedBalanceRepository,
    private val userDataRepository: UserDataRepository
) {
    @Transactional
    fun processLogin(userDto: UserDto): UserData {
        val user = userDataRepository.findByName(userDto.name!!)?.let { existingUser ->
            LOGGER.info("Existing user found: $existingUser.")
            if (existingUser.activeUser != null) throw InvalidRequestException(USER_ALREADY_LOGGED_IN_MSG)
            activeUserRepository.save(ActiveUser(userData = existingUser)).also {
                LOGGER.info("Saved activeUser: $it.")
            }
            existingUser
        } ?: UserData(name = userDto.name).let { userData ->
            userData.logRelatedEntities()
            userDataRepository.save(userData).also {
                LOGGER.info("Saved userData: $it.")
            }
        }
        // set user and amount details that current user owes from
        user.accountBalance!!.owedToDetails = owedBalanceRepository.findBySourceUserDataId(user.id!!).associate {
            it.targetUserData.name to it.amount
        }
        // set user and amount details that current user owes to
        user.accountBalance!!.owedFromDetails = owedBalanceRepository.findByTargetUserDataId(user.id!!).associate {
            it.sourceUserData.name to it.amount
        }
        return user
    }

    fun processLogout(userDto: UserDto): UserData {
        return userDataRepository.findByName(userDto.name!!)?.let { userData ->
            // remove from active user list on logout
            activeUserRepository.delete(userData.activeUser!!).also {
                LOGGER.info("Saved activeUser: $it.")
            }
            userData
        } ?: throw InvalidRequestException(USER_NOT_LOGGED_IN_MSG)
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}