package com.liberaid.sceneformtest

import android.content.Context
import android.net.Uri
import com.google.ar.sceneform.rendering.ModelRenderable
import java.util.concurrent.CompletableFuture

object ModelLoader {
    fun loadFromAssets(context: Context?, filename: String): CompletableFuture<ModelRenderable> =
        ModelRenderable.builder()
            .setSource(context, Uri.parse(filename))
            .build()
}