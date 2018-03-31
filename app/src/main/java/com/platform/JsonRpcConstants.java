package com.platform;

/**
 * Created by byfieldj on 3/26/18.
 */

public class JsonRpcConstants {


    // Ethereum rpc endpoint
    public static final String BRD_ETH_RPC_ENDPOINT = "/ethq/ropsten/proxy";
    public static final String ETH_RPC_TX_LIST = "https://ropsten.etherscan.io/api?module=account&action=txlist&address=%s";
    public static final String ETH_ENDPOINT_ESTIMATE_GAS = "eth_estimateGas";
    public static final String ETH_ENDPOINT_GAS_PRICE = "eth_gasPrice";
    public static final String ETH_ENDPOINT_GET_BALANCE = "eth_getBalance";


}
