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


JNIEXPORT jobjectArray JNICALL Java_at_ac_tuwien_caa_docscan_camera_cv_NativeWrapper_nativeGetPageSegmentation(JNIEnv * env, jclass cls, jlong src, jboolean useLab, jobject jOldRect) {

    dsc::DkPolyRect oldRect = cvt::jPolyRectToC(env, jOldRect);

    // call the main function:
    std::vector<dsc::DkPolyRect> polyRects = dsc::DkPageSegmentation::apply(*((cv::Mat*)src), useLab, oldRect);

    // JNI type signatures: http://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/types.html
    jclass jPolyRectClass = env->FindClass("at/ac/tuwien/caa/docscan/camera/cv/DkPolyRect");

    // "(FFFFFFFF)V" -> (8 x float) return void
    jmethodID cnstrctr = env->GetMethodID(
        jPolyRectClass,
        "<init>",
        "(FFFFFFFFII)V");

    if (cnstrctr == 0)
        __android_log_write(ANDROID_LOG_INFO, "DkPageSegmentation", "did not find constructor!");

    // convert the polyRects vector to a Java array:
    jobjectArray outJRects = env->NewObjectArray(polyRects.size(), jPolyRectClass, NULL);

    //jobject patch1 = env->NewObject(patchClass, cnstrctr, 1, 42, 3, 4, 5.4);
    jobject polyRect;


    for (int i = 0; i < polyRects.size(); i++) {

        std::vector<cv::Point> points = polyRects[i].toCvPoints();

         if (points.empty())
            continue;

        polyRect = env->NewObject(
            jPolyRectClass,
            cnstrctr,
            (float) points[0].x,
            (float) points[0].y,
            (float) points[1].x,
            (float) points[1].y,
            (float) points[2].x,
            (float) points[2].y,
            (float) points[3].x,
            (float) points[3].y,
            polyRects[i].channel(),
            polyRects[i].threshold());

        env->SetObjectArrayElement(outJRects, i, polyRect);
    }

    return outJRects;

}

JNIEXPORT jobjectArray JNICALL Java_at_ac_tuwien_caa_docscan_camera_cv_NativeWrapper_nativeGetFocusMeasures(JNIEnv * env, jclass cls, jlong src) {

    try {
        // call the main function:
        std::vector<dsc::Patch> patches = dsc::FocusEstimation::apply(*((cv::Mat *) src));

        // find the Java Patch class and its constructor:
        jclass patchClass = env->FindClass("at/ac/tuwien/caa/docscan/camera/cv/Patch");

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
    catch (std::exception e) {

        return NULL;

    }

}

namespace cvt {

dsc::DkPolyRect jPolyRectToC(JNIEnv * env, jobject jRect) {

    jclass jRectClass = env->GetObjectClass(jRect);

    std::vector<cv::Point> pts = std::vector<cv::Point>();

    float x1 = env->CallFloatMethod(jRect, env->GetMethodID(jRectClass, "getX1", "()F"));
    float y1 = env->CallFloatMethod(jRect, env->GetMethodID(jRectClass, "getY1", "()F"));
    pts.push_back(cv::Point(x1, y1));

    float x2 = env->CallFloatMethod(jRect, env->GetMethodID(jRectClass, "getX2", "()F"));
    float y2 = env->CallFloatMethod(jRect, env->GetMethodID(jRectClass, "getY2", "()F"));
    pts.push_back(cv::Point(x2, y2));

    float x3 = env->CallFloatMethod(jRect, env->GetMethodID(jRectClass, "getX3", "()F"));
    float y3 = env->CallFloatMethod(jRect, env->GetMethodID(jRectClass, "getY3", "()F"));
    pts.push_back(cv::Point(x3, y3));

    float x4 = env->CallFloatMethod(jRect, env->GetMethodID(jRectClass, "getX4", "()F"));
    float y4 = env->CallFloatMethod(jRect, env->GetMethodID(jRectClass, "getY4", "()F"));
    pts.push_back(cv::Point(x4, y4));

    int chl = env->CallIntMethod(jRect, env->GetMethodID(jRectClass, "channel", "()I"));
    int thr = env->CallIntMethod(jRect, env->GetMethodID(jRectClass, "threshold", "()I"));

    dsc::DkPolyRect p = dsc::DkPolyRect(pts);
    p.setChannel(chl);
    p.setThreshold(thr);

    return p;

}

}

#endif // #ifndef NO_JNI