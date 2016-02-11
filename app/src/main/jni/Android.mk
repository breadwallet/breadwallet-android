LOCAL_PATH := $(call my-dir)

#//TODO take out the  -DBITCOIN_TESTNET=1 from flags (TESTING)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := \
$(LOCAL_PATH)/transition\
$(LOCAL_PATH)/breadwallet-core

LOCAL_SRC_FILES := \
./transition/WalletCallbacks.c\
./transition/core.c\
./transition/wallet.c\
./transition/PeerManager.c

LOCAL_MODULE := core

LOCAL_LDLIBS := -llog -lm

LOCAL_SHARED_LIBRARIES := bread

LOCAL_CFLAGS := -std=c99 -DBITCOIN_TESTNET=1

include $(BUILD_SHARED_LIBRARY)

#______________________________

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
./breadwallet-core/BRTransaction.c\
./breadwallet-core/BRWallet.c\
./breadwallet-core/test.c

LOCAL_C_INCLUDES := \
$(LOCAL_PATH)/breadwallet-core/secp256k1\
$(LOCAL_PATH)/breadwallet-core

LOCAL_MODULE := bread

LOCAL_LDLIBS := -llog -lm

LOCAL_EXPORT_C_INCLUDES := \
$(LOCAL_PATH)/breadwallet-core\
$(LOCAL_PATH)/breadwallet-core/secp256k1

LOCAL_CFLAGS := -std=c99 -DBITCOIN_TESTNET=1

include $(BUILD_SHARED_LIBRARY)
