package com.marketintelligence.ai.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Note: Hilt bindings for HttpClient and Json are provided in SingletonComponent
// by com.example.marketintelligence.di.NetworkModule.
object NetworkModule {

    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    fun provideJson(): Json {
        return Json { 
            ignoreUnknownKeys = true 
            isLenient = true
            prettyPrint = true
        }
    }
}
