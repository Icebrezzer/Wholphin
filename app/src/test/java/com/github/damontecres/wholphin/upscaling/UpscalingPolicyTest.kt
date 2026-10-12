package com.github.damontecres.wholphin.upscaling

import com.github.damontecres.wholphin.preferences.PlayerBackend
import com.github.damontecres.wholphin.preferences.VideoUpscalingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpscalingPolicyTest {
    private val capable =
        UpscalingCapabilities(
            supportsGlEs31 = true,
            performanceClass = UpscalingPerformanceClass.HIGH,
        )

    @Test
    fun offAlwaysUsesNormalRenderer() {
        val decision =
            UpscalingPolicy.resolve(
                VideoUpscalingMode.UPSCALING_OFF,
                capable,
                video(isAnime = true),
                PlayerBackend.EXO_PLAYER,
            )
        assertTrue(decision is UpscalingDecision.Disabled)
    }

    @Test
    fun autoSelectsAnime4kForAnimeOnCapableDevice() {
        val decision =
            UpscalingPolicy.resolve(
                VideoUpscalingMode.UPSCALING_AUTO,
                capable,
                video(isAnime = true),
                PlayerBackend.EXO_PLAYER,
            )
        assertTrue(decision is UpscalingDecision.Enabled)
        assertEquals(UpscalingPipeline.ANIME4K, (decision as UpscalingDecision.Enabled).pipeline)
    }

    @Test
    fun autoSelectsGeneralPipelineForLiveAction() {
        val decision =
            UpscalingPolicy.resolve(
                VideoUpscalingMode.UPSCALING_AUTO,
                capable,
                video(isAnime = false),
                PlayerBackend.EXO_PLAYER,
            )
        assertTrue(decision is UpscalingDecision.Enabled)
        assertEquals(UpscalingPipeline.GENERAL_VIDEO, (decision as UpscalingDecision.Enabled).pipeline)
    }

    @Test
    fun generalModeSelectsGeneralPipeline() {
        val decision =
            UpscalingPolicy.resolve(
                VideoUpscalingMode.UPSCALING_GENERAL,
                capable,
                video(isAnime = false),
                PlayerBackend.EXO_PLAYER,
            )
        assertTrue(decision is UpscalingDecision.Enabled)
        assertEquals(UpscalingPipeline.GENERAL_VIDEO, (decision as UpscalingDecision.Enabled).pipeline)
    }

    @Test
    fun hdrAlwaysBypassesUpscaling() {
        val decision =
            UpscalingPolicy.resolve(
                VideoUpscalingMode.UPSCALING_ANIME4K,
                capable,
                video(isAnime = true, hdr = true),
                PlayerBackend.EXO_PLAYER,
            )
        assertTrue(decision is UpscalingDecision.Disabled)
    }

    @Test
    fun mpvFallsBackUntilMpvPipelineExists() {
        val decision =
            UpscalingPolicy.resolve(
                VideoUpscalingMode.UPSCALING_ANIME4K,
                capable,
                video(isAnime = true),
                PlayerBackend.MPV,
            )
        assertTrue(decision is UpscalingDecision.Disabled)
    }

    private fun video(
        isAnime: Boolean,
        hdr: Boolean = false,
    ) = VideoUpscalingInfo(
        width = 1280,
        height = 720,
        frameRate = 24f,
        isHdr = hdr,
        isLikelyAnime = isAnime,
    )
}
