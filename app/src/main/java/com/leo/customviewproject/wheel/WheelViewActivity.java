package com.leo.customviewproject.wheel;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.leo.customviewproject.BaseAcivity;
import com.leo.customviewproject.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created on 2017/10/31 下午8:36.
 * leo linxiaotao1993@vip.qq.com
 */

public class WheelViewActivity extends BaseAcivity {

    @BindView(R.id.wheel)
    WheelView mWheel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wheel);
        ButterKnife.bind(this);

        List<String> dataSources = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            dataSources.add("测试" + i);
        }
        mWheel.setDataSources(dataSources);

    }
}
