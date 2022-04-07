package org.limpingen.kpsi;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.android.vending.billing.IInAppBillingService;
import com.limpingen.donations.google.util.IabException;
import com.limpingen.donations.google.util.IabHelper;
import com.limpingen.donations.google.util.IabResult;
import com.limpingen.donations.google.util.Purchase;
import com.limpingen.donations.google.util.Inventory;
import com.limpingen.donations.google.util.Purchase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

public class Donasi extends AppCompatActivity {

    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAofS9wtpCH+20ZPuctPnJDtMT4vXi8pIIkc4qQuiUN19hlyL+RLjdN4mgPdxDZ7I4Tpy0FhWNQOlzP+rExCtJvqJN5GGmFVmNex9EOXh1Mz1ggSDJd5tyknFFP/OY3a6Lg+UspXjx77bIN1C2O+YQOPJEVoGOf1RMpa3Fnrn11M32alCSydS08YOvdyZspEWEaERky7+a5zbGoMOq4ow1A6eYtfGOtXsfUmGppOJAB1f9oUw5/coXFW745QQoWUjazew3ahVbQPCvOjli9QaCdkvkauR3Potg347fslYOvFONHLx3iLiJgK1lA6NvZAhcgXjKBevHT5LocpuOAu6bmQIDAQAB";
    private static final String[] GOOGLE_CATALOG = new String[]{"donasi.kpsi.10", "donasi.kpsi.25","donasi.kpsi.50","donasi.kpsi.100","donasi.kpsi.200","donasi.kpsi.500"};
    private static final String[] CATALOG_DEBUG = new String[]{"android.test.purchased",
            "android.test.canceled", "android.test.refunded", "android.test.item_unavailable"};
    private static final String TAG = "KPSI Donations";

    private Spinner mGoogleSpinner;

    private IabHelper mHelper;

    protected boolean mDebug = true;

    protected boolean mGoogleEnabled = false;
    protected String mGooglePubkey = "";
    protected String[] mGgoogleCatalog = new String[]{};
    protected String[] mGoogleCatalogValues = new String[]{};

    public IInAppBillingService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donasi);
        ActionBar actionBar = getActionBar();
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");


        ServiceConnection mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mService = IInAppBillingService.Stub.asInterface(service);
            }
        };
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);







        mGoogleEnabled = true;
        mGooglePubkey = GOOGLE_PUBKEY;
        mGgoogleCatalog = GOOGLE_CATALOG;
        mGoogleCatalogValues =  getResources().getStringArray(R.array.donation_google_catalog_values);
        mGoogleSpinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter;
        adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mGoogleCatalogValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGoogleSpinner.setAdapter(adapter);

        Button btGoogle = (Button) findViewById(R.id.buttonDonasi);
        btGoogle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v);
            }
        });
        if (mDebug)
            Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, mGooglePubkey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(mDebug);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        if (mDebug)
            Log.d(TAG, "Starting setup.");
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (mDebug)
                    Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    openDialog(android.R.drawable.ic_dialog_alert, R.string.donations__google_android_market_not_supported_title,
                            getString(R.string.donations__google_android_market_not_supported));
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;
                consumeAllPurchases();
            }
        });

    }



        public void consumeAllPurchases() {

            try
            {
                Bundle ownedItems =  mService.getPurchases(3, getPackageName(), "inapp", null);
                int response = ownedItems.getInt("RESPONSE_CODE");
                if (response == 0)
                {
                    ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                    //ArrayList<String> signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE");
                    //String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");
                    for (int i = 0; i < purchaseDataList.size(); ++i) {
                        try {
                            String purchaseData = purchaseDataList.get(i);
                            JSONObject jo = new JSONObject(purchaseData);
                            final String token = jo.getString("purchaseToken");
                            String sku = null;
                            if (ownedSkus != null)
                                sku = ownedSkus.get(i);

                            response = mService.consumePurchase(3, getPackageName(), token);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
            catch (RemoteException e)
            {


            }

        }
    public void donateGoogleOnClick(View view) {
        final int index;
        index = mGoogleSpinner.getSelectedItemPosition();
        if (mDebug)
            Log.d(TAG, "selected item in spinner: " + index);


            mHelper.launchPurchaseFlow(this, mGgoogleCatalog[index], com.limpingen.donations.google.util.IabHelper.ITEM_TYPE_INAPP, 0, mPurchaseFinishedListener, null);

    }
    void openDialog(int icon, int title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(icon);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(true);
        dialog.setNeutralButton(R.string.donations__button_close,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );
        dialog.show();
    }
    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (mDebug)
                Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isSuccess()) {
                if (mDebug)
                    Log.d(TAG, "Purchase successful.");

                // directly consume in-app purchase, so that people can donate multiple times
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);

                // show thanks openDialog
                openDialog(android.R.drawable.ic_dialog_info, R.string.donations__thanks_dialog_title,
                        getString(R.string.donations__thanks_dialog));
            }
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (mDebug)
                Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isSuccess()) {
                if (mDebug)
                    Log.d(TAG, "Consumption successful. Provisioning.");
            }
            if (mDebug)
                Log.d(TAG, "End consumption flow.");
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mDebug)
            Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the fragment result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {

            if (mDebug)
                Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
