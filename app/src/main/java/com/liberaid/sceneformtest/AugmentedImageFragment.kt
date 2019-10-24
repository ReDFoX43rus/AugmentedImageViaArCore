package com.liberaid.sceneformtest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.lang.Exception

class AugmentedImageFragment : ArFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // Turn off the plane discovery since we're only looking for images
        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        arSceneView.planeRenderer.isEnabled = false
        arSceneView.isLightEstimationEnabled = false
        return view
    }

    override fun getSessionConfiguration(session: Session): Config {
        val config = super.getSessionConfiguration(session)
        if(!setupAugmentedImage(config, session))
            Log.d("AUGMENTED_IMAGE", "Cannot setup config (database)")

        return config
    }

    private fun setupAugmentedImage(config: Config, session: Session): Boolean {
        val bitmap = loadImageBitmap() ?: return false
        val db = AugmentedImageDatabase(session).also {
            it.addImage("qc_frame_gs.png", bitmap)
        }

        config.setAugmentedImageDatabase(db)
        return true
    }

    private fun loadImageBitmap(): Bitmap? {
        val stream = context?.assets?.open("qc_frame_gs.png") ?: return null
        return try {
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            null
        }
    }
}