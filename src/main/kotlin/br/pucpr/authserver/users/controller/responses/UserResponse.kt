package br.pucpr.authserver.users.controller.responses

import br.pucpr.authserver.users.User

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val avatar: String,
) {
    constructor(user: User, avatarUrl: String) :
            this(user.id!!, user.email, user.name, avatarUrl)
}
