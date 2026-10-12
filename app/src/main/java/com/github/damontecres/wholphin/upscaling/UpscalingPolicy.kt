package com.github.damontecres.wholphin.upscaling

import com.github.damontecres.wholphin.preferences.PlayerBackend
import com.github.damontecres.wholphin.preferences.VideoUpscalingMode

/**
 * Pure policy layer for adaptive video upscaling.
 *
 * Rendering is deliberately kept out of this class so AUTO can be unit tested and so unsupported
 * devices always fall back to Wholphin's normal playback path.
 */
object UpscalingPolicy {
    fun resolve(
        requestedMode: VideoUpscalingMode,
        capabilities: UpscalingCapabilities,
        video: VideoUpscalingInfo,
        backend: PlayerBackend,
    ): UpscalingDecision {
        if (requestedMode == VideoUpscalingMode.UPSCALING_OFF) {
            return UpscalingDecision.Disabled("disabled by user")
        }
        if (backend != PlayerBackend.EXO_PLAYER) {
            return UpscalingDecision.Disabled("upscaling currently requires ExoPlayer")
        }
        if (!capabilities.supportsGlEs31) {
            return UpscalingDecision.Disabled("OpenGL ES 3.1 is not available")
        }
        if (video.isHdr) {
            return UpscalingDecision.Disabled("HDR bypass protects the original HDR rendering path")
        }
        if (video.width <= 0 || video.height <= 0) {
            return UpscalingDecision.Disabled("video dimensions are unknown")
        }
        if (video.width > 1920 || video.height > 1080) {
            return UpscalingDecision.Disabled("source is already above 1080p")
        }
        if (capabilities.performanceClass == UpscalingPerformanceClass.LOW) {
            return UpscalingDecision.Disabled("device performance class is too low")
        }

        return when (requestedMode) {
            VideoUpscalingMode.UPSCALING_ANIME4K ->
                UpscalingDecision.Enabled(UpscalingPipeline.ANIME4K, "Anime4K selected by user")

            VideoUpscalingMode.UPSCALING_AUTO -> {
                if (!video.isLikelyAnime) {
                    UpscalingDecision.Disabled("AUTO did not classify the video as anime")
                } else if (video.frameRate > 30f && capabilities.performanceClass != UpscalingPerformanceClass.HIGH) {
                    UpscalingDecision.Disabled("high frame-rate video exceeds the safe device budget")
                } else {
                    UpscalingDecision.Enabled(UpscalingPipeline.ANIME4K, "AUTO selected Anime4K")
                }
            }

            else -> UpscalingDecision.Disabled("unsupported upscaling mode")
        }
    }
}

enum class UpscalingPipeline {
    ANIME4K,
}

enum class UpscalingPerformanceClass {
    LOW,
    MEDIUM,
    HIGH,
}

data class UpscalingCapabilities(
    val supportsGlEs31: Boolean,
    val performanceClass: UpscalingPerformanceClass,
)

data class VideoUpscalingInfo(
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val isHdr: Boolean,
    val isLikelyAnime: Boolean,
)

sealed interface UpscalingDecision {
    val reason: String

    data class Enabled(
        val pipeline: UpscalingPipeline,
        override val reason: String,
    ) : UpscalingDecision

    data class Disabled(
        override val reason: String,
    ) : UpscalingDecision
}
