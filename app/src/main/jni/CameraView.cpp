//
// Created by fabian on 07.06.2016.
//

#include <jni.h>

void Java_com_athenasciences_demo_widgets_CameraView_handleFrame(JNIEnv *env,
                                                                 jobject thiz,
                                                                 jint width,
                                                                 jint height,
                                                                 jbyteArray nv21Data,
                                                                 jobject bitmap)
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