package com.breadwallet.tools.manager;

import androidx.annotation.StringDef;

import com.breadwallet.presenter.entities.Fee;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Litewallet
 * Created by Mohamed Barry on 3/10/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
public final class FeeManager {

    // this is the default that matches the mobile-api if the server is unavailable
    private static final long defaultEconomyFeePerKB = 2_500L; // From legacy minimum. default min is 1000 as Litecoin Core version v0.17.1
    private static final long defaultRegularFeePerKB = 2_5000L;
    private static final long defaultLuxuryFeePerKB = 66_746L;
    private static final long defaultTimestamp = 1583015199122L;

    private Fee defaultValues = new Fee(defaultLuxuryFeePerKB, defaultRegularFeePerKB, defaultEconomyFeePerKB, defaultTimestamp);

    private static final FeeManager instance;

    private String feeType;
    private Fee currentFees;

    public static FeeManager getInstance() {
        return instance;
    }

    static {
        instance = new FeeManager();
        instance.initWithDefaultValues();
    }

    private void initWithDefaultValues() {
        currentFees = defaultValues;
        feeType = REGULAR;
    }

    private FeeManager() {
    }

    public Fee getFees() {
        return currentFees;
    }

    public void setFeeType(@FeeType String feeType) {
        this.feeType = feeType;
    }

    public void resetFeeType() {
        this.feeType = REGULAR;
    }

    public boolean isRegularFee() {
        return feeType.equals(REGULAR);
    }

    public static final String LUXURY = "luxury";
    public static final String REGULAR = "regular";
    public static final String ECONOMY = "economy";

    public void setFees(long luxuryFee, long regularFee, long economyFee) {
        // TODO: to be implemented when feePerKB API will be ready
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({LUXURY, REGULAR, ECONOMY})
    public @interface FeeType {
    }
}
