//
// Created by markus on 23.06.2016.
//

#include "DocScanInterface.h"
#include "FocusMeasure.h"

#include <android/log.h>
#include <jni.h>

#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>

#include <sstream>
#include <string>
#include <iostream>

JNIEXPORT jobjectArray JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetPatches(JNIEnv * env, jclass cls, jlong src) {


    // call the main function:
    std::vector<rdf::Patch> patches = rdf::getPatches(*((cv::Mat*)src));

    // find the Java Patch class and its constructor:
    jclass patchClass = env->FindClass("at/ac/tuwien/caa/docscan/cv/Patch");
    jmethodID cnstrctr = env->GetMethodID(patchClass, "<init>", "(IIIID)V");

    // convert the patches vector to a Java array:
    jobjectArray outJNIArray = env->NewObjectArray(patches.size(), patchClass, NULL);

    //jobject patch1 = env->NewObject(patchClass, cnstrctr, 1, 42, 3, 4, 5.4);
    jobject patch;
    for (int i = 0; i < patches.size(); i++) {
        patch = env->NewObject(patchClass, cnstrctr, patches[i].upperLeftX(), patches[i].upperLeftY(),
            patches[i].width(), patches[i].height(), patches[i].fm());
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
