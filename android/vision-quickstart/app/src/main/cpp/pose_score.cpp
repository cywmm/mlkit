#include <jni.h>
#include <string>
#include <cmath>
#include <vector>
#include <iostream>

#include <android/log.h>

#define TAG "native-lib" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

float get_angle(float firstX, float firstY, float midX, float midY, float lastX, float lastY);

extern "C" JNIEXPORT jstring JNICALL
Java_com_aidong_posescore_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_aidong_posescore_MainActivity_sum(JNIEnv *env, jobject thiz, jint a, jint b) {
    return a + b;
}

float get_angle(float firstX, float firstY, float midX, float midY, float lastX, float lastY) {
    double theta = atan2(lastY - midY, lastX - midX)
                   - atan2(firstY - midY, firstX - midX);
    if (theta > M_PI)
        theta -= 2 * M_PI;
    if (theta < -M_PI)
        theta += 2 * M_PI;

    theta = abs(theta * 180.0 / M_PI);
//    LOGD("%s", std::to_string(theta).c_str());
    return theta;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aidong_posescore_MainActivity_getAngle(JNIEnv *env, jobject thiz, jfloatArray x_list,
                                                jfloatArray y_list, jfloatArray xy_list) {
//    PointF firstPoint(0, 3);
//    PointF midPoint(0, 0);
//    PointF lastPoint(9, 0);
//
//    get_angle(firstPoint.x, firstPoint.y, midPoint.x, midPoint.y, lastPoint.x, lastPoint.y);

    //数组求和
//    float result = 0;
//
//    //方式1  推荐使用
//    jint arr_len = env->GetArrayLength(x_list);
//    //动态申请数组
//    jfloat *c_array = (jfloat *) malloc(arr_len * sizeof(jint));
//    //初始化数组元素内容为0
//    memset(c_array, 0, sizeof(jint) * arr_len);
//    //将java数组的[0-arr_len)位置的元素拷贝到c_array数组中
//    env->GetFloatArrayRegion(x_list, 0, arr_len, c_array);
//    for (int i = 0; i < arr_len; ++i) {
//        result += c_array[i];
//    }
//    //动态申请的内存 必须释放
//    free(c_array);

    //此种方式比较危险,GetIntArrayElements会直接获取数组元素指针,是可以直接对该数组元素进行修改的.
    jfloat *x_arr = env->GetFloatArrayElements(x_list, NULL);
    jfloat *y_arr = env->GetFloatArrayElements(y_list, NULL);
    jfloat *xy_arr = env->GetFloatArrayElements(xy_list, NULL);
    if (x_arr == nullptr || y_arr == nullptr) {
        return;
    }

    jint lenX = env->GetArrayLength(x_list);
    jint lenY = env->GetArrayLength(y_list);
    if (lenX == 15 && lenY == 15) {
        xy_arr[0] = get_angle(x_arr[5], y_arr[5], x_arr[2], y_arr[2], x_arr[3], y_arr[3]);
        xy_arr[1] = get_angle(x_arr[2], y_arr[2], x_arr[3], y_arr[3], x_arr[4], y_arr[4]);
        xy_arr[2] = get_angle(x_arr[2], y_arr[2], x_arr[5], y_arr[5], x_arr[6], y_arr[6]);
        xy_arr[3] = get_angle(x_arr[5], y_arr[5], x_arr[6], y_arr[6], x_arr[7], y_arr[7]);
        xy_arr[4] = get_angle(x_arr[8], y_arr[8], x_arr[9], y_arr[9], x_arr[10], y_arr[10]);
        xy_arr[5] = get_angle(x_arr[11], y_arr[11], x_arr[12], y_arr[12], x_arr[13], y_arr[13]);
        xy_arr[6] = get_angle(x_arr[2], y_arr[2], x_arr[5], y_arr[5], x_arr[11], y_arr[11]);
        xy_arr[7] = get_angle(x_arr[2], y_arr[2], x_arr[8], y_arr[8], x_arr[9], y_arr[9]);
        xy_arr[8] = get_angle(x_arr[5], y_arr[5], x_arr[11], y_arr[11], x_arr[12], y_arr[12]);
        xy_arr[9] = get_angle(x_arr[5], y_arr[5], x_arr[2], y_arr[2], x_arr[8], y_arr[8]);
    }
    //有Get,一般就有Release
    env->ReleaseFloatArrayElements(x_list, x_arr, 0);
    env->ReleaseFloatArrayElements(y_list, y_arr, 0);
    env->ReleaseFloatArrayElements(xy_list, xy_arr, 0);
}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_aidong_posescore_MainActivity_getScore(JNIEnv *env, jobject thiz, jfloatArray a,
                                                jfloatArray b) {
    jfloat *x_arr = env->GetFloatArrayElements(a, NULL);
    jfloat *y_arr = env->GetFloatArrayElements(b, NULL);
    float trunkScore = 0;
    if (x_arr == nullptr || y_arr == nullptr) {
        return trunkScore;
    }

    jint lenX = env->GetArrayLength(a);
    jint lenY = env->GetArrayLength(b);

    if (lenX == 10 && lenY == 10) {
        for (int i = 0; i < lenX; ++i) {
            auto vs = abs(abs(x_arr[i]) - y_arr[i]);
            auto abss = abs((vs / 180) * 100);

            float score = 100 - abss;
            //角度相似超过50时
            trunkScore += score;
        }
    }

    return trunkScore / lenX;
}