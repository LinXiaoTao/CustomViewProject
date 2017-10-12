package com.leo.customviewproject.photoview;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.leo.customviewproject.R;

/**
 * Created on 2017/10/12 下午5:13.
 * leo linxiaotao1993@vip.qq.com
 */

public class PhotoViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PhotoView photoView = new PhotoView(this);
        photoView.setImageResource(R.drawable.wallpaper);
        photoView.setBackgroundResource(R.color.colorAccent);
        setContentView(photoView);
    }
}
