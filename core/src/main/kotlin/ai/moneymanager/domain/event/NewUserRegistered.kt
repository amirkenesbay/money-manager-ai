package ai.moneymanager.domain.event

import ai.moneymanager.domain.model.UserInfo

data class NewUserRegistered(val user: UserInfo)
