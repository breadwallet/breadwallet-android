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
             src/main/jni/breadwallet-core/BRAddress.c
             src/main/jni/breadwallet-core/BRAddress.h
             src/main/jni/breadwallet-core/BRArray.h
             src/main/jni/breadwallet-core/BRBase58.c
             src/main/jni/breadwallet-core/BRBase58.h
             src/main/jni/breadwallet-core/BRBech32.c
             src/main/jni/breadwallet-core/BRBech32.h
             src/main/jni/breadwallet-core/BRBIP32Sequence.c
             src/main/jni/breadwallet-core/BRBIP32Sequence.h
             src/main/jni/breadwallet-core/BRBIP38Key.c
             src/main/jni/breadwallet-core/BRBIP38Key.h
             src/main/jni/breadwallet-core/BRBIP39Mnemonic.c
             src/main/jni/breadwallet-core/BRBIP39Mnemonic.h
             src/main/jni/breadwallet-core/BRBIP39WordsEn.h
             src/main/jni/breadwallet-core/BRBloomFilter.c
             src/main/jni/breadwallet-core/BRBloomFilter.h
             src/main/jni/breadwallet-core/BRCrypto.c
             src/main/jni/breadwallet-core/BRCrypto.h
             src/main/jni/breadwallet-core/BRInt.h
             src/main/jni/breadwallet-core/BRKey.c
             src/main/jni/breadwallet-core/BRKey.h
             src/main/jni/breadwallet-core/BRMerkleBlock.c
             src/main/jni/breadwallet-core/BRMerkleBlock.h
             src/main/jni/breadwallet-core/BRPaymentProtocol.c
             src/main/jni/breadwallet-core/BRPaymentProtocol.h
             src/main/jni/breadwallet-core/BRPeer.c
             src/main/jni/breadwallet-core/BRPeer.h
             src/main/jni/breadwallet-core/BRPeerManager.c
             src/main/jni/breadwallet-core/BRPeerManager.h
             src/main/jni/breadwallet-core/BRSet.c
             src/main/jni/breadwallet-core/BRSet.h
             src/main/jni/breadwallet-core/BRTransaction.c
             src/main/jni/breadwallet-core/BRTransaction.h
             src/main/jni/breadwallet-core/BRWallet.c
             src/main/jni/breadwallet-core/BRWallet.h

             src/main/jni/transition/core.c
             src/main/jni/transition/core.h
             src/main/jni/transition/JNIBase58.c
             src/main/jni/transition/JNIBase58.h
             src/main/jni/transition/JNIBIP32Sequence.c
             src/main/jni/transition/JNIBIP32Sequence.h
             src/main/jni/transition/JNIKey.c
             src/main/jni/transition/JNIKey.h
             src/main/jni/transition/PeerManager.c
             src/main/jni/transition/PeerManager.h
             src/main/jni/transition/wallet.c
             src/main/jni/transition/wallet.h)


include_directories(src/main/jni/breadwallet-core/)
include_directories(src/main/secp/secp256k1/include/)
include_directories(src/main/secp/secp256k1/src/)
include_directories(src/main/secp/secp256k1/)
include_directories(src/main/secp/)

# add lib dependencies
target_link_libraries(
    core-lib
    android
    log)
