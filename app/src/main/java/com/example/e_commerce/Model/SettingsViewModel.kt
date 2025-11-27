package com.example.e_commerce.Model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e_commerce.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchHistory(
    val id: Int,
    val user_id: String,
    val query: String,
    @SerialName("created_at")
    val createdAt: String
)

class SettingsViewModel : ViewModel() {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
    }

    private val _searchHistory = MutableLiveData<List<SearchHistory>>()
    val searchHistory: LiveData<List<SearchHistory>> = _searchHistory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val SEARCH_HISTORY_TABLE_NAME = "search_history"

    fun fetchSearchHistory(userId: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val history = supabase.from(SEARCH_HISTORY_TABLE_NAME)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", Order.DESCENDING)
                        limit(10)
                    }.decodeList<SearchHistory>()
                _searchHistory.postValue(history)
                Log.d("SettingsViewModel", "Fetched ${history.size} history items for user $userId")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error fetching search history: ${e.message}", e)
                _searchHistory.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
