/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   16. June 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

#ifndef NO_JNI

#include "DocScanInterface.h"
#include "FocusMeasure.h"
#include "PageSegmentation.h"

#include <android/log.h>
#include <jni.h>

#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>

#include <sstream>
#include <string>
#include <iostream>


JNIEXPORT void JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetPageSegmentationTest(JNIEnv* env, jobject thiz, jint width, jint height, jbyteArray yuv, jintArray bgra)
{


    jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
    jint*  _bgra = env->GetIntArrayElements(bgra, 0);

    cv::Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
    cv::Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
    cv::Mat mgray(height, width, CV_8UC1, (unsigned char *)_yuv);

    //Please make attention about BGRA byte order
    //ARGB stored in java as int array becomes BGRA at native level
    cv::cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);

    dsc::Utils::print("asdf", "asf");
    // call the main function:

/*
    std::vector<dsc::DkPolyRect> polyRects = dsc::DkPageSegmentation::apply(mbgra);

    jclass polyRectClass = env->FindClass("at/ac/tuwien/caa/docscan/cv/DkPolyRect");

    // JNI type signatures: http://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/types.html

    // "(FFFFFFFF)V" -> (8 x float) return void
    jmethodID cnstrctr = env->GetMethodID(polyRectClass, "<init>", "(FFFFFFFF)V");

    if (cnstrctr == 0)
        __android_log_write(ANDROID_LOG_INFO, "DkPageSegmentation", "did not find constructor!");

    // convert the polyRects vector to a Java array:
    jobjectArray outJNIArray = env->NewObjectArray(polyRects.size(), polyRectClass, NULL);

    //jobject patch1 = env->NewObject(patchClass, cnstrctr, 1, 42, 3, 4, 5.4);
    jobject polyRect;


    for (int i = 0; i < polyRects.size(); i++) {

        std::vector<cv::Point> points = polyRects[i].toCvPoints();

        // TODO: check why this happens:
        if (points.empty()) {

            std::stringstream strs;
            strs << i;
            std::string temp_str = strs.str();
            char* char_type = (char*) temp_str.c_str();

            __android_log_write(ANDROID_LOG_INFO, "DocScanInterfaceEmpty", char_type);

            continue;


        }
        else {

            std::stringstream strs;
            strs << i;
            std::string temp_str = strs.str();
            char* char_type = (char*) temp_str.c_str();

            __android_log_write(ANDROID_LOG_INFO, "DocScanInterfaceNotEmpty", char_type);

        }

        polyRect = env->NewObject(polyRectClass, cnstrctr,
            (float) points[0].x, (float)  points[0].y, (float) points[1].x, (float) points[1].y, (float) points[2].x, (float) points[2].y, (float) points[3].x, (float) points[3].y);

        env->SetObjectArrayElement(outJNIArray, i, polyRect);


    }

    /*
    std::stringstream strs;
    strs << polyRects.size();
    std::string temp_str = strs.str();
    char* char_type = (char*) temp_str.c_str();

    __android_log_write(ANDROID_LOG_INFO, "DocScanInterface", char_type);
    */



    //return outJNIArray;



    env->ReleaseIntArrayElements(bgra, _bgra, 0);
    env->ReleaseByteArrayElements(yuv, _yuv, 0);

}

JNIEXPORT jobjectArray JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetPageSegmentation(JNIEnv * env, jclass cls, jlong src) {




    // call the main function:
    std::vector<dsc::DkPolyRect> polyRects = dsc::DkPageSegmentation::apply(*((cv::Mat*)src));

    jclass polyRectClass = env->FindClass("at/ac/tuwien/caa/docscan/cv/DkPolyRect");

    // JNI type signatures: http://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/types.html

    // "(FFFFFFFF)V" -> (8 x float) return void
    jmethodID cnstrctr = env->GetMethodID(polyRectClass, "<init>", "(FFFFFFFF)V");

    if (cnstrctr == 0)
        __android_log_write(ANDROID_LOG_INFO, "DkPageSegmentation", "did not find constructor!");

    // convert the polyRects vector to a Java array:
    jobjectArray outJNIArray = env->NewObjectArray(polyRects.size(), polyRectClass, NULL);

    //jobject patch1 = env->NewObject(patchClass, cnstrctr, 1, 42, 3, 4, 5.4);
    jobject polyRect;


    for (int i = 0; i < polyRects.size(); i++) {

        std::vector<cv::Point> points = polyRects[i].toCvPoints();

        // TODO: check why this happens:
        if (points.empty()) {

            std::stringstream strs;
            strs << i;
            std::string temp_str = strs.str();
            char* char_type = (char*) temp_str.c_str();

            __android_log_write(ANDROID_LOG_INFO, "DocScanInterfaceEmpty", char_type);

            continue;


        }
        else {

            std::stringstream strs;
            strs << i;
            std::string temp_str = strs.str();
            char* char_type = (char*) temp_str.c_str();

            __android_log_write(ANDROID_LOG_INFO, "DocScanInterfaceNotEmpty", char_type);

        }

        polyRect = env->NewObject(polyRectClass, cnstrctr,
            (float) points[0].x, (float)  points[0].y, (float) points[1].x, (float) points[1].y, (float) points[2].x, (float) points[2].y, (float) points[3].x, (float) points[3].y);

        env->SetObjectArrayElement(outJNIArray, i, polyRect);


    }

    /*
    std::stringstream strs;
    strs << polyRects.size();
    std::string temp_str = strs.str();
    char* char_type = (char*) temp_str.c_str();

    __android_log_write(ANDROID_LOG_INFO, "DocScanInterface", char_type);
    */



    return outJNIArray;

}

JNIEXPORT jobjectArray JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetFocusMeasures(JNIEnv * env, jclass cls, jlong src) {


    // call the main function:
    std::vector<dsc::Patch> patches = dsc::FocusEstimation::apply(*((cv::Mat*)src));

    // find the Java Patch class and its constructor:
    jclass patchClass = env->FindClass("at/ac/tuwien/caa/docscan/cv/Patch");

    // JNI type signatures: http://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/types.html
    // "(FFIIDZZ)V" -> (float float int int double boolean boolean) return void
    jmethodID cnstrctr = env->GetMethodID(patchClass, "<init>", "(FFIIDZZ)V");


    // convert the patches vector to a Java array:
    jobjectArray outJNIArray = env->NewObjectArray(patches.size(), patchClass, NULL);

    //jobject patch1 = env->NewObject(patchClass, cnstrctr, 1, 42, 3, 4, 5.4);
    jobject patch;

    for (int i = 0; i < patches.size(); i++) {
        patch = env->NewObject(patchClass, cnstrctr, patches[i].centerX(), patches[i].centerY(),
            patches[i].width(), patches[i].height(), patches[i].fm(),
            patches[i].isSharp(), patches[i].foreground());
        env->SetObjectArrayElement(outJNIArray, i, patch);
    }



    // set the returned array:


    //setObjectArrayElement(env*, outJNIArray, 0, patch1);

/*
    if (cnstrctr == 0)
        __android_log_write(ANDROID_LOG_INFO, "FocusMeasure", "did not find constructor.");

    else
        __android_log_write(ANDROID_LOG_INFO, "FocusMeasure", "found constructor!");
*/

    //return env->NewObject(c, cnstrctr, 1, 2, 3, 4, 5.4);
    return outJNIArray;

}

#endif // #ifndef NO_JNI