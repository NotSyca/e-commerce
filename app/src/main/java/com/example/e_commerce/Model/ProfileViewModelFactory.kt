package com.example.e_commerce.Model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.e_commerce.Api.TokenManager
import io.github.jan.supabase.SupabaseClient

class ProfileViewModelFactory(
    private val supabase: SupabaseClient,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(supabase, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}