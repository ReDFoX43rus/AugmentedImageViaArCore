package com.liberaid.sceneformtest

import android.animation.ValueAnimator
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.liberaid.sceneformtest.scaletype.ScaleType

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
        val videoFilename = "take_part.mp4"

        mediaPlayer = MediaPlayer().apply {
            setSurface(texture.surface)
            isLooping = true
            setVolume(0f, 0f)
        }

        var videoWidth = 0f
        var videoHeight = 0f
        assets.openFd(videoFilename).use { descriptor ->

            val metadataRetriever = MediaMetadataRetriever().apply {
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }

            videoWidth = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toFloatOrNull() ?: 0f
            videoHeight = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toFloatOrNull() ?: 0f

            mediaPlayer?.setDataSource(descriptor)
        }

        Log.d(tag, "Create video plane")

        val anchorNode = AnchorNode()

        Log.d(tag, "Load renderables")

        val planeFilename = "splited_plane.sfb"

        ModelRenderable.builder()
            .setSource(this, Uri.parse(planeFilename))
            .build()
            .thenAccept { renderable ->
                Log.d(tag, "Renderable is loaded")

                anchorNode.anchor = image.createAnchor(image.centerPose)
                anchorNode.setParent(arFragment.arSceneView.scene)

                val scaleType = if(videoWidth == 0f || videoHeight == 0f) ScaleType.FIT_XY else ScaleType.CENTER_CROP
                val scale = scaleType.getScale(image.extentX, image.extentZ, videoWidth, videoHeight)

                val videoNode = Node().apply {
                    setParent(anchorNode)
                    localScale = scale
                }

                texture.surfaceTexture.setOnFrameAvailableListener {
                    it.setOnFrameAvailableListener(null)

                    Log.d(tag, "Assign renderable")
                    videoNode.renderable = renderable.also {
                        it.isShadowReceiver = false
                        it.isShadowCaster = false
                        it.material.setExternalTexture("videoTexture", texture)

                        val scaledImageWidth = image.extentX / scale.x
                        it.material.setFloat("maxWidth", scaledImageWidth / 2f)

                        it.material.setFloat("alpha", 0f)

                        ValueAnimator.ofFloat(0f, 1f).apply {
                            duration = 500L
                            interpolator = DecelerateInterpolator()
                            addUpdateListener { animator ->
                                it.material.setFloat("alpha", animator.animatedFraction)
                            }
                            start()
                        }
                    }
                }

                Log.d(tag, "Start mediaPlayer")
                mediaPlayer?.prepare()
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
