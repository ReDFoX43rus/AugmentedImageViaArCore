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
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.util.concurrent.CompletableFuture

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
        }
    }

    private fun playVideo(image: AugmentedImage): AnchorNode {
        releaseMediaPlayer()

        val texture = ExternalTexture()

        Log.d(tag, "Create mediaPlayer")
        mediaPlayer = MediaPlayer.create(this, R.raw.take_part).apply {
            setSurface(texture.surface)
            isLooping = true
            setVolume(0f, 0f)
        }

        Log.d(tag, "Create video plane")

        val anchorNode = AnchorNode()

        Log.d(tag, "Load renderables")

        val planeFilename = "video_plane.sfb"

        ModelRenderable.builder()
            .setSource(this, Uri.parse(planeFilename))
            .build()
            .thenAccept { renderable ->
                Log.d(tag, "Renderable is loaded")

                anchorNode.anchor = image.createAnchor(image.centerPose)
                anchorNode.setParent(arFragment.arSceneView.scene)

                val frontPlaneVideoNode = Node().apply {
                    setParent(anchorNode)
                    localScale = Vector3(image.extentX, 1f, image.extentZ)
                }

                texture.surfaceTexture.setOnFrameAvailableListener {
                    it.setOnFrameAvailableListener(null)

                    Log.d(tag, "Assign renderable")
                    frontPlaneVideoNode.renderable = renderable.also {
                        it.isShadowReceiver = false
                        it.isShadowCaster = false
                        it.material.setExternalTexture("videoTexture", texture)
                    }
                }

                Log.d(tag, "Start mediaPlayer")
                mediaPlayer?.start()
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
