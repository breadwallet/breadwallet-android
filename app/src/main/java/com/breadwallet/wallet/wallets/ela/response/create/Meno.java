package com.breadwallet.wallet.wallets.ela.response.create;

public class Meno {
    public String type;
    public String msg;

    public Meno(String type, String msg){
        this.type = type;
        this.msg = msg;
    }

    public String toString() {
        return "type:"+type + ",msg:"+msg;
    }
}
