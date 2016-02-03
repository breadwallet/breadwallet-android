package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
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
import com.breadwallet.presenter.fragments.MainFragmentQR;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.QRCodeEncoder;
import com.breadwallet.tools.security.RequestHandler;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.security.InvalidAlgorithmParameterException;

public class RequestQRActivity extends Activity {

    public static final String TAG = RequestQRActivity.class.getName();
    private ImageView qrcode;
    public static String THE_ADDRESS = "";
    public static String tmpAmount = "0";
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_qr);



        //TODO get the actual address
        SharedPreferences prefs = getSharedPreferences(MainFragmentQR.RECEIVE_ADDRESS_PREFS, Context.MODE_PRIVATE);
        String requestAddrs = prefs.getString(MainFragmentQR.RECEIVE_ADDRESS, "");
        RequestQRActivity.THE_ADDRESS = "bitcoin:" + requestAddrs + "?amount=" + tmpAmount;
        qrcode = (ImageView) findViewById(R.id.request_image_qr_code);
        Button close = (Button) findViewById(R.id.request_close);
        TextView requestAmountText = (TextView) findViewById(R.id.request_amount_text);
        TextView requestAddressText = (TextView) findViewById(R.id.request_address_text);
        RelativeLayout addressLayout = (RelativeLayout) findViewById(R.id.request_address_layout);
//        Log.e(TAG,"THE_ADDRESS: " + THE_ADDRESS);

        generateQR(THE_ADDRESS);
        String address = "";
        String amount = "";
        try {
            RequestObject obj = RequestHandler.getRequestFromString(THE_ADDRESS);
            address = obj.address;
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            final String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
            final float rate = settings.getFloat(FragmentCurrency.RATE, 1);
            amount = CurrencyManager.getInstance(this).getBitsAndExchangeString(rate, iso, obj.amount);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
                onBackPressed();
                finish();
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

}
