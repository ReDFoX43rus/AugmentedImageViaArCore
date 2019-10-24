package com.liberaid.sceneformtest

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment

    private val augmentedImagesNodes = mutableMapOf<AugmentedImage, AnchorNode>()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        arFragment.arSceneView.scene.addOnUpdateListener(this::onUpdateFrame)
    }

    private fun onUpdateFrame(frameTime: FrameTime) {
        val frame = arFragment.arSceneView.arFrame ?: return

        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        updatedAugmentedImages.forEach { augImage ->

//            runBlocking {
                when(augImage.trackingState) {
                    TrackingState.PAUSED -> {
                        Log.d(tag, "Tracking paused $augImage")
                    }

                    TrackingState.TRACKING -> {
                        if(augmentedImagesNodes.containsKey(augImage))
                            return

                        Log.d(tag, "Tracking started $augImage")
                        Log.d(tag, "Add image to map")

                        val node = playVideo(augImage)

                        augmentedImagesNodes.put(augImage, node)
                        arFragment.arSceneView.scene.addChild(node)
                    }

                    TrackingState.STOPPED -> {
                        Log.d(tag, "Tracking stopped $augImage")
                        augmentedImagesNodes.remove(augImage)
                    }

                    else -> {
                        Log.d(tag, "Tracking state is null ($augImage)")
                    }
                }
//            }
        }
    }

    private fun playVideo(image: AugmentedImage): AnchorNode {
        releaseMediaPlayer()

        val texture = ExternalTexture()

        Log.d(tag, "Create mediaPlayer")
        mediaPlayer = MediaPlayer.create(this, R.raw.quad_colors).apply {
            setSurface(texture.surface)
            isLooping = true
            setVolume(0f, 0f)
        }

        Log.d(tag, "Create video plane")

        val anchorNode = AnchorNode()

        ModelRenderable.builder()
            .setSource(this, Uri.parse("video_plane.sfb"))
            .build()
            .thenAccept { renderable ->
                Log.d(tag, "Renderable is loaded")

                renderable.isShadowCaster = false
                renderable.isShadowReceiver = false
                renderable.material.setExternalTexture("videoTexture", texture)

                Log.d(tag, "Place video plane on scene")

                anchorNode.anchor = image.createAnchor(image.centerPose)
                anchorNode.setParent(arFragment.arSceneView.scene)

                Log.d(tag, "Setup videoNode")

                val videoNode = Node()
                videoNode.setParent(anchorNode)

                val scale = 1.01f
                videoNode.localScale = Vector3(scale * image.extentX, 1f, scale * image.extentZ)

                Log.d(tag, "Setup texture surface callback")

                texture.surfaceTexture.setOnFrameAvailableListener {
                    it.setOnFrameAvailableListener(null)

                    Log.d(tag, "Assign videoNode.renderable")
                    videoNode.renderable = renderable
                }

                Log.d(tag, "Start player")
                mediaPlayer?.start()

                Log.d(tag, "Playback has started")
            }
            .exceptionally { t ->
                Log.d(tag, "Error loading video_plane: $t")
                null
            }

        return anchorNode
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()

        releaseMediaPlayer()
    }

    companion object {
        const val tag = "AUGMENTED_IMAGE"
    }
}
