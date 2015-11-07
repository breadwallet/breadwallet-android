LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := ./transition/core.c

LOCAL_MODULE := core

LOCAL_C_INCLUDES := /Users/Mihail/Library/Android/android-ndk-r10e/platforms/android-21/arch-arm64/usr/include

LOCAL_SHARED_LIBRARIES := bread stdlib

LOCAL_CFLAGS := -std=c99

include $(BUILD_SHARED_LIBRARY)

#static module______________________________

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
./breadwallet-core/BRAddress.c\
./breadwallet-core/BRBase58.c\
./breadwallet-core/BRBIP32Sequence.c\
./breadwallet-core/BRBIP38Key.c\
./breadwallet-core/BRBIP39Mnemonic.c\
./breadwallet-core/BRBloomFilter.c\
./breadwallet-core/BRHash.c\
./breadwallet-core/BRKey.c\
./breadwallet-core/BRMerkleBlock.c\
./breadwallet-core/BRPaymentProtocol.c\
./breadwallet-core/BRPeer.c\
./breadwallet-core/BRPeerManager.c\
./breadwallet-core/BRSet.c\
./breadwallet-core/BRSort.c\
./breadwallet-core/BRTransaction.c\
./breadwallet-core/BRWallet.c

LOCAL_C_INCLUDES := \
$(LOCAL_PATH)/breadwallet-core/secp256k1\
$(LOCAL_PATH)/breadwallet-core\

LOCAL_MODULE := bread

LOCAL_CFLAGS := -std=c99

LOCAL_EXPORT_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
