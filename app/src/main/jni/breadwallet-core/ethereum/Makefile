
CORE_SRCS=../BRAddress.c \
	../BRBIP32Sequence.c \
	../BRBIP38Key.c \
	../BRBIP39Mnemonic.c \
	../BRBase58.c \
	../BRBech32.c \
	../BRBloomFilter.c \
	../BRCrypto.c \
	../BRKey.c \
	../BRMerkleBlock.c \
	../BRPaymentProtocol.c \
	../BRPeer.c \
	../BRPeerManager.c \
	../BRSet.c \
	../BRTransaction.c \
	../BRWallet.c \
	../bcash/BRBCashAddr.c

CORE_OBJS=$(CORE_SRCS:.c=.o)

ETH_SRCS=./util/BRUtilMath.c \
	./util/BRUtilMathParse.c \
	./util/BRUtilHex.c \
	./rlp/BRRlpCoder.c \
	./event/BREvent.c \
	./event/BREventQueue.c \
	./BREthereumAccount.c \
	./BREthereumAmount.c \
	./BREthereumBlock.c \
	./BREthereumContract.c \
	./BREthereumEther.c \
	./BREthereumGas.c \
	./BREthereumLightNode.c \
	./BREthereumLightNodeAnnounce.c \
	./BREthereumLightNodeListen.c \
	./BREthereumNetwork.c \
	./BREthereumToken.c \
	./BREthereumTransaction.c \
	./BREthereumWallet.c \
	./BREthereum.c \
	./BREthereumHash.c \
	./BREthereumLog.c \
	./BREthereumBloomFilter.c \
	./BREthereumAccountState.c \
	./BREthereumTransactionReceipt.c \
	./les/BREthereumFrameCoder.c \
	./les/BREthereumHandshake.c \
	./les/BREthereumLES.c \
	./les/BREthereumLESBase.c \
	./les/BREthereumNode.c \
	./les/BREthereumNodeEventHandler.c \
	./les/BREthereumNodeManager.c

ETH_OBJS=$(ETH_SRCS:.c=.o)

test:	build
	./test

build: clean
	cc -o test -Wno-format-extra-args -Wno-nullability-completeness -Wno-unknown-warning-option \
		-I/usr/include -I/usr/include/malloc -I/usr/include/machine -I../secp256k1 -I.. \
		-I. -I./util -I./rlp -I./event -I./les -I../secp256k1/include \
		-DDEBUG -DTEST_ETHEREUM_NEED_MAIN $(CORE_SRCS) $(ETH_SRCS) les/test-les.c test.c -lc

clean:
	rm -rf $(CORE_OBJS) $(ETH_OBJS)

