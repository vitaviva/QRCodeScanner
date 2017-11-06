
package com.vitaviva.qrcodescanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;

public class TextScanResultActivity extends Activity implements OnClickListener {

    public final static String KEY_TEXT = "TEXT_RESUTL";

    private EditText mEtText;
    private Button mBtnCopy;
    private Button mBtnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result_text);
        TextView articleTx = (TextView) findViewById(R.id.title);
        articleTx.setText(R.string.text_message);
        TextView backBtn = (TextView) findViewById(R.id.back);
        backBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mEtText = (EditText) findViewById(R.id.et_text);
        mBtnCopy = (Button) findViewById(R.id.btn_copy);
        mBtnSearch = (Button) findViewById(R.id.btn_search);
        mBtnCopy.setOnClickListener(this);
        mBtnSearch.setOnClickListener(this);

        Intent intent = getIntent();
        if (intent != null) {
            String text = intent.getStringExtra(KEY_TEXT);
            mEtText.setText(text);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_copy) {
            ClipboardManager clipboarManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboarManager.setText(mEtText.getText().toString());
            Toast.makeText(this, R.string.added_to_clip_board, Toast.LENGTH_LONG).show();
        } else if (id == R.id.btn_search) {
            String url = String.format(
                    "http://m.baidu.com/s?word=%s",
                    URLEncoder.encode(mEtText.getText().toString()));

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            setResult(RESULT_OK);
            finish();
        }
    }
}
