LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := core

LOCAL_CFLAGS := -std=c99 -DBITCOIN_TESTNET=0 -Wno-trigraphs -Wmissing-field-initializers -Wno-missing-prototypes -Werror=return-type -Wdocumentation -Wunreachable-code-aggressive -Wno-missing-braces -Wparentheses -Wswitch -Wno-unused-function -Wunused-label -Wno-unused-parameter -Wunused-variable -Wunused-value -Wempty-body -Wconditional-uninitialized -Wno-unknown-pragmas -pedantic -Wshadow -Wfour-char-constants -Wno-conversion -Wconstant-conversion -Wint-conversion -Wbool-conversion -Wenum-conversion -Wassign-enum -Wshorten-64-to-32 -Wpointer-sign -Wnewline-eof -Wdeprecated-declarations -Wno-sign-conversion

LOCAL_LDLIBS := -llog -lm

LOCAL_SHARED_LIBRARIES := bread

LOCAL_SRC_FILES := \
./transition/core.c\
./transition/wallet.c\
./transition/PeerManager.c

//TODO -DBITCOIN_TESTNET=1 (TESTING)

include $(BUILD_SHARED_LIBRARY)

#______________________________

include $(CLEAR_VARS)

LOCAL_MODULE := bread

LOCAL_CFLAGS := -std=c99 -DBITCOIN_TESTNET=0 -Wno-trigraphs -Wmissing-field-initializers -Wno-missing-prototypes -Werror=return-type -Wdocumentation -Wunreachable-code-aggressive -Wno-missing-braces -Wparentheses -Wswitch -Wno-unused-function -Wunused-label -Wno-unused-parameter -Wunused-variable -Wunused-value -Wempty-body -Wconditional-uninitialized -Wno-unknown-pragmas -pedantic -Wshadow -Wfour-char-constants -Wno-conversion -Wconstant-conversion -Wint-conversion -Wbool-conversion -Wenum-conversion -Wassign-enum -Wshorten-64-to-32 -Wpointer-sign -Wnewline-eof -Wdeprecated-declarations -Wno-sign-conversion
LOCAL_C_INCLUDES := \
$(LOCAL_PATH)/breadwallet-core/secp256k1\
$(LOCAL_PATH)/breadwallet-core

LOCAL_LDLIBS := -llog -lm

LOCAL_EXPORT_C_INCLUDES := \
$(LOCAL_PATH)/breadwallet-core\
$(LOCAL_PATH)/breadwallet-core/secp256k1

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

include $(BUILD_SHARED_LIBRARY)