/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin.posedetector

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.odml.image.MlImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.demo.PoseAngleUtils
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.java.posedetector.classification.PoseClassifierProcessor
import com.google.mlkit.vision.demo.kotlin.VisionProcessorBase
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** A processor to run pose.kt detector. */
class PoseDetectorProcessor(
    private val context: Context,
    options: PoseDetectorOptionsBase,
    private val showInFrameLikelihood: Boolean,
    private val visualizeZ: Boolean,
    private val rescaleZForVisualization: Boolean,
    private val runClassification: Boolean,
    private val isStreamMode: Boolean
) : VisionProcessorBase<Pose>(context) {

    private val detector: PoseDetector
    private val classificationExecutor: Executor
    val score = MutableLiveData<Double>()

    private var poseClassifierProcessor: PoseClassifierProcessor? = null

    /** Internal class to hold Pose and classification results. */
    class PoseWithClassification(val pose: Pose, val classificationResult: List<String>)

    init {
        detector = PoseDetection.getClient(options)
        classificationExecutor = Executors.newSingleThreadExecutor()
    }

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage): Task<Pose> {
        return detector.process(image)
    }

    override fun detectInImage(image: MlImage): Task<Pose> {
        return detector.process(image)
    }

    override fun onSuccess(
        pose: Pose,
        graphicOverlay: GraphicOverlay
    ) {
        graphicOverlay.add(
            PoseGraphic(
                graphicOverlay,
                pose,
                showInFrameLikelihood,
                visualizeZ,
                rescaleZForVisualization)
        )

        val allPoseLandmarks = pose.allPoseLandmarks
        //左边：140.99289:147.85149---右边：121.878944:196.83575
        //左边：140.8273:39.963863---右边：113.53857:108.80207
        if (allPoseLandmarks.size >= 33) {
            val referencePoint = PoseAngleUtils.googleTo15Point(allPoseLandmarks)
            if (isStreamMode) {
                val start = System.currentTimeMillis()
                Log.d(TAG, "onSuccess: ")
                score.postValue(
                    PoseAngleUtils.getPoseScoreAngle(
                        PoseAngleUtils.trunkPoseAngles,
                        PoseAngleUtils.getPoseAngles(referencePoint)
                    )
                )
                Log.d(TAG, "onSuccess time: ${System.currentTimeMillis() - start}")
            } else {
                PoseAngleUtils.setCurrentPoint(PoseAngleUtils.googleTo15Point(allPoseLandmarks))
            }
//        }
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Pose detection failed!", e)
    }

    override fun isMlImageEnabled(context: Context?): Boolean {
        // Use MlImage in Pose Detection by default, change it to OFF to switch to InputImage.
        return true
    }

    companion object {
        private val TAG = "PoseDetectorProcessor"
    }
}
