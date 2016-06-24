LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT:= C:\VSProjects\OpenCV3-android
OPENCV_CAMERA_MODULES:=off
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES := FocusMeasure.cpp DkMath.cpp PageSegmentationUtils.cpp PageSegmentation.cpp DocScanInterface.cpp
LOCAL_LDLIBS += -llog
LOCAL_MODULE := docscan-native
LOCAL_CFLAGS += -std=c++11
LOCAL_LDFLAGS += -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
