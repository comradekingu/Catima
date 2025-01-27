package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Custom Scannner Activity extending from Activity to display a custom layout form scanner view.
 *
 * Based on https://github.com/journeyapps/zxing-android-embedded/blob/0fdfbce9fb3285e985bad9971c5f7c0a7a334e7b/sample/src/main/java/example/zxing/CustomScannerActivity.java
 * originally licensed under Apache 2.0
 */
public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "Catima";

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    private String cardId;

    private void extractIntentFields(Intent intent) {
        final Bundle b = intent.getExtras();
        cardId = b != null ? b.getString("cardId") : null;
        Log.d(TAG, "Scan activity: id=" + cardId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        extractIntentFields(getIntent());

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);

        // Even though we do the actual decoding with the barcodeScannerView
        // CaptureManager needs to be running to show the camera and scanning bar
        capture = new CaptureManager(this, barcodeScannerView);
        Intent captureIntent = new Intent();
        Bundle captureIntentBundle = new Bundle();
        captureIntentBundle.putBoolean(Intents.Scan.BEEP_ENABLED, false);
        captureIntent.putExtras(captureIntentBundle);
        capture.initializeFromIntent(captureIntent, savedInstanceState);

        barcodeScannerView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                Intent scanResult = new Intent();
                Bundle scanResultBundle = new Bundle();
                scanResultBundle.putString(BarcodeSelectorActivity.BARCODE_CONTENTS, result.getText());
                scanResultBundle.putString(BarcodeSelectorActivity.BARCODE_FORMAT, result.getBarcodeFormat().toString());
                scanResult.putExtras(scanResultBundle);
                ScanActivity.this.setResult(RESULT_OK, scanResult);
                finish();
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);

        BarcodeValues barcodeValues = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent, this);

        if (!barcodeValues.isEmpty()) {
            Intent manualResult = new Intent();
            Bundle manualResultBundle = new Bundle();
            manualResultBundle.putString(BarcodeSelectorActivity.BARCODE_CONTENTS, barcodeValues.content());
            manualResultBundle.putString(BarcodeSelectorActivity.BARCODE_FORMAT, barcodeValues.format());
            manualResult.putExtras(manualResultBundle);
            ScanActivity.this.setResult(RESULT_OK, manualResult);
            finish();
        }
    }

    public void addManually(View view) {
        Intent i = new Intent(getApplicationContext(), BarcodeSelectorActivity.class);
        if (cardId != null) {
            final Bundle b = new Bundle();
            b.putString("initialCardId", cardId);
            i.putExtras(b);
        }
        startActivityForResult(i, Utils.SELECT_BARCODE_REQUEST);
    }

    public void addFromImage(View view) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, Utils.BARCODE_IMPORT_FROM_IMAGE_FILE);
    }
}
