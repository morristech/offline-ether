package com.example.sundeep.offline_ether.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.sundeep.offline_ether.R;
import com.example.sundeep.offline_ether.api.ether.EtherApi;
import com.example.sundeep.offline_ether.entities.SentTransaction;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import mehdi.sakout.fancybuttons.FancyButton;

import static com.example.sundeep.offline_ether.Constants.SIGNED_TRANSACTION;

public class SendTransactionActivity extends AppCompatActivity {

    private final static String TAG = "SendTransactionActivty";
    private ProgressBar progressBar;
    private FancyButton okButton;
    private FancyButton sendButton;
    private TextView message;
    private EtherApi etherApi;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.send_transaction);

        // views
        progressBar = findViewById(R.id.send_transaction_progress);
        okButton = findViewById(R.id.close_button);
        sendButton = findViewById(R.id.send_transaction_button);
        message = findViewById(R.id.transaction_send_msg_textview);
        message.setVisibility(View.VISIBLE);
        message.setText("Send transaction?");
        okButton.setVisibility(View.GONE);

        String transaction = getIntent().getStringExtra(SIGNED_TRANSACTION);

        etherApi = EtherApi.getEtherApi(getResources().getString(R.string.etherScanHost));
        okButton.setOnClickListener(view -> SendTransactionActivity.this.finish());
        sendButton.setOnClickListener(view -> sendTransaction(transaction));

    }

    private void sendTransaction(String transaction) {
        etherApi.sendTransaction(transaction)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<SentTransaction>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        sendButton.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        message.setText("Broadcasting transaction...");
                    }

                    @Override
                    public void onNext(SentTransaction sentTransaction) {
                        message.setVisibility(View.VISIBLE);
                        if (sentTransaction.getError() != null) {
                            sendButton.setText("Retry");
                            sendButton.setVisibility(View.VISIBLE);
                            message.setText("Error occurred sending transaction. \n" + sentTransaction.getError().getMessage());
                        } else {
                            okButton.setVisibility(View.VISIBLE);
                            message.setText("Transaction successfully sent.");
                        }
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Error sending transactions", e);
                        progressBar.setVisibility(View.GONE);
                        sendButton.setText("Retry");
                        sendButton.setVisibility(View.VISIBLE);
                        message.setText("Error occurred sending transaction. \nCheck network settings." );
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "Completed transaction");
                    }
                });
    }


}
