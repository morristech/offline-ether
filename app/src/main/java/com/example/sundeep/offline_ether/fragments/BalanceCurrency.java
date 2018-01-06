package com.example.sundeep.offline_ether.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.sundeep.offline_ether.App;
import com.example.sundeep.offline_ether.R;
import com.example.sundeep.offline_ether.api.RestClient;
import com.example.sundeep.offline_ether.api.ether.EtherApi;
import com.example.sundeep.offline_ether.entities.EtherAddress;
import com.example.sundeep.offline_ether.utils.EtherMath;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

public class BalanceCurrency extends Fragment {

    private static final String TAG = "BalanceCurrency";
    private DataSubscription observer;
    private TextView balance;
    private EtherApi etherApi;
    private Box<EtherAddress> boxStore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.balance, container, false);
        balance = rootView.findViewById(R.id.balance_textView);
        balance.setText("-");

        String etherScanHost = getResources().getString(R.string.etherScanHost);
        etherApi = new EtherApi(new RestClient(new OkHttpClient()), etherScanHost);


        boxStore = ((App) getActivity().getApplication()).getBoxStore().boxFor(EtherAddress.class);
        Query<EtherAddress> addressQuery = boxStore.query().build();
        observer = addressQuery
                .subscribe()
                .observer(onAddress());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        observer.cancel();
    }

    @NonNull
    private DataObserver<List<EtherAddress>> onAddress() {
        return newAddresses -> {
            BigDecimal balanceEther = EtherMath.sumAddresses(newAddresses);
            updateBalanceInCurrency(balanceEther);
        };
    }

    private void updateBalanceInCurrency(BigDecimal ether) {
        etherApi.getPrices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(convertEtherPriceToFiat(ether), e -> Log.e(TAG, "Error fetching prices", e));
    }

    @NonNull
    private Consumer<Map<String, String>> convertEtherPriceToFiat(BigDecimal ether) {
        return new Consumer<Map<String, String>>() {
            @Override
            public void accept(Map<String, String> currToPrice) throws Exception {
                Currency currency = Currency.getInstance(Locale.getDefault());
                String currencyCode = currency.getCurrencyCode();

                if (!currToPrice.containsKey(currencyCode)){
                    currency = Currency.getInstance(Locale.US);
                    currencyCode = currency.getCurrencyCode();
                }

                if (currToPrice.containsKey(currencyCode)) {
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    balance.setText(currencyFormatter.format(ether.multiply(new BigDecimal(currToPrice.get(currencyCode)))));
                }
            }
        };
    }


}
