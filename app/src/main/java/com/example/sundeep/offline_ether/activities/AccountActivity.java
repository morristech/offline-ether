package com.example.sundeep.offline_ether.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.sundeep.offline_ether.App;
import com.example.sundeep.offline_ether.R;
import com.example.sundeep.offline_ether.adapters.TransactionsAdapter;
import com.example.sundeep.offline_ether.api.ether.EtherApi;
import com.example.sundeep.offline_ether.entities.EtherAddress;
import com.example.sundeep.offline_ether.entities.EtherTransaction;
import com.example.sundeep.offline_ether.mvc.presenters.AccountPresenter;
import com.example.sundeep.offline_ether.mvc.views.AccountView;
import com.example.sundeep.offline_ether.utils.EtherMath;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;

import static com.example.sundeep.offline_ether.Constants.PUBLIC_ADDRESS;

public class AccountActivity extends AppCompatActivity implements AccountView {

    private final static String TAG = "AccountActivity";

    private TransactionsAdapter transactionAdapter;
    private String address;
    private List<EtherTransaction> etherTransactionsList = new ArrayList<>();
    private AccountPresenter accountPresenter;

    // views
    private RecyclerView addressRecyclerView;
    private TextView balanceTextView;
    private ImageView addressPhoto;
    private TextView addressTextView;
    private FloatingActionButton fab;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.account);

        // views
        initViews();

        transactionAdapter = getTransactionAdapter();
        accountPresenter = getAccountPresenter();

        initTransactionRecyclerView();

        fab.setOnClickListener(startOfflineTransaction(address));

        accountPresenter.loadEtherAddress();
    }

    private void initViews() {
        address = getIntent().getStringExtra(PUBLIC_ADDRESS);
        addressRecyclerView = findViewById(R.id.transactions_recycler_view);
        balanceTextView = findViewById(R.id.balance);
        addressPhoto = findViewById(R.id.address_photo);
        addressTextView = findViewById(R.id.address_textview);
        fab = findViewById(R.id.new_offline_transaction);
    }

    private void initTransactionRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(addressRecyclerView.getContext(),
                layoutManager.getOrientation());

        addressRecyclerView.addItemDecoration(dividerItemDecoration);
        addressRecyclerView.setLayoutManager(layoutManager);
        addressRecyclerView.setAdapter(transactionAdapter);
    }

    private TransactionsAdapter getTransactionAdapter() {
        int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        return new TransactionsAdapter(etherTransactionsList, address, drawable(R.drawable.orange_rounded_corner),
                drawable(R.drawable.green_rounded_corner), drawable(R.drawable.dark_rounded_corner), paddingPx);
    }

    private AccountPresenter getAccountPresenter() {
        EtherApi etherApi = EtherApi.getEtherApi(getResources().getString(R.string.etherScanHost));
        Box<EtherAddress> addressBoxStore = ((App) getApplication()).getBoxStore().boxFor(EtherAddress.class);
        return new AccountPresenter(this, addressBoxStore, address, etherApi);
    }

    private Drawable drawable(int drawable) {
        return getResources().getDrawable(drawable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resume called");
        accountPresenter.loadLast50Transactions();
    }

    @NonNull
    private View.OnClickListener startOfflineTransaction(String address) {
        return view -> {
            Intent intent = new Intent(AccountActivity.this, OfflineTransactionActivity.class);
            intent.putExtra(PUBLIC_ADDRESS, address);
            startActivity(intent);
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        accountPresenter.destroy();
    }

    @Override
    public void onTransactions(List<EtherTransaction> transactions) {
        etherTransactionsList.clear();
        etherTransactionsList.addAll(transactions);
        transactionAdapter.notifyDataSetChanged();
    }

    @Override
    public void addressLoadError(Throwable e) {
        View viewById = findViewById(R.id.main_container);
        Snackbar.make(viewById, "Unable to fetch transactions. Check Network.", Snackbar.LENGTH_LONG).show();
        Log.e(TAG, "Error fetching transactions", e);
    }

    @Override
    public void onAddressLoad(EtherAddress address) {
        byte[] blockie = address.getBlockie();
        Bitmap bitmap = BitmapFactory.decodeByteArray(blockie, 0, blockie.length);
        addressPhoto.setImageBitmap(bitmap);

        addressTextView.setText(address.getAddress());
        balanceTextView.setText(EtherMath.weiAsEtherStr(address.getBalance()));
        onTransactions(address.getEtherTransactions());
    }
}
