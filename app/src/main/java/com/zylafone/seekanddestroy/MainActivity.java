package com.zylafone.seekanddestroy;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.zylafone.seekanddestroy.services.BluetoothBleClient;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    static final String DEFAULT_KEY_NAME = "default_key";
    private static final String SECRET_MESSAGE = "Very secret message";
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String TAG = MainActivity.class.getSimpleName();
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private Cipher defaultCipher;
    private Cipher cipherNotInvalidated;

    private ImageView mBluetoothConnectionIndicator;

    private final int REQUEST_ENABLE_BT = 40;
    private BluetoothBleClient mBluetoothClient;

    private Button mPreviousButton;

    //region Activity Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bootstrapBluetooth();
        bootstrapFingerprintScanning();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mBluetoothClient.dispose();
    }

    @Override
    protected void onResume(){
        mBluetoothClient.connect(mBluetoothConnectionIndicator);
        super.onResume();
    }
    //endregion

    //region Button Handlers
    public void directionalButtonClick(View v){
        switch(v.getId()){
            case  R.id.leftButton :
                mBluetoothClient.sendData("LEFT");
                break;
            case  R.id.rightButton :
                mBluetoothClient.sendData("RIGHT");
                break;
            case  R.id.forwardButton :
                mBluetoothClient.sendData("FORWARD");
                break;
            case  R.id.backwardButton :
                mBluetoothClient.sendData("BACK");
                break;
            default :
                //Should never reach this.
                //TODO: Log something?
                break;
        }

        manageDirectionalButtonColors(mPreviousButton, (Button)v);
    }
    public void manageDirectionalButtonColors(Button previousSelection, Button currentSelection) {
        if(previousSelection !=null)
        {
            previousSelection.setBackgroundResource(android.R.drawable.btn_default);
        }
        currentSelection.setBackgroundColor(Color.YELLOW);

        mPreviousButton = currentSelection;
    }

    public void fireButtonClick(View v){
        // Set up the crypto object for later. The object will be authenticated by use
        // of the fingerprint.
        if (initCipher(defaultCipher, DEFAULT_KEY_NAME)) {

            // Show the fingerprint dialog. The user has the option to use the fingerprint with
            // crypto, or you can fall back to using a server-side verified password.
            FingerprintAuthenticationDialogFragment fragment
                    = new FingerprintAuthenticationDialogFragment();
            fragment.setCryptoObject(new FingerprintManager.CryptoObject(defaultCipher));
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        }
    }
    //endregion

    //region Intent Handler

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mBluetoothClient = new BluetoothBleClient(this);
                mBluetoothClient.connect(mBluetoothConnectionIndicator);
            }
        }
    }
    //endregion

    /**
     * @param cryptoObject the Crypto object
     */
    public void onFireCommand(@Nullable FingerprintManager.CryptoObject cryptoObject) {

        // If the user has authenticated with fingerprint, verify that using cryptography and
        // then show the confirmation message.
        assert cryptoObject != null;
        tryEncrypt(cryptoObject.getCipher());



        mBluetoothClient.sendData("FIRE");
        ImageButton fireButton = (ImageButton)findViewById(R.id.imageButton);
        fireButton.setBackgroundColor(Color.GRAY);
        fireButton.setEnabled(false);
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via fingerprint.
     */
    private void tryEncrypt(Cipher cipher) {
        try {
            byte[] encrypted = cipher.doFinal(SECRET_MESSAGE.getBytes());

        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. "
                    + "Retry the purchase", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        }
    }

    public void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true) // Require the user to authenticate with a fingerprint to authorize every use of the key
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    private void bootstrapBluetooth(){
        mBluetoothConnectionIndicator = findViewById(R.id.bluetoothConnectionIndicator);
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else{
            mBluetoothClient = new BluetoothBleClient(this);
            mBluetoothClient.connect(mBluetoothConnectionIndicator);
        }
    }
    private void bootstrapFingerprintScanning(){


        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        try {
            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }

        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipherNotInvalidated = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }

        createKey(DEFAULT_KEY_NAME, true);

    }
}
