package com.leo.customviewproject.photoview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * 参考地址 https://github.com/chrisbanes/PhotoView
 * Created on 2017/9/29 下午3:55.
 * leo linxiaotao1993@vip.qq.com
 */

public class PhotoView extends AppCompatImageView {

    private final PhotoViewAttacher mPhotoViewAttacher;
    private ScaleType mTargetScaleType;

    public PhotoView(Context context) {
        this(context, null);
    }

    public PhotoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPhotoViewAttacher = new PhotoViewAttacher(this);

        super.setScaleType(ScaleType.MATRIX);
        if (mTargetScaleType != null) {
            setScaleType(mTargetScaleType);
            mTargetScaleType = null;
        }
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (mPhotoViewAttacher == null) {
            mTargetScaleType = scaleType;
        } else {
            mPhotoViewAttacher.setScaleType(scaleType);
        }
    }

    @Override
    public ScaleType getScaleType() {
        return mPhotoViewAttacher.getScaleType();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        dispatchAttacherUpdate();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);
        dispatchAttacherUpdate();
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        super.setImageURI(uri);
        dispatchAttacherUpdate();
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        if (changed) {
            dispatchAttacherUpdate();
        }
        return changed;
    }

    private void dispatchAttacherUpdate() {
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.update();
        }
    }
}
