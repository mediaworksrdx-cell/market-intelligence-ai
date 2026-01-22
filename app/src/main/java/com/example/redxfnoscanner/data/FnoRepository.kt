package com.example.redxfnoscanner.data

class FnoRepository {
    private val apiService = RetrofitClient.instance

    suspend fun getFnoData(symbol: String): FnoData {
        return apiService.getFnoData(symbol)
    }
}
