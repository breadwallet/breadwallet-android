package com.breadwallet.wallet.wallets.ela.response.history;

import java.util.List;

public class Txs {
    public double valueOut;
    public boolean isCoinBase;
    public List<Vout> vout;
    public String blockhash;
    public long time;
    public List<Vin> vin;
    public String txid;
    public long blocktime;
    public int version;
    public int confirmations;
    public double fees;
    public int blockheight;
    public int locktime;
    public String _id;
    public int size;
}
