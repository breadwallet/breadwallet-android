package com.breadwallet.presenter.entities;

public class TokenItem{

    public String address;
    public String symbol;
    public String name;
    public String image; // may need to change to int if logos become resources
    public boolean isAdded;


    public TokenItem(String address, String symbol, String name, String image){
        this.address = address;
        this.symbol = symbol;
        this.name = name;
        this.image = image;
    }
}