package com.google.mlkit.vision.demo

import android.util.Log
import com.google.mlkit.vision.demo.kotlin.entity.PosePoint
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

object AngleUtils {
    private const val TAG = "AngleUtils"
    val currentPoint = ArrayList<PosePoint>()
    val trunkPoseAngles = ArrayList<Double>()

    fun setCurrentPoint(referencePoint: ArrayList<PosePoint>){
        trunkPoseAngles.clear()
        currentPoint.clear()
        currentPoint.addAll(referencePoint)
        trunkPoseAngles.addAll(getTrunkPoseAngles(referencePoint))
    }

    fun to15Point(allPoseLandmarks: List<PoseLandmark>): ArrayList<PosePoint> {
        val poses = arrayListOf<PosePoint>()
        poses.add(
            PosePoint(
                allPoseLandmarks[0].position3D.x,
                allPoseLandmarks[0].position3D.y
            )
        )
        //颈部中心点
        val poseNeckX =
            (allPoseLandmarks[11].position.x - allPoseLandmarks[12].position.x) / 2 + allPoseLandmarks[12].position.x
        val poseNeckY = allPoseLandmarks[11].position.y
        val neckPoseLandmark = PosePoint(poseNeckX, poseNeckY)
        poses.add(neckPoseLandmark)
        poses.add(
            PosePoint(
                allPoseLandmarks[12].position3D.x,
                allPoseLandmarks[12].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[14].position3D.x,
                allPoseLandmarks[14].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[16].position3D.x,
                allPoseLandmarks[16].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[11].position3D.x,
                allPoseLandmarks[11].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[13].position3D.x,
                allPoseLandmarks[13].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[15].position3D.x,
                allPoseLandmarks[15].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[24].position3D.x,
                allPoseLandmarks[24].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[26].position3D.x,
                allPoseLandmarks[26].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[28].position3D.x,
                allPoseLandmarks[28].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[23].position3D.x,
                allPoseLandmarks[23].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[25].position3D.x,
                allPoseLandmarks[25].position3D.y
            )
        )
        poses.add(
            PosePoint(
                allPoseLandmarks[27].position3D.x,
                allPoseLandmarks[27].position3D.y
            )
        )
        //肚脐
        val poseNavelX =
            (allPoseLandmarks[23].position.x - allPoseLandmarks[24].position.x) / 2
        val poseNavelY = allPoseLandmarks[23].position.y
        val navelPoseLandmark = PosePoint(poseNavelX, poseNavelY)
        poses.add(navelPoseLandmark)

        return poses
    }

    fun getTrunkPoseAngles(referencePoint: ArrayList<PosePoint>): ArrayList<Double> {
        val trunkPoseAngles = arrayListOf<Double>()
        trunkPoseAngles.add(getAngle(referencePoint[1], referencePoint[2], referencePoint[3]))
        trunkPoseAngles.add(getAngle(referencePoint[2], referencePoint[3], referencePoint[4]))
        trunkPoseAngles.add(getAngle(referencePoint[1], referencePoint[5], referencePoint[6]))
        trunkPoseAngles.add(getAngle(referencePoint[5], referencePoint[6], referencePoint[7]))
        trunkPoseAngles.add(getAngle(referencePoint[14], referencePoint[8], referencePoint[9]))
        trunkPoseAngles.add(getAngle(referencePoint[8], referencePoint[9], referencePoint[10]))
        trunkPoseAngles.add(getAngle(referencePoint[14], referencePoint[11], referencePoint[12]))
        trunkPoseAngles.add(getAngle(referencePoint[11], referencePoint[12], referencePoint[13]))
        trunkPoseAngles.add(getAngle(referencePoint[8], referencePoint[14], referencePoint[11]))
        return trunkPoseAngles
    }

    fun getAngle(
        firstPoint: PosePoint,
        midPoint: PosePoint,
        lastPoint: PosePoint
    ): Double {
        var result = Math.toDegrees(
            (atan2(
                lastPoint.y - midPoint.y,
                lastPoint.x - midPoint.x
            )
                    - atan2(
                firstPoint.y - midPoint.y,
                firstPoint.x - midPoint.x
            )).toDouble()
        )

//        result = abs(result) // Angle should never be negative
        if (result > 180) {
            result = 360.0 - result // Always get the acute representation of the angle
        }

        Log.d(TAG, "getAngle: $result")
        return result
    }

    fun getPoseScore(referPoseAngles: ArrayList<Double>, trunkPoseAngles: ArrayList<Double>):Double {
        val trunkScore = arrayListOf<Double>()
        referPoseAngles.forEachIndexed { index, d ->
            val isSame = trunkPoseAngles[index] * d > 0
            if (isSame) {
                val vs = abs(abs(trunkPoseAngles[index]) - abs(d))
                if (vs > 180) {
                    trunkScore.add(0.0)
                } else {
                    val abs = abs((vs / 180) * 100)
                    trunkScore.add(100 - abs)
                    Log.d("AngleUtils", "abs:${100 - abs}")
                }
            } else {
                trunkScore.add(0.0)
            }
        }

        Log.d("AngleUtils", "average:$trunkScore----${trunkScore.average()}")
return trunkScore.average()
    }
}