
#ifndef NO_JNI

#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <android/log.h>
#include <time.h>
#include "NativeWrapper.h"

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"opencv-jni",__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"opencv-jni",__VA_ARGS__)

double lastTime;

extern "C" {
/*
JNIEXPORT void JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_handleFrame(JNIEnv* env, jobject thiz, jint width, jint height, jbyteArray yuv, jintArray bgra)
{
    jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
    jint*  _bgra = env->GetIntArrayElements(bgra, 0);

    Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);

    Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
    Mat mgray(height, width, CV_8UC1, (unsigned char *)_yuv);

    cv::flip(mbgra, mbgra, 0);

    //Please make attention about BGRA byte order
    //ARGB stored in java as int array becomes BGRA at native level
    cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);

    vector<KeyPoint> v;


    circle(mbgra, Point(100, 100), 10, Scalar(0,0,255,255));


    env->ReleaseIntArrayElements(bgra, _bgra, 0);
    env->ReleaseByteArrayElements(yuv, _yuv, 0);

	 struct timespec res;
	 clock_gettime(CLOCK_MONOTONIC, &res);

	 double currentTime = 1000.0*res.tv_sec + (double)res.tv_nsec/1e6;
	 double timeDelta = currentTime-lastTime;
	 lastTime = currentTime;
	 //LOGI("FPS: %f",timeDelta);
}
*/
JNIEXPORT void Java_at_ac_tuwien_caa_docscan_NativeWrapper_handleFrame2(JNIEnv *env, jobject thiz, jint width, jint height, jbyteArray nv21Data, jobject bitmap)
{
    try
    {
        // create output rgba-formatted output Mat object using the raw Java data
        // allocated by the Bitmap object to prevent an extra memcpy. note that
        // the bitmap must be created in ARGB_8888 format
        AndroidBitmapAccessor bitmapAccessor(env, bitmap);
        cv::Mat rgba(height, width, CV_8UC4, bitmapAccessor.getData());

        // create input nv21-formatted input Mat object using the raw Java data to
        // prevent extraneous allocations. note the use of height*1.5 to account
        // for the nv21 (YUV420) formatting
        JavaArrayAccessor< jbyteArray, uchar > nv21Accessor(env, nv21Data);
        cv::Mat nv21(height * 1.5, width, CV_8UC1, nv21Accessor.getData());

        // initialize the rgba output using the nv21 data
        cv::cvtColor(nv21, rgba, CV_YUV2RGBA_NV21);
        circle(rgba, Point(100, 100), 10, Scalar(0,0,255,255));

        // convert the nv21 image to grayscale by lopping off the extra 0.5*height bits. note
        // this this ctor is smart enough to not actually copy the data
        cv::Mat gray(nv21, cv::Rect(0, 0, width, height));

        // do your processing on the nv21 and/or grayscale image here, making sure to update the
        // rgba mat with the appropriate output
    }
    catch(const AndroidBitmapAccessorException& e)
    {
        LOGE("error locking bitmap: %d", e.code);
    }
}

}

#endif	// #ifndef NO_JNI