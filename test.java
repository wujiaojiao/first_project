package com.hezhongguojin.app.activity.account;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import com.aspsine.irecyclerview.IRecyclerView;
import com.hezhongguojin.app.Config;
import com.hezhongguojin.app.R;
import com.hezhongguojin.app.activity.MainActivity;
import com.hezhongguojin.app.activity.PublicWebView.PublicWebViewActivity;
import com.hezhongguojin.app.activity.base.BackBaseActivity;
import com.hezhongguojin.app.adapter.MyCurrentAccountAdapter;
import com.hezhongguojin.app.beans.MyCurrentAccountBean;
import com.hezhongguojin.app.netutils.MyWealthApi;
import com.hezhongguojin.app.netutils.SuscriberX;
import com.hezhongguojin.app.netutils.requestparams.SortedParams;
import com.hezhongguojin.app.utils.FormatUtils;
import com.hezhongguojin.app.utils.ToastUtil;
import com.hezhongguojin.app.widgets.ActivityCollector;
import com.hezhongguojin.app.widgets.recycler.OnItemClickListener;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MyCurrentAccountActivity extends BackBaseActivity implements OnItemClickListener<MyCurrentAccountBean.Data.Info.PrdList>, View.OnClickListener {
    private MyCurrentAccountAdapter currentAccountAdapter;
    private IRecyclerView currentAccountRecycler;
    private TextView accumulatedIncomeSum, assetsHeldSum, yesterdayIncome;
    private double mDistanceY = 0;
    private double mHeight = 0;
    private final int TO_CURRENT_DETAILS = 0x0053;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_current_account);
        setTitle("我的活期理财");
        initData();
    }

    private void initData() {
        findViewById(R.id.roll_out).setOnClickListener(this);
        currentAccountAdapter = new MyCurrentAccountAdapter(this);
        currentAccountRecycler = (IRecyclerView) findViewById(R.id.current_account_recycler);
        currentAccountAdapter.setOnItemClickListener(this);
        currentAccountRecycler.setLayoutManager(new LinearLayoutManager(this));
        currentAccountRecycler.setIAdapter(currentAccountAdapter);
        currentAccountRecycler.setLoadMoreEnabled(false);
        View headerView = LayoutInflater.from(this).inflate(R.layout.current_account_header_item, null, false);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
        headerView.setLayoutParams(params);
        yesterdayIncome = (TextView) headerView.findViewById(R.id.yesterday_income);
        accumulatedIncomeSum = (TextView) headerView.findViewById(R.id.accumulated_income_sum);
        assetsHeldSum = (TextView) headerView.findViewById(R.id.assets_held_sum);
        View footerView = LayoutInflater.from(this).inflate(R.layout.current_account_bottom_item, null, false);
        AbsListView.LayoutParams paramsFooter = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
        footerView.setLayoutParams(paramsFooter);
        footerView.findViewById(R.id.transaction_record).setOnClickListener(this);
        footerView.findViewById(R.id.skip_invest).setOnClickListener(this);
        currentAccountRecycler.addHeaderView(headerView);
        currentAccountRecycler.addFooterView(footerView);
        currentAccountRecycler.setRefreshEnabled(false);
        initMeasure();
        currentAccountRecycler.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Log.d("dy:", dy + "");
                mDistanceY += dy;
                //toolbar的高度
                //当滑动的距离 <= toolbar高度的时候，改变Toolbar背景色的透明度，达到渐变的效果
                if (mDistanceY <= mHeight) {
                    double offset = mDistanceY / mHeight;
                    toolbar.setBackgroundColor(Color.argb((int) (offset * 255), 19, 186, 211));
                } else {
                    toolbar.setBackgroundColor(Color.argb(255, 19, 186, 211));
                }
            }
        });
    }

    private void initMeasure() {
        ViewTreeObserver vto = toolbar.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    toolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    toolbar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                mHeight = toolbar.getHeight();
            }
        });
    }

    @Override
    public void onItemClick(int position, MyCurrentAccountBean.Data.Info.PrdList prdList, View v) {
        Intent intent = new Intent(MyCurrentAccountActivity.this, MyCurrentDetailActivity.class);
        intent.putExtra("prdList", prdList);
        startActivityForResult(intent, TO_CURRENT_DETAILS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TO_CURRENT_DETAILS && resultCode != RESULT_OK) {
            currentAccountRecycler.smoothScrollToPosition(1);
        }
    }

    private void getCurrentAccountRequest() {
        mSubscription.add(MyWealthApi.getInstance().getMyWealthService()
                .getCurrentAccount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SuscriberX<MyCurrentAccountBean>(this) {
                    @Override
                    public void onNext(MyCurrentAccountBean myCurrentAccountBean) {
                        super.onNext(myCurrentAccountBean);
                        if (myCurrentAccountBean.getCode().equals("000000")) {
                            yesterdayIncome.setText(FormatUtils.getStandardMoney(myCurrentAccountBean.getData().getInfo().getYesterdayIncome()));
                            assetsHeldSum.setText(FormatUtils.getStandardMoney(myCurrentAccountBean.getData().getInfo().getAccountAmt() + myCurrentAccountBean.getData().getInfo().getInAmt()));
                            accumulatedIncomeSum.setText(FormatUtils.getStandardMoney(myCurrentAccountBean.getData().getInfo().getTotalIncome()));
                            currentAccountAdapter.refreshItems(myCurrentAccountBean.getData().getInfo().getPrdList());
                        } else {
                            ToastUtil.showToast(MyCurrentAccountActivity.this, myCurrentAccountBean.getDesc());
                        }
                        closeLoadingDialog();
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                        closeLoadingDialog();
                    }
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLoadingDialog();
        getCurrentAccountRequest();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.transaction_record:
                Intent intent = new Intent(MyCurrentAccountActivity.this, PublicWebViewActivity.class);
                SortedParams params = new SortedParams();
                params.put("productId", "all");
                params.put("type", "inAmt");
                intent.putExtra("URL", Config.transactionDetail + params.toString());
                intent.putExtra("refreshEnable","false");
                startActivity(intent);
                break;
            case R.id.skip_invest:
                ActivityCollector.removeAll(MainActivity.class);
                MainActivity mainActivity = (MainActivity) ActivityCollector.getActivity(MainActivity.class);
                mainActivity.type = 0;
                mainActivity.setTabSelection(1);
                break;
            case R.id.roll_out:
                Intent intentSec = new Intent(MyCurrentAccountActivity.this, PublicWebViewActivity.class);
                intentSec.putExtra("URL", Config.rollOutSchedule);
                startActivity(intentSec);
                break;
        }
    }
}
