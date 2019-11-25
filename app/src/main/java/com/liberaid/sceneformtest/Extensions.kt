package com.liberaid.sceneformtest

import com.google.ar.sceneform.math.Quaternion

fun quaternionFromFloatArray(floatArray: FloatArray): Quaternion {
    assert(floatArray.size == 4)
    return Quaternion(floatArray[0], floatArray[1], floatArray[2], floatArray[3])
}