package com.podut.dataagregate.core.network

import com.podut.dataagregate.core.domain.model.CategoryCreate
import com.podut.dataagregate.core.domain.model.DigestResponse
import com.podut.dataagregate.core.domain.model.FeedCategory
import com.podut.dataagregate.core.domain.model.RSSIngestRequest
import com.podut.dataagregate.core.domain.model.RssFeedRequest
import com.podut.dataagregate.core.domain.model.SerpConfigUpdate
import com.podut.dataagregate.core.domain.model.TechNews
import com.podut.dataagregate.core.domain.model.UserProfileSync
import kotlinx.serialization.builtins.ListSerializer
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class IngestApiService @Inject constructor(
    private val client: HttpClient,
    @Named("baseUrl") private val BASE_URL: String
) {

    suspend fun checkRssHealth(url: String): Boolean {
        println("Checking health for: $url")
        return try {
            val response: HttpResponse = client.get(url)
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Health check failed for $url: ${e.message}")
            false
        }
    }

    suspend fun ingestRss(request: RSSIngestRequest): Result<String> {
        val url = "$BASE_URL/ingest"
        return try {
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success("Succes: ${response.bodyAsText()}")
            } else {
                Result.failure(Exception("Eroare Server: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncAll(): Result<String> {
        val url = "$BASE_URL/sync-all"
        return try {
            val response: HttpResponse = client.post(url)
            if (response.status.isSuccess()) {
                Result.success("Sincronizare pornită!")
            } else {
                Result.failure(Exception("Eroare: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDigest(deviceId: String? = null): Result<DigestResponse> {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/digest") {
                if (deviceId != null) {
                    parameter("deviceId", deviceId)
                }
            }
            if (response.status.isSuccess()) Result.success(response.body<DigestResponse>())
            else Result.failure(Exception("Server error: ${response.status}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // ─── Categories ──────────────────────────────────────────────────────────

    suspend fun getCategories(deviceId: String): Result<List<FeedCategory>> = try {
        val r: HttpResponse = client.get("$BASE_URL/categories") {
            parameter("deviceId", deviceId)
        }
        if (r.status.isSuccess()) Result.success(r.body())
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createCategory(body: CategoryCreate): Result<Unit> = try {
        val r: HttpResponse = client.post("$BASE_URL/categories") {
            contentType(ContentType.Application.Json); setBody(body)
        }
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteCategory(name: String): Result<Unit> = try {
        val r: HttpResponse = client.delete("$BASE_URL/categories/$name")
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun addFeed(category: String, url: String, deviceId: String): Result<Unit> = try {
        val encodedCategory = java.net.URLEncoder.encode(category, "UTF-8").replace("+", "%20")
        val r: HttpResponse = client.post("$BASE_URL/categories/$encodedCategory/feeds") {
            contentType(ContentType.Application.Json); setBody(RssFeedRequest(url, deviceId))
        }
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun toggleFeed(category: String, url: String, isActive: Boolean, deviceId: String): Result<Unit> = try {
        val encodedCategory = java.net.URLEncoder.encode(category, "UTF-8").replace("+", "%20")
        val r: HttpResponse = client.put("$BASE_URL/categories/$encodedCategory/feeds/toggle") {
            contentType(ContentType.Application.Json); setBody(RssFeedRequest(url, deviceId, isActive))
        }
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun removeFeed(category: String, url: String, deviceId: String): Result<Unit> = try {
        val encodedCategory = java.net.URLEncoder.encode(category, "UTF-8").replace("+", "%20")
        val r: HttpResponse = client.delete("$BASE_URL/categories/$encodedCategory/feeds") {
            contentType(ContentType.Application.Json); setBody(RssFeedRequest(url, deviceId))
        }
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateSerpConfig(category: String, body: SerpConfigUpdate): Result<Unit> = try {
        val encodedCategory = java.net.URLEncoder.encode(category, "UTF-8").replace("+", "%20")
        val r: HttpResponse = client.put("$BASE_URL/categories/$encodedCategory/serp") {
            contentType(ContentType.Application.Json); setBody(body)
        }
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun fetchSerp(category: String): Result<Unit> = try {
        val encodedCategory = java.net.URLEncoder.encode(category, "UTF-8").replace("+", "%20")
        val r: HttpResponse = client.post("$BASE_URL/categories/$encodedCategory/serp/fetch")
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun syncProfile(deviceId: String, favoriteCategories: List<String>, name: String = "Reader", language: String = "en"): Result<Unit> = try {
        val r: HttpResponse = client.post("$BASE_URL/profile") {
            contentType(ContentType.Application.Json)
            setBody(UserProfileSync(deviceId = deviceId, favoriteCategories = favoriteCategories, name = name, language = language))
        }
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getProfile(deviceId: String): Result<UserProfileSync> = try {
        val r: HttpResponse = client.get("$BASE_URL/profile/$deviceId")
        if (r.status.isSuccess()) Result.success(r.body())
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getArticlesByCategory(category: String, limit: Int = 50, offset: Int = 0): Result<List<TechNews>> = try {
        val encodedCategory = java.net.URLEncoder.encode(category, "UTF-8").replace("+", "%20")
        val r: HttpResponse = client.get("$BASE_URL/categories/$encodedCategory/articles") {
            parameter("limit", limit)
            parameter("offset", offset)
        }
        if (r.status.isSuccess()) Result.success(r.body())
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getArticlesByTag(tag: String, limit: Int = 20, offset: Int = 0): Result<List<TechNews>> = try {
        val r: HttpResponse = client.get("$BASE_URL/articles/by-tag") {
            parameter("tag", tag.lowercase())
            parameter("limit", limit)
            parameter("offset", offset)
        }
        if (r.status.isSuccess()) Result.success(r.body())
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateScheduler(intervalHours: Int): Result<Unit> = try {
        val r: HttpResponse = client.put("$BASE_URL/scheduler/config") {
            contentType(ContentType.Application.Json)
            setBody(com.podut.dataagregate.core.domain.model.SchedulerConfig(intervalHours))
        }
        if (r.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getSchedulerConfig(): Result<com.podut.dataagregate.core.domain.model.SchedulerConfig> = try {
        val r: HttpResponse = client.get("$BASE_URL/scheduler/config")
        if (r.status.isSuccess()) Result.success(r.body())
        else Result.failure(Exception("Error: ${r.status}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun markSeen(deviceId: String, urls: List<String>): Result<Unit> {
        if (urls.isEmpty()) return Result.success(Unit)
        return try {
            val r: HttpResponse = client.post("$BASE_URL/articles/mark-seen") {
                contentType(ContentType.Application.Json)
                setBody(com.podut.dataagregate.core.domain.model.MarkSeenRequest(deviceId, urls))
            }
            if (r.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Error: ${r.status}"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
