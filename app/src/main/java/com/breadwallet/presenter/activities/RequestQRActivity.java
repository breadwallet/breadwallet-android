package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.QRCodeEncoder;
import com.breadwallet.tools.security.RequestHandler;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;

public class RequestQRActivity extends Activity {

    public static final String TAG = RequestQRActivity.class.getName();
    private ImageView qrcode;
    public boolean activityIsInBackground = false;
    public static RequestQRActivity requestApp;
    public Button close;

    //    public static String THE_ADDRESS = "";
//    public static String tmpAmount = "0";
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_qr);
        requestApp = this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        String requestAddrs = getIntent().getExtras().getString(BRConstants.INTENT_EXTRA_REQUEST_ADDRESS);
        String requestAmount = getIntent().getExtras().getString(BRConstants.INTENT_EXTRA_REQUEST_AMOUNT);

        Log.e(TAG, "requestAddrs: " + "|" + requestAddrs + "|");
        Log.e(TAG, "requestAmount: " + "|" + requestAmount + "|");

        String finalAddress = "bitcoin:" + requestAddrs + "?amount=" + requestAmount;
        qrcode = (ImageView) findViewById(R.id.request_image_qr_code);
        close = (Button) findViewById(R.id.request_close);
        TextView requestAmountText = (TextView) findViewById(R.id.request_amount_text);
        TextView requestAddressText = (TextView) findViewById(R.id.request_address_text);
        RelativeLayout addressLayout = (RelativeLayout) findViewById(R.id.request_address_layout);
//        Log.e(TAG,"THE_ADDRESS: " + THE_ADDRESS);

        generateQR(finalAddress);
        String address = "";
        String amount = "";
        try {
            RequestObject obj = RequestHandler.getRequestFromString(finalAddress);
            address = obj.address;
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            final String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
            final float rate = settings.getFloat(FragmentCurrency.RATE, 1);
            amount = CurrencyManager.getInstance(this).getBitsAndExchangeString(rate, iso,
                    String.valueOf(new BigDecimal(obj.amount).multiply(new BigDecimal("1000000"))));
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        final Intent intent = new Intent(this, MainActivity.class);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
                onBackPressed();

                startActivity(intent);
                if (!RequestQRActivity.this.isDestroyed()) {
                    finish();
                }

            }
        });
        requestAddressText.setText(address);
        requestAmountText.setText(amount);
        final BreadWalletApp breadWalletApp = (BreadWalletApp) getApplication();
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                breadWalletApp.cancelToast();
//                if (FragmentAnimator.checkTheMultipressingAvailability()) {
//                    sharingFragment.setTheAddress(mainAddressText.getText().toString());
//                    sharingFragment.show(fm, SharingFragment.class.getName());
//                }
            }
        });

    }

    private void generateQR(String bitcoinURL) {
        if (qrcode == null) return;
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 3 / 4;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(bitcoinURL,
                BarcodeFormat.QR_CODE.toString(),
                smallerDimension);
        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            qrcode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityIsInBackground = false;
        requestApp = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityIsInBackground = true;
    }
}
