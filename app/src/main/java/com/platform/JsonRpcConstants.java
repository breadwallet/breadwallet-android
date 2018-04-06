package com.platform;

import com.breadwallet.BuildConfig;

/**
 * Created by byfieldj on 3/26/18.
 */

public class JsonRpcConstants {
    // Ethereum rpc endpoint
    public static final String BRD_ETH_RPC_ENDPOINT = BuildConfig.BITCOIN_TESTNET ? "/ethq/ropsten/proxy" : "/ethq/mainnet/proxy";
    public static final String BRD_ETH_TX_ENDPOINT = BuildConfig.BITCOIN_TESTNET ? "/ethq/ropsten/" : "/ethq/mainnet/";
//    //public static final String ETH_RPC_TX_LIST = BuildConfig.BITCOIN_TESTNET ?
//            "https://ropsten.etherscan.io/api?module=account&action=txlist&address=%s" :
//            "https://api.etherscan.io/api?module=account&action=txlist&address=%s";

}
