#include <jni.h>

//#define DEBUG_TAG "NDK_AndroidNDK1SampleActivity"
JNIEXPORT jstring Java_com_breadwallet_presenter_activities_MainActivity_messageFromNativeCode
        (JNIEnv *env, jobject this, jstring logThis) {
    const char *nativeString = (*env)->GetStringUTFChars(env, logThis, 0);
    return (*env)->NewStringUTF(env, nativeString);
}