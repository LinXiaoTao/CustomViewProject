package com.leo.customviewproject.tape;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.leo.customviewproject.R;

import java.util.Locale;

/**
 * Created on 2017/10/13 上午11:39.
 * leo linxiaotao1993@vip.qq.com
 */

public class SlideTapeActivity extends AppCompatActivity {

    private SlideTapeView mSlideTapeView;
    private TextView mTextView;

    private static final int INDICATOR_COLOR = Color.rgb(77, 166, 104);
    private static final String TAG = "SlideTapeActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_tap);

        mTextView = (TextView) findViewById(R.id.text);
        mTextView.setTextColor(INDICATOR_COLOR);


        mSlideTapeView = (SlideTapeView) findViewById(R.id.slideTape);
        mSlideTapeView.setValue(30, 50);
        mSlideTapeView.setLongUnix(1);
        mSlideTapeView.setShortPointCount(10);
        mSlideTapeView.setCallBack(new SlideTapeView.CallBack() {
            @Override
            public void onSlide(float current) {
                mTextView.setText(String.format(Locale.getDefault(),"%.1f",current));
            }
        });
    }
}
