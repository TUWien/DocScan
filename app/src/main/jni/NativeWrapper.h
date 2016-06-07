
#include <exception>

#include <jni.h>

#include <android/bitmap.h>

extern "C" {
    JNIEXPORT void JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeHandleFrame(JNIEnv * env, jclass cls, jint width, jint height, jbyteArray nv21Data, jobject bitmap);
}

template< class T, class TCpp >
class JavaArrayAccessor
{
public:
    JavaArrayAccessor(JNIEnv* env, T array) :
        env(env),
        array(array),
        data(reinterpret_cast< TCpp* >(env->GetPrimitiveArrayCritical(array, NULL))) // never returns NULL
    {}

    ~JavaArrayAccessor()
    {
        env->ReleasePrimitiveArrayCritical(array, data, 0);
    }

    TCpp* getData()
    {
        return data;
    }

private:
    JNIEnv* env;
    T array;
    TCpp* data;
};

class AndroidBitmapAccessorException : public std::exception
{
public:
    AndroidBitmapAccessorException(int code) :
        code(code)
    {}

    virtual ~AndroidBitmapAccessorException() throw()
    {}

    const int code;
};

class AndroidBitmapAccessor
{
public:
    AndroidBitmapAccessor(JNIEnv* env, jobject bitmap) throw(AndroidBitmapAccessorException):
        env(env),
        bitmap(bitmap),
        data(NULL)
    {
        int rv = AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast< void** >(&data));
        if(rv != ANDROID_BITMAP_RESULT_SUCCESS)
        {
            throw AndroidBitmapAccessorException(rv);
        }
    }

    ~AndroidBitmapAccessor()
    {
        if(data)
        {
            AndroidBitmap_unlockPixels(env, bitmap);
        }
    }

    unsigned char* getData()
    {
        return data;
    }

private:
    JNIEnv* env;
    jobject bitmap;
    unsigned char* data;
};



/*
#ifndef DOCSCAN_NATIVEWRAPPER_H
#define DOCSCAN_NATIVEWRAPPER_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeLogPolar
  (JNIEnv *, jclass, jlong, jlong, jfloat, jfloat, jdouble, jdouble, jdouble);

JNIEXPORT jstring JNICALL Java_at_ac_tuwien_caa_docscan_NativeWrapper_getStringFromNative
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif //DOCSCAN_NATIVEWRAPPER_H
*/