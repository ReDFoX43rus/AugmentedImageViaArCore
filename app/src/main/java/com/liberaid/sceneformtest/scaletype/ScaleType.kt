package com.liberaid.sceneformtest.scaletype

import com.google.ar.sceneform.math.Vector3

enum class ScaleType {

    FIT_XY {
        override fun getScale(
            imageWidth: Float,
            imageHeight: Float,
            videoWidth: Float,
            videoHeight: Float
        ): Vector3 {
            return Vector3(imageWidth, 1f, imageHeight)
        }
    },

    CENTER_CROP {
        override fun getScale(
            imageWidth: Float,
            imageHeight: Float,
            videoWidth: Float,
            videoHeight: Float
        ): Vector3 {
            val videoAspectRatio = videoWidth / videoHeight
            val imageAspectRatio = imageWidth / imageHeight

            return if (videoAspectRatio > imageAspectRatio)
                Vector3(imageHeight * videoAspectRatio, 1f, imageHeight)
            else Vector3(imageWidth, 1f, imageWidth / videoAspectRatio)
        }
    },

    CENTER_INSIDE {
        override fun getScale(
            imageWidth: Float,
            imageHeight: Float,
            videoWidth: Float,
            videoHeight: Float
        ): Vector3 {
            val videoAspectRatio = videoWidth / videoHeight
            val imageAspectRatio = imageWidth / imageHeight

            return if (videoAspectRatio < imageAspectRatio)
                Vector3(imageHeight * videoAspectRatio, 1f, imageHeight)
            else Vector3(imageWidth, 1f, imageWidth / videoAspectRatio)
        }
    };

    abstract fun getScale(imageWidth: Float, imageHeight: Float, videoWidth: Float, videoHeight: Float): Vector3
}