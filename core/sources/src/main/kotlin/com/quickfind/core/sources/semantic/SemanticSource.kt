package com.quickfind.core.sources.semantic

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.di.DefaultDispatcher
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.Vectors
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.SearchSource
import com.quickfind.core.sources.apps.AppSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Meaning-based app search with a small on-device embedding model (spec 10.3):
 * "payment" finds PhonePe, "taxi" finds Uber, even when no letters match.
 *
 * Model: Universal Sentence Encoder (MediaPipe Text Embedder, ~6 MB, Apache 2.0), bundled in
 * assets so it works offline on every phone. App descriptions are embedded once in the
 * background; each query costs one embedding (a few ms) plus a dot product per app.
 * Semantic hits score below letter matches, so typing a name always wins.
 */
@Singleton
class SemanticSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apps: AppSource,
    settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
    @DefaultDispatcher private val cpu: CoroutineDispatcher,
) : SearchSource {
    override val type = SourceType.APP
    // Indexed: runs after the 80 ms pause, never on the instant path.
    override val kind = SourceKind.INDEXED
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.semanticEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    private val mutex = Mutex()
    private var embedder: TextEmbedder? = null
    private var failed = false
    private var index: List<Pair<String, FloatArray>> = emptyList()
    private var indexedVersion = -1

    override fun start() {
        scope.launch(cpu) {
            // Let app start-up finish first; then build the index so the first query is fast.
            delay(5_000)
            if (isEnabled.value) runCatching { ensureIndex() }
        }
    }

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> = withContext(cpu) {
        if (query.normalized.length < MIN_QUERY) return@withContext emptyList()
        val vectors = ensureIndex() ?: return@withContext emptyList()
        val q = embed(query.normalized) ?: return@withContext emptyList()
        currentCoroutineContext().ensureActive()
        vectors.asSequence()
            .map { (id, v) -> id to Vectors.dot(q, v) }
            .filter { it.second >= THRESHOLD }
            .sortedByDescending { it.second }
            .take(MAX_RESULTS.coerceAtMost(limit))
            .mapNotNull { (id, similarity) ->
                apps.resultForId(id)?.copy(matchScore = (similarity * 100).toInt() + SCORE_OFFSET)
            }
            .toList()
    }

    private suspend fun ensureIndex(): List<Pair<String, FloatArray>>? = mutex.withLock {
        if (failed) return null
        if (indexedVersion == apps.version && index.isNotEmpty()) return index
        val docs = apps.semanticDocuments()
        if (docs.isEmpty()) return null
        val built = ArrayList<Pair<String, FloatArray>>(docs.size)
        for ((id, text) in docs) {
            currentCoroutineContext().ensureActive()
            embedLocked(text)?.let { built += id to it }
        }
        index = built
        indexedVersion = apps.version
        index
    }

    private suspend fun embed(text: String): FloatArray? = mutex.withLock { embedLocked(text) }

    /** Caller holds [mutex]: TextEmbedder is not thread-safe. */
    private fun embedLocked(text: String): FloatArray? {
        val e = embedder ?: create() ?: return null
        return runCatching {
            Vectors.normalize(e.embed(text).embeddingResult().embeddings()[0].floatEmbedding())
        }.getOrNull()
    }

    private fun create(): TextEmbedder? = try {
        val options = TextEmbedder.TextEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL).build())
            .build()
        TextEmbedder.createFromOptions(context, options).also { embedder = it }
    } catch (e: Exception) {
        // Missing model or unsupported CPU: turn the feature off quietly; lexical search still works.
        failed = true
        null
    } catch (e: UnsatisfiedLinkError) {
        failed = true
        null
    }

    private companion object {
        const val MODEL = "universal_sentence_encoder.tflite"
        const val MIN_QUERY = 3
        const val THRESHOLD = 0.35f
        const val MAX_RESULTS = 5
        /** Similarity 0.6 → score 30: below a typical name match (60+), above nothing. */
        const val SCORE_OFFSET = -30
    }
}
