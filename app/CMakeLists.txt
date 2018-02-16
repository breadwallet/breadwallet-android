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
             src/main/jni/digibytewallet-core/BRAddress.c
             src/main/jni/digibytewallet-core/BRAddress.h
             src/main/jni/digibytewallet-core/BRArray.h
             src/main/jni/digibytewallet-core/BRBase58.c
             src/main/jni/digibytewallet-core/BRBase58.h
             src/main/jni/digibytewallet-core/BRBIP32Sequence.c
             src/main/jni/digibytewallet-core/BRBIP32Sequence.h
             src/main/jni/digibytewallet-core/BRBIP38Key.c
             src/main/jni/digibytewallet-core/BRBIP38Key.h
             src/main/jni/digibytewallet-core/BRBIP39Mnemonic.c
             src/main/jni/digibytewallet-core/BRBIP39Mnemonic.h
             src/main/jni/digibytewallet-core/BRBIP39WordsEn.h
             src/main/jni/digibytewallet-core/BRBloomFilter.c
             src/main/jni/digibytewallet-core/BRBloomFilter.h
             src/main/jni/digibytewallet-core/BRChainParams.h
             src/main/jni/digibytewallet-core/BRCrypto.c
             src/main/jni/digibytewallet-core/BRCrypto.h
             src/main/jni/digibytewallet-core/BRInt.h
             src/main/jni/digibytewallet-core/BRKey.c
             src/main/jni/digibytewallet-core/BRKey.h
             src/main/jni/digibytewallet-core/BRMerkleBlock.c
             src/main/jni/digibytewallet-core/BRMerkleBlock.h
             src/main/jni/digibytewallet-core/BRPaymentProtocol.c
             src/main/jni/digibytewallet-core/BRPaymentProtocol.h
             src/main/jni/digibytewallet-core/BRPeer.c
             src/main/jni/digibytewallet-core/BRPeer.h
             src/main/jni/digibytewallet-core/BRPeerManager.c
             src/main/jni/digibytewallet-core/BRPeerManager.h
             src/main/jni/digibytewallet-core/BRSet.c
             src/main/jni/digibytewallet-core/BRSet.h
             src/main/jni/digibytewallet-core/BRTransaction.c
             src/main/jni/digibytewallet-core/BRTransaction.h
             src/main/jni/digibytewallet-core/BRWallet.c
             src/main/jni/digibytewallet-core/BRWallet.h
             src/main/jni/digibytewallet-core/crypto/sha256.h
             src/main/jni/digibytewallet-core/crypto/groestl.c
             src/main/jni/digibytewallet-core/crypto/groestl.h
             src/main/jni/digibytewallet-core/crypto/qubit.c
             src/main/jni/digibytewallet-core/crypto/qubit.h
             src/main/jni/digibytewallet-core/crypto/skein.c
             src/main/jni/digibytewallet-core/crypto/skein.h
             src/main/jni/digibytewallet-core/crypto/sha3/aes_helper.c
             src/main/jni/digibytewallet-core/crypto/sha3/blake.c
             src/main/jni/digibytewallet-core/crypto/sha3/bmw.c
             src/main/jni/digibytewallet-core/crypto/sha3/cubehash.c
             src/main/jni/digibytewallet-core/crypto/sha3/groestl.c
             src/main/jni/digibytewallet-core/crypto/sha3/hashblock.h
             src/main/jni/digibytewallet-core/crypto/sha3/keccak.c
             src/main/jni/digibytewallet-core/crypto/sha3/skein.c
             src/main/jni/digibytewallet-core/crypto/sha3/sph_types.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_blake.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_bmw.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_cubehash.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_groestl.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_keccak.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_skein.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_luffa.c
             src/main/jni/digibytewallet-core/crypto/sha3/sph_luffa.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_shavite.c
             src/main/jni/digibytewallet-core/crypto/sha3/sph_shavite.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_simd.c
             src/main/jni/digibytewallet-core/crypto/sha3/sph_simd.h
             src/main/jni/digibytewallet-core/crypto/sha3/sph_echo.c
             src/main/jni/digibytewallet-core/crypto/sha3/sph_echo.h

             #include "sha3/sph_luffa.h"
             #include "sha3/sph_shavite.h"
             #include "sha3/sph_simd.h"
             #include "sha3/sph_echo.h"

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


include_directories(src/main/jni/digibytewallet-core/)
include_directories(src/main/secp/secp256k1/include/)
include_directories(src/main/secp/secp256k1/src/)
include_directories(src/main/secp/secp256k1/)
include_directories(src/main/secp/)


# add lib dependencies
target_link_libraries(
    core-lib
    android
    log)
