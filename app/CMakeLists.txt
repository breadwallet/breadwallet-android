# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

# now build app's shared lib
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DANDROID_STL=gnustl_static -DANDROID_TOOLCHAIN=clang")

# -Wimplicit-function-declaration
# -Wno-missing-prototypes -Werror=return-type -Wdocumentation -Wunreachable-code-aggressive -Wno-missing-braces
# -Wparentheses -Wswitch -Wno-unused-function -Wunused-label -Wno-unused-parameter -Wunused-variable -Wunused-value -Wempty-body
# -Wconditional-uninitialized -Wno-unknown-pragmas -pedantic -Wshadow -Wfour-char-constants -Wno-conversion -Wconstant-conversion
# -Wint-conversion -Wbool-conversion -Wenum-conversion -Wassign-enum -Wno-shorten-64-to-32 -Wpointer-sign -Wnewline-eof
# -Wdeprecated-declarations -Wno-sign-conversion

add_library( # Specifies the name of the library.
             core-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/jni/breadwallet-core/BRAddress.c
             src/jni/breadwallet-core/BRAddress.h
             src/jni/breadwallet-core/BRArray.h
             src/jni/breadwallet-core/BRBase58.c
             src/jni/breadwallet-core/BRBase58.h
             src/jni/breadwallet-core/BRBIP32Sequence.c
             src/jni/breadwallet-core/BRBIP32Sequence.h
             src/jni/breadwallet-core/BRBIP38Key.c
             src/jni/breadwallet-core/BRBIP38Key.h
             src/jni/breadwallet-core/BRBIP39Mnemonic.c
             src/jni/breadwallet-core/BRBIP39Mnemonic.h
             src/jni/breadwallet-core/BRBIP39WordsEn.h
             src/jni/breadwallet-core/BRBloomFilter.c
             src/jni/breadwallet-core/BRBloomFilter.h
             src/jni/breadwallet-core/BRCrypto.c
             src/jni/breadwallet-core/BRCrypto.h
             src/jni/breadwallet-core/BRInt.h
             src/jni/breadwallet-core/BRKey.c
             src/jni/breadwallet-core/BRKey.h
             src/jni/breadwallet-core/BRMerkleBlock.c
             src/jni/breadwallet-core/BRMerkleBlock.h
             src/jni/breadwallet-core/BRPaymentProtocol.c
             src/jni/breadwallet-core/BRPaymentProtocol.h
             src/jni/breadwallet-core/BRPeer.c
             src/jni/breadwallet-core/BRPeer.h
             src/jni/breadwallet-core/BRPeerManager.c
             src/jni/breadwallet-core/BRPeerManager.h
             src/jni/breadwallet-core/BRSet.c
             src/jni/breadwallet-core/BRSet.h
             src/jni/breadwallet-core/BRTransaction.c
             src/jni/breadwallet-core/BRTransaction.h
             src/jni/breadwallet-core/BRWallet.c
             src/jni/breadwallet-core/BRWallet.h

             src/jni/transition/core.c
             src/jni/transition/core.h
             src/jni/transition/JNIBase58.c
             src/jni/transition/JNIBase58.h
             src/jni/transition/JNIBIP32Sequence.c
             src/jni/transition/JNIBIP32Sequence.h
             src/jni/transition/JNIKey.c
             src/jni/transition/JNIKey.h
             src/jni/transition/PeerManager.c
             src/jni/transition/PeerManager.h
             src/jni/transition/wallet.c
             src/jni/transition/wallet.h
             
                                        src/jni/breadwallet-core/Java/BRCoreJni.h
                                        src/jni/breadwallet-core/Java/BRCoreJni.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreJniReference.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreJniReference.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreAddress.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreAddress.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreChainParams.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreChainParams.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCorePeer.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCorePeer.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreMasterPubKey.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreMasterPubKey.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreKey.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreKey.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreMerkleBlock.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreMerkleBlock.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCorePeerManager.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCorePeerManager.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreTransaction.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreTransaction.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreTransactionInput.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreTransactionInput.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreTransactionOutput.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreTransactionOutput.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCorePaymentProtocol.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCorePaymentProtocol.h
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreWallet.c
                                        src/jni/breadwallet-core/Java/com_breadwallet_core_BRCoreWallet.h

             )


include_directories(src/jni/breadwallet-core/Java)
include_directories(src/jni/breadwallet-core/)
include_directories(src/jni/breadwallet-core/secp256k1/include)
include_directories(src/jni/breadwallet-core/secp256k1/src)
include_directories(src/jni/breadwallet-core/secp256k1)

#include_directories(src/main/secp/secp256k1/include/)
#include_directories(src/main/secp/secp256k1/src/)
#include_directories(src/main/secp/secp256k1/)
#include_directories(src/main/secp/)

# add lib dependencies
target_link_libraries(
    core-lib
    android
    log)
