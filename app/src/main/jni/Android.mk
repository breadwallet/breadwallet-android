LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := BreadWalletCore
LOCAL_SRC_FILES := $(LOCAL_PATH)/transition/core.h

LOCAL_C_INCLUDES := $(LOCAL_PATH)/breadwallet-core/secp256k1
include $(BUILD_SHARED_LIBRARY)

