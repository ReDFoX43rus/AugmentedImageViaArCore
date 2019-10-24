package com.liberaid.sceneformtest

import android.content.Context
import android.net.Uri
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ModelLoader {
    suspend fun loadFromAssets(context: Context?, filename: String) = suspendCoroutine<ModelRenderable> { cont ->
        ModelRenderable.builder()
            .setSource(context, Uri.parse(filename))
            .build()
            .thenAccept { cont.resume(it) }
            .exceptionally { t ->
                cont.resumeWithException(t)
                null
            }
    }
}