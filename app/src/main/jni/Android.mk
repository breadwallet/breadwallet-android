LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := BreadWalletCore
LOCAL_SRC_FILES := native.c
include $(BUILD_SHARED_LIBRARY)