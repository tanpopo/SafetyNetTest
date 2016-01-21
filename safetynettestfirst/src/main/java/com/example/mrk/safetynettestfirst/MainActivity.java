package com.example.mrk.safetynettestfirst;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.common.api.ResultCallback;

import junit.framework.Assert;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static String tag = "MainActivity";

    private GoogleApiClient mGoogleApiClient = null;
    private SecureRandom mSecureRandom = null;

    private long startTime = 0, endTime = 0;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(tag, "onCreate");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //attest
        Button attestButton = (Button) findViewById(R.id.attestButton);
        attestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                attest();
            }
        });

        ////////////////////////////////
        // initialize variables

        try {
            mSecureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            Log.e(tag, "securerandom initialize failed!");
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        buildGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int attest() {
        TextView outputView = (TextView) findViewById(R.id.outputView);
        outputView.setText("");


        String logText = "attest start\n";
        Log.d(tag, "attest -in");
        startTime = java.lang.System.currentTimeMillis();

        byte[] nonce = getRequestNonce(); // Should be at least 16 bytes in length.
//        Assert.assertNotNull(nonce);
//        Assert.assertNotNull(mGoogleApiClient);

        if (nonce == null) {
            logText += "Error: nonce is null!!" + "\n";
            return 1;
        }
        if (mGoogleApiClient == null) {
            logText += "Error: client is null!!" + "\n";
            return 1;
        }
        logText += "attest ready: nonce=" + Integer.toString(byteToHex(nonce), 16) + "\n";

        PendingResult pendingResult = SafetyNet.SafetyNetApi.attest(mGoogleApiClient, nonce);
        if (pendingResult.isCanceled()) {
            logText += "pendingResult is cancelled\n";
        } else {
            logText += "pendingResult waiting\n";
        }
        pendingResult.setResultCallback(new ResultCallback<SafetyNetApi.AttestationResult>() {
                    @Override
                    public void onResult(@NonNull SafetyNetApi.AttestationResult result) {
                        String logText = "onResult called\n";
                        Status status = result.getStatus();
                        if (status.isSuccess()) {
                            Log.d(tag, "attest success!");
                            logText += "attest success!\n";
                            final String jwsResult = result.getJwsResult();
                            if (!TextUtils.isEmpty(jwsResult)) {
                                Log.d(tag, "result(raw)\n" + jwsResult);

                                String decodedPayloadString = parseJsonWebSignaturePayload(jwsResult);
                                if (decodedPayloadString != null) {
                                    logText += "decodedPayloadString=" + decodedPayloadString + "\n";;
                                    Log.d(tag, "result(JWS decoded)\n" + decodedPayloadString);
                                } else {
                                    logText += "Error: JWS format is incorrect" + "\n";;
                                }
                            } else {
                                logText += "Error: JWS is empty" + "\n";
                            }
                        } else {
                            Log.e(tag, "attest failed!");
                            logText += "Error: attest failed: errorcode = " + status.getStatusCode() + ":" + CommonStatusCodes.getStatusCodeString(status.getStatusCode())+ "\n";
                        }
                        endTime = java.lang.System.currentTimeMillis();
                        logText += "onResult elapsed time = " + Long.toString((endTime - startTime)/1000) + "\n";

                        TextView outputView = (TextView) findViewById(R.id.outputView);
                        outputView.setText(outputView.getText() + logText);

                    }

        }, 180, TimeUnit.SECONDS);
//                });
        logText += "attest completed\n";


        outputView = (TextView) findViewById(R.id.outputView);
        outputView.setText(outputView.getText() + logText);

        Log.d(tag, "attest -out");

        return 0;
    }


    ///////////////////////////////////////
    // override
    //interface GoogleApiClient.ConnectionCallbacks

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(tag, "GoogleApiClient.ConnectionCallbacks onConnected!");

        Toast.makeText(this, "connected to SafetyNet", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(tag, "GoogleApiClient.ConnectionCallbacks onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.d(tag, "GoogleApiClient.OnConnectionFailedListener onConnectionFailed result=" + Integer.toString(result.getErrorCode()) + ":" + result.getErrorMessage());
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.mrk.safetynettestfirst/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);

        mGoogleApiClient.connect();
        Log.d(tag, "GoogleApiClient connect requested");
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.mrk.safetynettestfirst/http/host/path")
        );

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();

    }

    ///////////////////////////
    // utils
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(SafetyNet.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private byte[] getRequestNonce() {
        byte[] nonce = null;

        if (mSecureRandom != null) {
            nonce = new byte[32];
            mSecureRandom.nextBytes(nonce);
        }
        return nonce;
    }

    static private String byteToString(byte[] bytes, String separator) {
        String str = "";
        for (int i=0; i<bytes.length; i++) {
            if (i != 0) {
                str += separator;
            }
            str += Byte.toString(bytes[i]);
        }
        return str;
    }

    static private Integer byteToHex(byte[] bytes) {
        Integer val = 0;
        for (int i=0; i<bytes.length; i++) {
            val = (val << 8) + (bytes[i] & 0xff);
        }
        return val;
    }

    private @Nullable
//        SafetyNetResponse parseJsonWebSignature(@NonNull String jwsResult) {
        String parseJsonWebSignaturePayload(@NonNull String jwsResult) {
            //the JWT (JSON WEB TOKEN) is just a 3 base64 encoded parts concatenated by a . character
        final String[] jwtParts = jwsResult.split("\\.");

        if(jwtParts.length==3) {
            //we're only really interested in the body/payload
            String decodedPayload = new String(Base64.decode(jwtParts[1], Base64.DEFAULT));

            Log.d(tag, "raw(result[0])=" + jwtParts[0]);
            Log.d(tag, "raw(result[1])=" + jwtParts[1]);
            Log.d(tag, "raw(result[2])=" + jwtParts[2]);

            Log.d(tag, "decode(result[0])=" + new String(Base64.decode(jwtParts[0], Base64.DEFAULT)) + "\n");
            Log.d(tag, "decode(result[1])=" + new String(Base64.decode(jwtParts[1], Base64.DEFAULT)) + "\n");
//            Log.d(tag, "decode(result[2])=" + new String(Base64.decode(jwtParts[2], Base64.DEFAULT)) + "\n");

//            return SafetyNetResponse.parse(decodedPayload);
            return decodedPayload;
        }else{
            Log.e(tag, "jwsResult is incorrect num of jwtParts=" + jwtParts.length);
            return null;
        }
    }
}
