package com.liberaid.sceneformtest

import android.animation.ValueAnimator
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.liberaid.sceneformtest.scaletype.ScaleType
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment

    private val augmentedImagesNodes = mutableMapOf<AugmentedImage, AnchorNode>()

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

                    Timber.d("ImageFound=${SystemClock.elapsedRealtime()}")

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

        val anchorNode = AnchorNode()

        Log.d(tag, "Load renderables")

        val planeFilename = "man10_backed.sfb"

        ModelRenderable.builder()
            .setSource(this, Uri.parse(planeFilename))
            .build()
            .thenAccept { renderable ->
                Log.d(tag, "Renderable is loaded")

                anchorNode.anchor = image.createAnchor(image.centerPose)
                anchorNode.setParent(arFragment.arSceneView.scene)

                val node = Node().apply {
                    setParent(anchorNode)
                    setRenderable(renderable)
                    val rot1 = Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)
                    val rot2 = Quaternion.axisAngle(Vector3(0f, 0f, 1f), 180f)
                    val result = Quaternion.multiply(rot2, rot1)
                    localRotation = result

                    val scale = .1f
                    localScale = Vector3.one().scaled(scale)

                    localPosition = Vector3(0f, 0f, image.extentZ / 2)
                }

                val animations = renderable.animationDataCount
                Timber.d("Animations: $animations")

                if(animations <= 0)
                    return@thenAccept

                val speakAnimation = renderable.getAnimationData(0)
                val animator = ModelAnimator(speakAnimation, renderable)
                animator.repeatCount = ValueAnimator.INFINITE
                animator.duration = 8000L
                animator.start()

                Timber.d("Started animation name=${speakAnimation.name}, duration=${speakAnimation.durationMs}")
            }

        return anchorNode
    }

    companion object {
        const val tag = "AUGMENTED_IMAGE"
    }
}
