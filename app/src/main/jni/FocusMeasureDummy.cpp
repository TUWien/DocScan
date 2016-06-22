#include <jni.h>
#include "FocusMeasureDummy.h"

#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>

inline void nativeProcessFrame(cv::Mat src) {

    __android_log_write(ANDROID_LOG_ERROR, "Tag", "Error msg");

}


JNIEXPORT void JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeProcessFrame(JNIEnv * env, jclass cls, jlong src) {

    nativeProcessFrame(*((cv::Mat*)src));
}