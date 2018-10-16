package com.breadwallet.wallet.wallets.ela.response.history;

import java.util.List;

public class HistoryTx {
    public int vsize;
    public int locktime;
    public String txid;
    public int confirmations;
    public int type;
    public int version;
    public List<Vout> vout;
    public String blockhash;
    public int size;
    public int blocktime;
    public Payload payload;
    public List<Vin> vin;
    public int payloadversion;
    public List<Attributes> attributes;
    public long time;
    public List<Programs> programs;
    public String hash;
}
