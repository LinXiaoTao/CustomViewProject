package com.leo.customviewproject;

import android.app.Application;

import com.leo.customviewproject.utils.DisplayUtils;

/**
 * Created on 2017/11/1 下午8:07.
 * leo linxiaotao1993@vip.qq.com
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayUtils.init(this);
    }
}
