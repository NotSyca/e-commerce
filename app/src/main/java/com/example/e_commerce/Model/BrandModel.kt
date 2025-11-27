package com.example.e_commerce.Model

import kotlinx.serialization.Serializable

@Serializable
data class BrandModel(
    val name: String="",
    val id:Int=0,
    val picUrl:String=""
)
