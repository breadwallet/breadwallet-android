package com.breadwallet.presenter.activities.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.wallets.ela.ElaDataSource;

import java.math.BigDecimal;

public class ElaNodeActivity extends BRActivity {

    private static String TAG = ElaNodeActivity.class.getSimpleName();

    BaseTextView mCurrentNode;
    BaseTextView mConnectStatus;
    BRButton mSwitchBtn;
    BRButton mSelectBtn;
    View mListBgView;
    ListView mNodeLv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ela_node);
        mCurrentNode = findViewById(R.id.node_text);
        mConnectStatus = findViewById(R.id.node_status);
        mSwitchBtn = findViewById(R.id.button_switch);
        mSelectBtn = findViewById(R.id.node_list_btn);
        mNodeLv = findViewById(R.id.node_listview);
        mListBgView = findViewById(R.id.list_bg);
        mCurrentNode.setText(BRSharedPrefs.getElaNode(this, ElaDataSource.ELA_NODE_KEY));
        mConnectStatus.setText(getString(R.string.NodeSelector_connected));

        mSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog();
            }
        });
        mSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showList();
            }
        });
        final String[] nodes = {"api-wallet-ela.elastos.org", "api-wallet-ela-testnet.elastos.org", "default node"};
        ArrayAdapter adapter = new ArrayAdapter(this, R.layout.node_item_layout, nodes);
        mNodeLv.setAdapter(adapter);
        mNodeLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(i < nodes.length-1){
                    Log.i(TAG, "item:"+i);
                    String oldNode = BRSharedPrefs.getElaNode(ElaNodeActivity.this, ElaDataSource.ELA_NODE_KEY);
                    String input = nodes[i];
                    if(!StringUtil.isNullOrEmpty(input) && !input.equals(oldNode)) {
                        BRSharedPrefs.putElaNode(ElaNodeActivity.this, ElaDataSource.ELA_NODE_KEY, input.trim());
                        mCurrentNode.setText(input);
                        wipeData();
                    }
                }
                hideList();
            }
        });
        findViewById(R.id.back_arrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void hideList(){
        mNodeLv.setVisibility(View.GONE);
        mListBgView.setVisibility(View.GONE);
        mSwitchBtn.setClickable(true);
        mSelectBtn.setClickable(true);
        mSwitchBtn.setEnabled(true);
        mSelectBtn.setEnabled(true);
    }

    private void showList(){
        mNodeLv.setVisibility(View.VISIBLE);
        mListBgView.setVisibility(View.VISIBLE);
        mSwitchBtn.setClickable(false);
        mSelectBtn.setClickable(false);
        mSwitchBtn.setEnabled(false);
        mSelectBtn.setEnabled(false);
    }

    private void createDialog() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final TextView customTitle = new TextView(this);

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad32 = Utils.getPixelsFromDps(this, 32);
        int pad16 = Utils.getPixelsFromDps(this, 16);
        customTitle.setPadding(pad16, pad16, pad16, pad16);
        customTitle.setText(getString(R.string.NodeSelector_enterTitle));
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);
        alertDialog.setMessage(getString(R.string.NodeSelector_enterBody));

        final EditText inputEdit = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(this, 24);

        inputEdit.setPadding(pix, 0, pix, pix);
        inputEdit.setLayoutParams(lp);
        alertDialog.setView(inputEdit);

        alertDialog.setNegativeButton(getString(R.string.Button_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.setPositiveButton(getString(R.string.Button_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String oldNode = BRSharedPrefs.getElaNode(ElaNodeActivity.this, ElaDataSource.ELA_NODE_KEY);
                        String input = inputEdit.getText().toString();
                        if(!StringUtil.isNullOrEmpty(input) && !input.equals(oldNode)) {
                            BRSharedPrefs.putElaNode(ElaNodeActivity.this, ElaDataSource.ELA_NODE_KEY, input.trim());
                            mCurrentNode.setText(input);
                            wipeData();
                        }
                    }
                });
        alertDialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                inputEdit.requestFocus();
                final InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(inputEdit, 0);
            }
        }, 200);
    }

    private void wipeData(){
        BRSharedPrefs.putCachedBalance(this, "ELA",  new BigDecimal(0));
        ElaDataSource.getInstance(this).deleteAllTransactions();
    }
}
