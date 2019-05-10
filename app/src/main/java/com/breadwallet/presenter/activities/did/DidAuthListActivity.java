package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.did.AuthorInfo;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.tools.adapter.AuthorAdapter;
import com.breadwallet.tools.util.PinyinUtil;
import com.breadwallet.tools.util.StringUtil;

import org.wallet.library.utils.StringUtils;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DidAuthListActivity extends BaseSettingsActivity {

    private ListView mAuthorList;


    @Override
    public int getLayoutId() {
        return R.layout.activity_did_auth_list_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initListener();
    }

    List<AuthorInfo> infos;
    private void initView(){
        infos = DidDataSource.getInstance(this).getAllInfos();
        mAuthorList = findViewById(R.id.author_app_list);
        AppNameComparator comparator = new AppNameComparator();
        Collections.sort(infos, comparator);
        mAuthorList.setAdapter(new AuthorAdapter(this, infos));
    }

    private void initListener(){
        mAuthorList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(DidAuthListActivity.this, DidDetailActivity.class);
                intent.putExtra("did", infos.get(i).getDid());
                intent.putExtra("appId", infos.get(i).getAppId());
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
    }

    static class AppNameComparator implements Comparator<AuthorInfo> {

        @Override
        public int compare(AuthorInfo o1, AuthorInfo o2) {

            Collator myCollator = Collator.getInstance(Locale.CHINESE);

            if(o1==null || o2==null) return 0;
            if(StringUtils.isNullOrEmpty(o1.getAppName()) || StringUtils.isNullOrEmpty(o2.getAppName())) return 0;

            String appName1 = PinyinUtil.getPingYin(o1.getAppName());
            String appName2 = PinyinUtil.getPingYin(o2.getAppName());
            if(StringUtil.isNullOrEmpty(appName1) || StringUtil.isNullOrEmpty(appName2)) return 0;
            if (myCollator.compare(appName1, appName2) < 0)
                return -1;
            else if (myCollator.compare(appName1, appName2) > 0)
                return 1;
            else
                return 0;
        }
    }
}
