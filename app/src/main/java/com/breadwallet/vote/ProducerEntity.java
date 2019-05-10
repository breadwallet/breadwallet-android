package com.breadwallet.vote;

public class ProducerEntity {
    public String Producer_public_key;
    public String Value;
    public int Rank;
    public String Address;
    public String Nickname;
    public String Votes;

    public ProducerEntity(String publickey, String value, int rank, String address, String nickname, String votes){
        this.Producer_public_key = publickey;
        this.Value = value;
        this.Rank = rank;
        this.Address = address;
        this.Nickname = nickname;
        this.Votes = votes;
    }
}
