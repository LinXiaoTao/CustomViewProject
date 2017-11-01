package com.leo.customviewproject;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

/**
 * Created on 2017/10/20 下午3:06.
 * leo linxiaotao1993@vip.qq.com
 */

public abstract class BaseAcivity extends AppCompatActivity {

    protected Toolbar mToolbar;

    @Override
    protected void onResume() {
        super.onResume();
        if (mToolbar == null) {
            mToolbar = findViewById(R.id.toolbar);
            setSupportActionBar(mToolbar);
            if (needBackIcon()) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);

    }

    protected boolean needBackIcon() {
        return true;
    }
}
