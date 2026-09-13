package com.cadeteria.cadete.data.remote.dto

data class LoginRequest(
    val username: String,
    val password: String,
)

data class TokenResponse(
    val token: String,
    val tokenType: String,
    val tipo: String,
    val expiresAt: String,
)
