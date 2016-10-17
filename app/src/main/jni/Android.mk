LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
# include file with user defined OpenCV SDK path (MY_OPENCVROOT):
include $(LOCAL_PATH)/local/Android.mk
OPENCVROOT:= $(MY_OPENCVROOT)

OPENCV_CAMERA_MODULES:=off
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES := DocScanInterface.cpp FocusMeasure.cpp DkMath.cpp PageSegmentationUtils.cpp PageSegmentation.cpp Utils.cpp Illumination.cpp
LOCAL_LDLIBS += -llog
LOCAL_MODULE := docscan-native
LOCAL_CFLAGS += -std=c++11
LOCAL_LDFLAGS += -ljnigraphics

LOCAL_CPPFLAGS += -DJNI_BUILD

include $(BUILD_SHARED_LIBRARY)
