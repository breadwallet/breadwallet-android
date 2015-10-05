LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := BreadWalletCore
LOCAL_SRC_FILES := core.c
include $(BUILD_SHARED_LIBRARY)