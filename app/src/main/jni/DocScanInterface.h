//
// Created by markus on 23.06.2016.
//

#pragma once

#include <jni.h>

extern "C" {
    JNIEXPORT jobjectArray JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetPatches(JNIEnv *, jclass, jlong);
}