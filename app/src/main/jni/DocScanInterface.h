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

#pragma once

#ifndef NO_JNI

#include <jni.h>

extern "C" {
    JNIEXPORT jobjectArray JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetFocusMeasures(JNIEnv *, jclass, jlong);
    JNIEXPORT jobjectArray JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetPageSegmentation(JNIEnv *, jclass, jlong, jboolean useLab);
    JNIEXPORT jdouble JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetIllumination(JNIEnv *, jclass, jlong, jobject);

    JNIEXPORT void JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetPageSegmentationTest(JNIEnv*, jobject, jint, jint, jbyteArray, jintArray);
}

#endif // #ifndef NO_JNI