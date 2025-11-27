package com.example.e_commerce.Model

import kotlinx.serialization.Serializable

@Serializable
data class SearchHistoryRequest(
    val user_id: String,
    val query: String
)
