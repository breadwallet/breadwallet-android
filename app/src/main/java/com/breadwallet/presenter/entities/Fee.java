package com.breadwallet.presenter.entities;

/**
 * Litewallet
 * Created by Mohamed Barry on 3/10/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
public class Fee {
    public final long luxury;
    public final long regular;
    public final long economy;
    public final long timestamp;

    public Fee(long luxury, long regular, long economy, long timestamp) {
        this.luxury = luxury;
        this.regular = regular;
        this.economy = economy;
        this.timestamp = timestamp;
    }
}
