package com.google.mlkit.vision.demo

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

/**
 * 转换骨骼点
 * 获取骨骼点角度
 * 获取两个pose相似度
 *
 * google 33个骨骼点与15个骨骼点关系图：
 * https://wx08xmlr43.feishu.cn/file/boxcnxKv2HbHuNnaKsloyKDm2yf
 */
object PoseAngleUtils {
    private const val TAG = "AngleUtils"
    val currentPoint = ArrayList<PointF>()
    val trunkPoseAngles = ArrayList<Double>()

    /**
     * 设置当前参考pose（15个）
     */
    fun setCurrentPoint(referencePoint: ArrayList<PointF>?) {
        if (!referencePoint.isNullOrEmpty()) {
            trunkPoseAngles.clear()
            currentPoint.clear()
            currentPoint.addAll(referencePoint)
            trunkPoseAngles.addAll(getPoseAngles(currentPoint))
        }
    }

    /**
     * 转换
     * google 33个关键点转15个
     * url：https://wx08xmlr43.feishu.cn/file/boxcnxKv2HbHuNnaKsloyKDm2yf
     */
    fun googleTo15Point(googlePose: List<PoseLandmark>?): ArrayList<PointF>? {
        if (googlePose.isNullOrEmpty() || googlePose.size < 33) return null
        val poses = arrayListOf<PointF>()
        //poses add 顺序不能改变
        poses.add(PointF(googlePose[0].position3D.x, googlePose[0].position3D.y))//0 鼻子
        //颈部中心点
        val poseNeckX =
            (googlePose[11].position.x - googlePose[12].position.x) / 2 + googlePose[12].position.x
        val poseNeckY = googlePose[11].position.y
        poses.add(PointF(poseNeckX, poseNeckY))//1 颈部
        poses.add(PointF(googlePose[12].position3D.x, googlePose[12].position3D.y))//2 右肩
        poses.add(PointF(googlePose[14].position3D.x, googlePose[14].position3D.y))//3 右臂关节点
        poses.add(PointF(googlePose[16].position3D.x, googlePose[16].position3D.y))//4 右手
        poses.add(PointF(googlePose[11].position3D.x, googlePose[11].position3D.y))//5 左肩
        poses.add(PointF(googlePose[13].position3D.x, googlePose[13].position3D.y))//6 左臂关节点
        poses.add(PointF(googlePose[15].position3D.x, googlePose[15].position3D.y))//7 右手
        poses.add(PointF(googlePose[24].position3D.x, googlePose[24].position3D.y))//8 右侧大腿根节点
        poses.add(PointF(googlePose[26].position3D.x, googlePose[26].position3D.y))//9 右膝盖
        poses.add(PointF(googlePose[28].position3D.x, googlePose[28].position3D.y))//10 右脚
        poses.add(PointF(googlePose[23].position3D.x, googlePose[23].position3D.y))//11 左侧大腿根节点
        poses.add(PointF(googlePose[25].position3D.x, googlePose[25].position3D.y))//12 左膝盖
        poses.add(PointF(googlePose[27].position3D.x, googlePose[27].position3D.y))//13 左脚
        //14 肚脐（双肩与双髋关节对角连线相交点）
        val navelPointF = lineLineIntersection(
            googlePose[12].position,
            googlePose[23].position,
            googlePose[11].position,
            googlePose[24].position
        )
        poses.add(PointF(navelPointF.x, navelPointF.y))

        return poses
    }

    /**
     * 获取
     * 15个骨骼点，pose角度
     */
    fun getPoseAngles(posePoint: ArrayList<PointF>?): ArrayList<Double> {
        val trunkPoseAngles = arrayListOf<Double>()
        if (!posePoint.isNullOrEmpty() && posePoint.size >= 15) {
            trunkPoseAngles.add(getAngle(posePoint[5], posePoint[2], posePoint[4]))
            trunkPoseAngles.add(getAngle(posePoint[2], posePoint[3], posePoint[4]))
            trunkPoseAngles.add(getAngle(posePoint[2], posePoint[5], posePoint[7]))
            trunkPoseAngles.add(getAngle(posePoint[5], posePoint[6], posePoint[7]))
            trunkPoseAngles.add(getAngle(posePoint[14], posePoint[8], posePoint[9]))
            trunkPoseAngles.add(getAngle(posePoint[8], posePoint[9], posePoint[10]))
            trunkPoseAngles.add(getAngle(posePoint[14], posePoint[11], posePoint[12]))
            trunkPoseAngles.add(getAngle(posePoint[11], posePoint[12], posePoint[13]))

            trunkPoseAngles.add(getAngle(posePoint[2], posePoint[5], posePoint[11]))
            trunkPoseAngles.add(getAngle(posePoint[5], posePoint[2], posePoint[8]))

            trunkPoseAngles.add(getAngle(posePoint[2], posePoint[8], posePoint[10]))
            trunkPoseAngles.add(getAngle(posePoint[5], posePoint[11], posePoint[13]))

            trunkPoseAngles.add(getAngle(posePoint[0], posePoint[14], posePoint[13]))
            trunkPoseAngles.add(getAngle(posePoint[0], posePoint[14], posePoint[10]))
        }

        return trunkPoseAngles
    }

    /**
     * 获取
     * 33个骨骼点，pose角度（按照15个骨骼点的角度个数）
     */
    fun getGooglePoseAngles(posePoint: ArrayList<PoseLandmark>?): ArrayList<Double> {
        val poseAngles = arrayListOf<Double>()
        if (!posePoint.isNullOrEmpty() && posePoint.size >= 33) {
            poseAngles.add(
                getAngle(
                    posePoint[11].position,
                    posePoint[12].position,
                    posePoint[16].position
                )
            )
            poseAngles.add(
                getAngle(
                    posePoint[12].position,
                    posePoint[14].position,
                    posePoint[16].position
                )
            )
            poseAngles.add(
                getAngle(
                    posePoint[12].position,
                    posePoint[11].position,
                    posePoint[15].position
                )
            )
            poseAngles.add(
                getAngle(
                    posePoint[11].position,
                    posePoint[13].position,
                    posePoint[15].position
                )
            )
            val navelPointF = lineLineIntersection(
                posePoint[12].position,
                posePoint[23].position,
                posePoint[11].position,
                posePoint[24].position
            )
            poseAngles.add(
                getAngle(
                    navelPointF,
                    posePoint[24].position,
                    posePoint[26].position
                )
            )
            poseAngles.add(
                getAngle(
                    posePoint[24].position,
                    posePoint[26].position,
                    posePoint[28].position
                )
            )
            poseAngles.add(
                getAngle(
                    navelPointF,
                    posePoint[23].position,
                    posePoint[25].position
                )
            )
            poseAngles.add(
                getAngle(
                    posePoint[23].position,
                    posePoint[25].position,
                    posePoint[27].position
                )
            )

            poseAngles.add(
                getAngle(
                    posePoint[12].position,
                    posePoint[11].position,
                    posePoint[23].position
                )
            )
            poseAngles.add(
                getAngle(
                    posePoint[11].position,
                    posePoint[12].position,
                    posePoint[24].position
                )
            )

            poseAngles.add(
                getAngle(
                    posePoint[12].position,
                    posePoint[24].position,
                    posePoint[28].position
                )
            )
            poseAngles.add(
                getAngle(
                    posePoint[11].position,
                    posePoint[23].position,
                    posePoint[27].position
                )
            )

            poseAngles.add(
                getAngle(
                    posePoint[0].position,
                    navelPointF,
                    posePoint[27].position
                )
            )
            poseAngles.add(
                getAngle(
                    posePoint[0].position,
                    navelPointF,
                    posePoint[28].position
                )
            )
        }

        return poseAngles
    }

    /**
     * 获取分值
     * 对比两个pose，分值
     * @param currentPose 33个骨骼点
     * @param referPose 33个参考点
     */
    fun getGooglePoseScore(
        currentPose: ArrayList<PoseLandmark>,
        referPose: ArrayList<PoseLandmark>
    ): Double {
        return getPoseScoreAngle(getGooglePoseAngles(currentPose), getGooglePoseAngles(referPose))
    }

    /**
     * 获取分值
     * 对比两个pose，获取分值
     * @param currentPose 15个骨骼点
     * @param referPose 15个参考点
     */
    fun getPoseScore(
        currentPose: ArrayList<PointF>,
        referPose: ArrayList<PointF>
    ): Double {
        return getPoseScoreAngle(getPoseAngles(currentPose), getPoseAngles(referPose))
    }

    /**
     * 获取分值
     * 对比两个pose
     * @param googlePose google 33个骨骼点
     * @param referPose 15个参考点
     */
    fun get33PoseAnd15PoseScore(
        googlePose: ArrayList<PoseLandmark>,
        referPose: ArrayList<PointF>
    ): Double {
        val googleTo15Point = googleTo15Point(googlePose)
        val poseAngles = getPoseAngles(googleTo15Point)
        val referPoseAngles = getPoseAngles(referPose)
        return getPoseScoreAngle(poseAngles, referPoseAngles)
    }

    /**
     * 两组角度数据遍历，获取对应的分数
     */
    fun getPoseScoreAngle(
        referPoseAngles: ArrayList<Double>,
        currentPoseAngles: ArrayList<Double>
    ): Double {
        val trunkScore = arrayListOf<Double>()
        referPoseAngles.forEachIndexed { index, d ->
            val isSame = currentPoseAngles[index] * d > 0
            if (isSame) {
                val vs = abs(abs(currentPoseAngles[index]) - abs(d))
                if (vs > 180) {
                    trunkScore.add(0.0)
                } else {
                    val abs = abs((vs / 180) * 100)

                    val score = 100 - abs
                    //角度相似超过50时
                    trunkScore.add(if (score >= 50) score else 0.0)
                    Log.d("AngleUtils", "abs:$score")
                }
            } else {
                trunkScore.add(0.0)
            }
        }

        Log.d("AngleUtils", "average:$trunkScore----${trunkScore.average()}")
        return trunkScore.average()
    }

    /**
     * 获取夹角
     * 三个点之间的夹角
     * @param firstPoint
     * @param midPoint
     * @param lastPoint
     */
    fun getAngle(firstPoint: PointF?, midPoint: PointF?, lastPoint: PointF?): Double {
        if (firstPoint == null || midPoint == null || lastPoint == null) return 0.0
        var result = Math.toDegrees(
            (atan2(lastPoint.y - midPoint.y, lastPoint.x - midPoint.x)
                    - atan2(firstPoint.y - midPoint.y, firstPoint.x - midPoint.x)).toDouble()
        )
        if (result < 0 && result > -180) result = abs(result) // Angle should never be negative
        if (result > 180) {
            result = 360.0 - result // Always get the acute representation of the angle
        }

        Log.d(TAG, "getAngle: $result")
        return result
    }

    /**
     * 计算两条直线相交点坐标
     * ab与cd
     */
    private fun lineLineIntersection(a: PointF, b: PointF, c: PointF, d: PointF): PointF {
        // Line AB represented as a1x + b1y = c1
        val a1 = (b.y - a.y)
        val b1 = (a.x - b.x)
        val c1 = a1 * a.x + b1 * a.y

        // Line CD represented as a2x + b2y = c2
        val a2 = (d.y - c.y)
        val b2 = (c.x - d.x)
        val c2 = a2 * c.x + b2 * c.y
        val determinant = a1 * b2 - a2 * b1
        return if (determinant == 0.0F) {
            // The lines are parallel. This is simplified
            // by returning a pair of FLT_MAX
            PointF(Float.MAX_VALUE, Float.MAX_VALUE)
        } else {
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            PointF(x, y)
        }
    }
}