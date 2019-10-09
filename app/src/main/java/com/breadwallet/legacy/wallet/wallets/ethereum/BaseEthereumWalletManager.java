package com.breadwallet.legacy.wallet.wallets.ethereum;

import android.content.Context;

import com.breadwallet.core.ethereum.BREthereumEWM;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.legacy.presenter.entities.BRSettingsItem;
import com.breadwallet.legacy.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.legacy.wallet.abstracts.BaseWalletManager;
import com.breadwallet.legacy.wallet.abstracts.OnTxListModified;
import com.breadwallet.legacy.wallet.abstracts.SyncListener;
import com.breadwallet.legacy.wallet.wallets.WalletManagerHelper;
import com.breadwallet.tools.util.Utils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public abstract class BaseEthereumWalletManager implements BaseWalletManager {
    private static final String ETHEREUM_ADDRESS_PREFIX = "0x";
    static final int SCALE = 8;

    private WalletManagerHelper mWalletManagerHelper;
    protected String mAddress;

    public BaseEthereumWalletManager() {
        mWalletManagerHelper = new WalletManagerHelper();
    }

    protected WalletManagerHelper getWalletManagerHelper() {
        return mWalletManagerHelper;
    }

    //TODO Not used by ETH, ERC20
    @Override
    public int getForkId() {
        return -1;
    }

    @Override
    public synchronized String getAddress(Context context) { //todo context is not used, refactor
        if (mAddress == null) {
            throw new IllegalArgumentException("Address cannot be null.  Make sure it is set in the constructor.");
        }

        // TODO: Test of we can remove the caching in memory and always call core directly.
        return mAddress;
    }

    public abstract BREthereumWallet getWallet();

    @Override
    public boolean isAddressValid(String address) {
        return !Utils.isNullOrEmpty(address) && address.startsWith(ETHEREUM_ADDRESS_PREFIX) && BREthereumEWM.addressIsValid(address);
    }

    @Override
    public void addBalanceChangedListener(BalanceUpdateListener listener) {
        mWalletManagerHelper.addBalanceChangedListener(listener);
    }

    @Override
    public void removeBalanceChangedListener(BalanceUpdateListener listener) {
        mWalletManagerHelper.removeBalanceChangedListener(listener);
    }

    @Override
    public void onBalanceChanged(BigDecimal balance) {
        mWalletManagerHelper.onBalanceChanged(getCurrencyCode(), balance);
    }

    // TODO not used by ETH, ERC20
    @Override
    public void addSyncListener(SyncListener listener) {
    }

    // TODO not used by ETH, ERC20
    @Override
    public void removeSyncListener(SyncListener listener) {
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified listener) {
        mWalletManagerHelper.addTxListModifiedListener(listener);
    }

    @Override
    public void removeTxListModifiedListener(OnTxListModified listener) {
        mWalletManagerHelper.removeTxListModifiedListener(listener);
    }

    //TODO Not used by ETH, ERC20
    @Override
    public void refreshAddress(Context app) {
    }

    protected abstract WalletEthManager getEthereumWallet();

    @Override
    public boolean checkConfirmations(int conformations) {
        return mWalletManagerHelper.checkConfirmations(conformations);
    }

    public List<BRSettingsItem> getSettingsList(Context context) {
        return Collections.emptyList();
    }
}
