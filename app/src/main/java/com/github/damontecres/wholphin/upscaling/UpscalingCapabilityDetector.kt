package com.github.damontecres.wholphin.upscaling

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conservative device classifier. It intentionally under-selects rather than enabling an expensive
 * shader on hardware that may cause dropped frames.
 */
@Singleton
class UpscalingCapabilityDetector
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun detect(): UpscalingCapabilities {
            val activityManager = context.getSystemService(ActivityManager::class.java)
            val glEsVersion = activityManager?.deviceConfigurationInfo?.reqGlEsVersion ?: 0
            val supportsGlEs31 = glEsVersion >= OPEN_GL_ES_3_1

            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val totalMemoryGb = memoryInfo.totalMem.toDouble() / GIB
            val cores = Runtime.getRuntime().availableProcessors()

            val performanceClass =
                when {
                    !supportsGlEs31 -> {
                        UpscalingPerformanceClass.LOW
                    }

                    Build.VERSION.SDK_INT >= 31 && totalMemoryGb >= 5.0 && cores >= 6 -> {
                        UpscalingPerformanceClass.HIGH
                    }

                    totalMemoryGb >= 2.0 && cores >= 4 -> {
                        UpscalingPerformanceClass.MEDIUM
                    }

                    else -> {
                        UpscalingPerformanceClass.LOW
                    }
                }

            return UpscalingCapabilities(
                supportsGlEs31 = supportsGlEs31,
                performanceClass = performanceClass,
            )
        }

        private companion object {
            const val OPEN_GL_ES_3_1 = 0x00030001
            const val GIB = 1024.0 * 1024.0 * 1024.0
        }
    }
