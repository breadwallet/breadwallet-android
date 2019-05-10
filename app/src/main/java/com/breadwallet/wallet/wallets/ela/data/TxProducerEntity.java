package com.breadwallet.wallet.wallets.ela.data;

public class TxProducerEntity {
    public String Ownerpublickey;
    public String Nodepublickey;
    public String Nickname;

    public TxProducerEntity(String ownerpublickey, String nodepublickey, String nickname){
        this.Ownerpublickey = ownerpublickey;
        this.Nodepublickey = nodepublickey;
        this.Nickname = nickname;
    }
}
