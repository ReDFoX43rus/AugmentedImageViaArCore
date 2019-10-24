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
        mediaPlayer = MediaPlayer.create(this, R.raw.quad_colors).apply {
            setSurface(texture.surface)
            isLooping = true
            setVolume(0f, 0f)
        }

        Log.d(tag, "Create video plane")

        val anchorNode = AnchorNode()



        Log.d(tag, "Load renderables")

        val planeFilename = "video_plane.sfb"
        val loadPlaneFuture: () -> CompletableFuture<ModelRenderable> = {
            ModelRenderable.builder()
                .setSource(this, Uri.parse(planeFilename))
                .build()
        }

        val frontPlane = loadPlaneFuture()
        val topPlane = loadPlaneFuture()
        val bottomPlane = loadPlaneFuture()
        val leftPlane = loadPlaneFuture()
        val rightPlane = loadPlaneFuture()

        val setupPlaneRenderable: (ModelRenderable) -> Unit = {
            it.isShadowCaster = false
            it.isShadowReceiver = false
            it.material.setExternalTexture("videoTexture", texture)
        }

        CompletableFuture.allOf(frontPlane, topPlane, bottomPlane, leftPlane, rightPlane)
            .thenAccept {
                Log.d(tag, "Renderables are loaded")

                anchorNode.anchor = image.createAnchor(image.centerPose)
                anchorNode.setParent(arFragment.arSceneView.scene)

                val frontPlaneVideoNode = Node().apply {
                    setParent(anchorNode)
                    localScale = Vector3(image.extentX, 1f, image.extentZ)
                }

                val topPlaneVideoNode = Node().apply {
                    setParent(anchorNode)
                    localScale = Vector3(image.extentX, 1f, image.extentZ)
                    localRotation = Quaternion.axisAngle(Vector3(0f, 0f, 1f), -90f)
                }

                texture.surfaceTexture.setOnFrameAvailableListener {
                    it.setOnFrameAvailableListener(null)

                    Log.d(tag, "Assign renderable")
                    frontPlaneVideoNode.renderable = frontPlane.getNow(null)?.also(setupPlaneRenderable)
//                    topPlaneVideoNode.renderable = topPlane.getNow(null)?.also(setupPlaneRenderable)
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
