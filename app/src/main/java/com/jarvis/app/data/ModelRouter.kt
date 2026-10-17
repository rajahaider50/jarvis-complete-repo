package com.jarvis.app.data

import android.content.Context
import com.jarvis.app.data.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ln

/** Scores real provider models and remembers temporary health/quota state locally. */
object ModelRouter {
    enum class Task { CHAT, CODING }
    data class Candidate(val provider: CustomProvider, val model: ProviderModel, val score: Double)
    private data class Health(
        var day: Long = today(), var usedTokens: Long = 0, var cooldownUntil: Long = 0,
        var successes: Int = 0, var failures: Int = 0, var errorStreak: Int = 0, var latencyMs: Long = 0
    )

    private val health = ConcurrentHashMap<String, Health>()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var file: File? = null
    private const val UNKNOWN_CONTEXT = 4096

    fun init(ctx: Context) {
        if (file != null) return
        file = File(ctx.applicationContext.filesDir, "model_router.json")
        load()
    }

    /** Preferred is honored; fallback providers are included so one limit cannot stop the request. */
    fun rank(preferred: CustomProvider?, task: Task, estimatedTokens: Int, needsTools: Boolean): List<Candidate> {
        val providers = (listOfNotNull(preferred) + CustomProviders.fallbacks()).distinctBy { it.id }
        val now = System.currentTimeMillis()
        val out = ArrayList<Candidate>()
        providers.forEach { p ->
            val models = if (p.modelId.isNotBlank()) {
                listOf(p.models.firstOrNull { it.id == p.modelId } ?: ProviderModel(p.modelId, source = "manual"))
            } else CustomProviders.eligibleModels(p)
            models.forEach { m ->
                val h = get(p.id, m.id)
                resetDay(h)
                if (h.cooldownUntil > now) return@forEach
                if (p.dailyLimitTokens > 0 && h.usedTokens + estimatedTokens >= p.dailyLimitTokens) return@forEach
                val context = m.contextWindow ?: UNKNOWN_CONTEXT
                if (estimatedTokens > (context - 512).coerceAtLeast(512)) return@forEach
                if (needsTools && m.supportsTools == false) return@forEach
                val quality = (m.qualityTier ?: 1).coerceIn(1, 5)
                val contextScore = ln(context.toDouble().coerceAtLeast(2.0)) * 10.0
                val toolsBonus = if (needsTools && m.supportsTools == true) 30.0 else 0.0
                val successRate = if (h.successes + h.failures == 0) 0.5 else h.successes.toDouble() / (h.successes + h.failures)
                val latencyPenalty = (h.latencyMs / 1000.0).coerceAtMost(20.0)
                val errorPenalty = h.errorStreak * 12.0
                val score = quality * 40.0 + contextScore + toolsBonus + successRate * 20.0 - latencyPenalty - errorPenalty
                out += Candidate(p, m, score)
            }
        }
        return out.sortedByDescending { it.score }
    }

    fun markSuccess(candidate: Candidate, latencyMs: Long, estimatedTokens: Int) {
        val h = get(candidate.provider.id, candidate.model.id)
        resetDay(h); h.successes++; h.errorStreak = 0; h.cooldownUntil = 0
        h.latencyMs = if (h.latencyMs == 0L) latencyMs else (h.latencyMs * 3 + latencyMs) / 4
        h.usedTokens += estimatedTokens.coerceAtLeast(0)
        saveAsync()
    }

    fun markFailure(candidate: Candidate, error: Throwable): Boolean {
        val code = httpCode(error)
        if (!retryable(error)) return false
        val h = get(candidate.provider.id, candidate.model.id)
        resetDay(h); h.failures++; h.errorStreak++
        val msg = error.message.orEmpty().lowercase()
        h.cooldownUntil = if (code == 402 && ("quota" in msg || "daily" in msg || "limit" in msg)) nextDay()
        else System.currentTimeMillis() + when (code) {
            429 -> 60_000L
            408, 409, 425 -> 30_000L
            in 500..599 -> (30_000L * (1L shl (h.errorStreak.coerceIn(0, 4)))).coerceAtMost(15 * 60_000L)
            else -> 20_000L
        }
        saveAsync()
        return true
    }

    fun retryable(error: Throwable): Boolean {
        val code = httpCode(error)
        if (code != null) return code == 402 || code == 408 || code == 409 || code == 425 || code == 429 || code in 500..599
        val m = error.message.orEmpty().lowercase()
        return error is java.net.SocketTimeoutException || "timeout" in m || "connection reset" in m || "empty reply" in m
    }

    fun httpCode(error: Throwable): Int? = Regex("HTTP (\\d+)").find(error.message.orEmpty())?.groupValues?.get(1)?.toIntOrNull()

    fun estimateTokens(turns: List<GeminiClient.Turn>, extra: String = ""): Int =
        ((turns.sumOf { it.text.length } + extra.length) / 4).coerceAtLeast(1)

    fun nextAvailable(preferred: CustomProvider?, task: Task, estimatedTokens: Int, needsTools: Boolean): Long? =
        rankIncludingCooldown(preferred, task, estimatedTokens, needsTools).map { it.second }.filter { it > System.currentTimeMillis() }.minOrNull()

    private fun rankIncludingCooldown(preferred: CustomProvider?, task: Task, estimatedTokens: Int, needsTools: Boolean): List<Pair<Candidate, Long>> {
        val providers = (listOfNotNull(preferred) + CustomProviders.fallbacks()).distinctBy { it.id }
        val out = ArrayList<Pair<Candidate, Long>>()
        providers.forEach { p ->
            val models = if (p.modelId.isNotBlank()) listOf(p.models.firstOrNull { it.id == p.modelId } ?: ProviderModel(p.modelId, source = "manual")) else CustomProviders.eligibleModels(p)
            models.forEach { m ->
                val h = get(p.id, m.id); resetDay(h)
                if (needsTools && m.supportsTools == false) return@forEach
                out += Candidate(p, m, 0.0) to h.cooldownUntil
            }
        }
        return out
    }

    /** Read-only view of the routing health for the Router Log screen. */
    data class Row(
        val key: String, val providerId: String, val modelId: String,
        val usedTokens: Long, val cooldownUntil: Long, val successes: Int,
        val failures: Int, val errorStreak: Int, val latencyMs: Long
    )

    fun snapshot(): List<Row> = health.entries.map { (k, h) ->
        val parts = k.split("\u0000")
        Row(k, parts.getOrElse(0) { "?" }, parts.getOrElse(1) { "?" }, h.usedTokens, h.cooldownUntil, h.successes, h.failures, h.errorStreak, h.latencyMs)
    }.sortedByDescending { it.usedTokens + it.failures * 100 }

    fun clearAll() { health.clear(); saveAsync() }

    private fun get(provider: String, model: String) = health.getOrPut("$provider\u0000$model") { Health() }
    private fun resetDay(h: Health) { if (h.day != today()) { h.day = today(); h.usedTokens = 0; h.failures = 0; h.errorStreak = 0; h.cooldownUntil = 0 } }
    private fun today(): Long = System.currentTimeMillis() / 86_400_000L
    private fun nextDay(): Long = (today() + 1) * 86_400_000L

    private fun load() {
        try {
            val a = JSONArray(file?.takeIf { it.exists() }?.readText() ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i); health[o.getString("k")] = Health(o.optLong("d", today()), o.optLong("u"), o.optLong("c"), o.optInt("s"), o.optInt("f"), o.optInt("e"), o.optLong("l"))
            }
        } catch (e: Exception) { AppLog.w(e) }
    }

    private fun saveAsync() {
        val snapshot = health.entries.map { (k, h) -> JSONObject().put("k", k).put("d", h.day).put("u", h.usedTokens).put("c", h.cooldownUntil).put("s", h.successes).put("f", h.failures).put("e", h.errorStreak).put("l", h.latencyMs) }
        io.launch { try { file?.writeText(JSONArray(snapshot).toString()) } catch (e: Exception) { AppLog.w(e) } }
    }
}
