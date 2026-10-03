package com.aistudio.couplebubble.qxztrw

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Fetcher
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Keyer
import com.aistudio.couplebubble.qxztrw.ui.coil.Base64Mapper

class CoupleBubbleApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(Base64Mapper())
                add(Base64Keyer())
                add(Base64Fetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(enable = true)
            .allowHardware(enable = false)
            .respectCacheHeaders(enable = false)
            .build()
    }
}
