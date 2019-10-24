package com.liberaid.sceneformtest

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

operator fun Quaternion.times(another: Quaternion): Quaternion {
    val (v1, w1) = toVectorW()
    val (v2, w2) = another.toVectorW()

    val newV = (v1 vecProduct v2) + v2.scaled(w1) + v1.scaled(w2)
    val newW = w1 * w2 - (v1 scalarProduct v2)

    return Quaternion(newV, newW)
}

fun Quaternion.toVectorW(): Pair<Vector3, Float> = Vector3(x, y, z) to w

infix fun Vector3.vecProduct(another: Vector3): Vector3 {
    val x2 = another.x
    val y2 = another.y
    val z2 = another.z

    return Vector3(y * z2 - z * y2, x2 * z - x * z2, x * y2 - x2 * y)
}

infix fun Vector3.scalarProduct(another: Vector3): Float {
    val x2 = another.x
    val y2 = another.y
    val z2 = another.z

    return x * x2 + y * y2 + z * z2
}

operator fun Vector3.plus(another: Vector3): Vector3 {
    return Vector3(x + another.x, y + another.y, z + another.z)
}