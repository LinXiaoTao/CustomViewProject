package com.leo.customviewproject.photoview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.OverScroller;

/**
 * Created on 2017/9/29 下午3:58.
 * leo linxiaotao1993@vip.qq.com
 */

final class PhotoViewAttacher implements View.OnTouchListener, View.OnLayoutChangeListener {

    private final ImageView mImageView;
    private ScaleType mScaleType = ScaleType.FIT_CENTER;

    private CustomGestureDetector mScaleDragDetector;

    private GestureDetector mGestureDetector;

    private boolean mZoomEnabled = true;

    private final float[] mMatrixValues = new float[9];
    private final RectF mDisplayRect = new RectF();
    private final Matrix mBaseMatrix = new Matrix();
    /** 实际绘制矩阵 */
    private final Matrix mDrawMatrix = new Matrix();
    /** 用于计算矩阵 */
    private final Matrix mSuppMatrix = new Matrix();

    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private float mMinScale = DEFAULT_MIN_SCALE;
    private float mMidScale = DEFAULT_MID_SCALE;
    private float mMaxScale = DEFAULT_MAX_SCALE;
    private int mZoomDuration = DEFAULT_ZOOM_DURATION;

    private boolean mBlockParentIntercept = false;
    private boolean mAllowParentInterceptOnEdge = true;
    private int mScrollEdge = EDGE_BOTH;

    private float mBaseRotation;

    private float mOverScaleCoefficient = 0.5f;

    private FlingRunnable mCurrentFlingRunnable;

    private final static float DEFAULT_MAX_SCALE = 3.0f;
    private final static float DEFAULT_MID_SCALE = 1.75f;
    private final static float DEFAULT_MIN_SCALE = 1.0f;
    private final static int DEFAULT_ZOOM_DURATION = 200;

    private static final int EDGE_NONE = -1;
    private static final int EDGE_LEFT = 0;
    private static final int EDGE_RIGHT = 1;
    private static final int EDGE_BOTH = 2;

    PhotoViewAttacher(ImageView imageView) {
        mImageView = imageView;
        imageView.setOnTouchListener(this);
        imageView.addOnLayoutChangeListener(this);

        if (imageView.isInEditMode()) {
            return;
        }

        mScaleDragDetector = new CustomGestureDetector(imageView.getContext(), mOnCustomGestureListener);

        mGestureDetector = new GestureDetector(imageView.getContext(), mOnGestureListener);
        mGestureDetector.setOnDoubleTapListener(mOnGestureListener);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean handled = false;

        if (mZoomEnabled && mImageView.getDrawable() != null) {
            switch (MotionEventCompat.getActionMasked(event)) {
                case MotionEvent.ACTION_DOWN:
                    ViewParent parent = v.getParent();
                    if (parent != null) {
                        //禁止父视图拦截接下来的事件，默认 DOWN 事件不能被拦截
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    cancelFling();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (getScale() < mMinScale) {
                        //缩放系数过小
                        RectF rectF = getDisplayRect();
                        if (rectF != null) {
                            //执行缩放动画
                            v.post(new AnimatedZoomRunnable(getScale(), mMinScale, rectF.centerX(), rectF.centerY()));
                            handled = true;
                        }
                    } else if (getScale() > mMaxScale) {
                        RectF rectF = getDisplayRect();
                        if (rectF != null) {
                            //执行缩放动画
                            v.post(new AnimatedZoomRunnable(getScale(), mMaxScale, rectF.centerX(), rectF.centerY()));
                            handled = true;
                        }
                    }
                    break;
            }

            //分发给 ScaleGestureDetector
            if (mScaleDragDetector != null) {
                boolean wasScaling = mScaleDragDetector.isScaling();
                boolean wasDragging = mScaleDragDetector.isDragging();

                handled = mScaleDragDetector.onTouchEvent(event);

                boolean didntScale = !wasScaling && !mScaleDragDetector.isScaling();
                boolean didntDrag = !wasDragging && !mScaleDragDetector.isDragging();

                //双重确认 ScaleGestureDetector 不需要消费事件
                mBlockParentIntercept = didntScale && didntDrag;
            }

            //分发给 GestureDetector
            if (mGestureDetector != null && mGestureDetector.onTouchEvent(event)) {
                handled = true;
            }

        }

        return handled;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            updateBaseMatrix(mImageView.getDrawable());
        }
    }

    void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.MATRIX) {
            throw new IllegalArgumentException("Matrix scale is not supported");
        }
        if (scaleType == null || scaleType == mScaleType) {
            return;
        }
        mScaleType = scaleType;
        update();
    }

    ScaleType getScaleType() {
        return mScaleType;
    }

    void update() {
        if (mZoomEnabled) {
            updateBaseMatrix(mImageView.getDrawable());
        } else {
            resetMatrix();
        }
    }

    float getScale() {
        // TODO: 2017/9/30 计算当前 scale
        float currentScale = (float) Math.sqrt((float) Math.pow(getValue(mSuppMatrix, Matrix.MSCALE_X), 2)
                + (float) Math.pow(getValue(mSuppMatrix, Matrix.MSKEW_Y), 2));
        return currentScale;
    }

    void setScale(float scale, float focalX, float focalY, boolean animate) {
        if (scale < mMinScale || scale > mMaxScale) {
            throw new IllegalArgumentException("Scale must be within the range of minScale and maxScale");
        }

        if (animate) {
            mImageView.post(new AnimatedZoomRunnable(getScale(), scale, focalX, focalY));
        } else {
            mSuppMatrix.setScale(scale, scale, focalX, focalY);
            checkAndDisplayMatrix();
        }
    }

    RectF getDisplayRect() {
        checkMatrixBounds();
        return getDisplayRect(getDrawMatrix());
    }

    void setBaseRotation(float degress) {
        if (mBaseRotation == degress) {
            return;
        }

        mBaseRotation = degress;
        update();
        setRotationBy(mBaseRotation);
    }

    void setRotationTo(float degress) {
        mSuppMatrix.setRotate(degress % 360);
        checkAndDisplayMatrix();
    }

    void setRotationBy(float degress) {
        mSuppMatrix.postRotate(degress % 360);
        checkAndDisplayMatrix();
    }

    private void resetMatrix() {
        mSuppMatrix.reset();
        setRotationBy(mBaseRotation);
        setImageViewMatrix(getDrawMatrix());
        checkMatrixBounds();
    }

    private void updateBaseMatrix(Drawable drawable) {
        if (drawable == null) {
            return;
        }

        final float viewWidth = getImageViewWidth(mImageView);
        final float viewHeight = getImageViewHeight(mImageView);
        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();

        mBaseMatrix.reset();

        final float widthScale = viewWidth / drawableWidth;
        final float heightScale = viewHeight / drawableHeight;

        if (mScaleType == ScaleType.CENTER) {

            mBaseMatrix.postTranslate((viewWidth - drawableHeight) / 2f, (viewHeight - drawableHeight) / 2f);

        } else if (mScaleType == ScaleType.CENTER_CROP) {

            float scale = Math.max(widthScale, heightScale);
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2f, (viewHeight - drawableHeight * scale) / 2f);

        } else if (mScaleType == ScaleType.CENTER_INSIDE) {

            float scale = Math.min(1.0f, Math.min(widthScale, heightScale));
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2f, (viewHeight - drawableHeight * scale) / 2f);

        } else {

            RectF tempSrc = new RectF(0, 0, drawableWidth, drawableHeight);
            RectF tempDst = new RectF(0, 0, viewWidth, viewHeight);

            if ((int) mBaseRotation % 180 != 0) {
                tempSrc = new RectF(0, 0, drawableHeight, drawableWidth);
            }

            switch (mScaleType) {
                case FIT_CENTER:
                    mBaseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);
                    break;
                case FIT_START:
                    mBaseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.START);
                    break;
                case FIT_END:
                    mBaseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.END);
                    break;
                case FIT_XY:
                    mBaseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.FILL);
                    break;
                default:
                    break;
            }
        }

        resetMatrix();
    }

    private void checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            setImageViewMatrix(getDrawMatrix());
        }
    }

    private void setImageViewMatrix(Matrix matrix) {
        mImageView.setImageMatrix(matrix);
    }

    /** 检测当前 Drawable 的矩阵是否在正确的显示范围 */
    private boolean checkMatrixBounds() {
        final RectF rectF = getDisplayRect(getDrawMatrix());
        if (rectF == null) {
            return false;
        }

        final float height = rectF.height();
        final float width = rectF.width();
        float deltaX = 0;
        float deltaY = 0;

        final int viewHeight = getImageViewHeight(mImageView);
        if (height <= viewHeight) {
            switch (mScaleType) {
                case FIT_START:
                    //需要向上偏移
                    deltaY = -rectF.top;
                    break;
                case FIT_END:
                    //需要向下偏移
                    deltaY = viewHeight - height - rectF.top;
                    break;
                default:
                    //居中处理
                    deltaY = (viewHeight - height) / 2 - rectF.top;
                    break;
            }
        } else if (rectF.top > 0) {
            //置顶
            deltaY = -rectF.top;
        } else if (rectF.bottom < viewHeight) {
            //置底
            deltaY = viewHeight - rectF.bottom;
        }

        final int viewWidth = getImageViewWidth(mImageView);
        if (width <= viewWidth) {
            switch (mScaleType) {
                case FIT_START:
                    //向左偏移
                    deltaX = -rectF.left;
                    break;
                case FIT_END:
                    //向右偏移
                    deltaY = viewWidth - width - rectF.left;
                    break;
                default:
                    //居中处理
                    deltaX = (viewWidth - width) / 2 - rectF.left;
                    break;
            }
            mScrollEdge = EDGE_BOTH;
        } else if (rectF.left > 0) {
            mScrollEdge = EDGE_LEFT;
            deltaX = -rectF.left;
        } else if (rectF.right < viewWidth) {
            mScrollEdge = EDGE_RIGHT;
            deltaX = viewWidth - rectF.right;
        } else {
            mScrollEdge = EDGE_NONE;
        }

        mSuppMatrix.postTranslate(deltaX, deltaY);
        return true;
    }

    private int getImageViewHeight(ImageView imageView) {
        return imageView.getHeight() - imageView.getPaddingTop() - imageView.getPaddingBottom();
    }

    private int getImageViewWidth(ImageView imageView) {
        return imageView.getWidth() - imageView.getPaddingLeft() - imageView.getPaddingRight();
    }

    private Matrix getDrawMatrix() {
        mDrawMatrix.set(mBaseMatrix);
        mDrawMatrix.postConcat(mSuppMatrix);
        return mDrawMatrix;
    }

    private RectF getDisplayRect(Matrix matrix) {
        Drawable drawable = mImageView.getDrawable();
        if (drawable != null) {
            mDisplayRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(mDisplayRect);
            return mDisplayRect;
        }
        return null;
    }

    private float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    private void cancelFling() {
        if (mCurrentFlingRunnable != null) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable = null;
        }
    }

    private final CustomGestureDetector.OnGestureListener mOnCustomGestureListener = new CustomGestureDetector.OnGestureListener() {

        @Override
        public void onDrag(float dx, float dy) {
            if (mScaleDragDetector.isScaling()) {
                return;
            }

            mSuppMatrix.postTranslate(dx, dy);
            checkAndDisplayMatrix();

            //是否让父视图拦截事件
            ViewParent parent = mImageView.getParent();
            if (parent == null) {
                return;
            }
            if (mAllowParentInterceptOnEdge && !mScaleDragDetector.isScaling() && !mBlockParentIntercept) {
                if (mScrollEdge == EDGE_BOTH
                        || (mScrollEdge == EDGE_LEFT && dx >= 1f)
                        || (mScrollEdge == EDGE_RIGHT && dx <= -1f)) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
            } else {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }

        @Override
        public void onFling(float startX, float startY, float velocityX, float velocityY) {
            mCurrentFlingRunnable = new FlingRunnable(mImageView.getContext());
            mCurrentFlingRunnable.fling(getImageViewWidth(mImageView), getImageViewHeight(mImageView)
                    , (int) velocityX, (int) velocityY);
            mImageView.post(mCurrentFlingRunnable);
        }

        @Override
        public void onScale(float scaleFactor, float focusX, float focusY) {
            if ((getScale() < (mMaxScale + mOverScaleCoefficient) || scaleFactor < 1f)
                    && (getScale() > (mMinScale - mOverScaleCoefficient) || scaleFactor > 1f)) {
                mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                checkAndDisplayMatrix();
            }
        }
    };

    private final GestureDetector.SimpleOnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            try {
                float scale = getScale();
                float x = e.getX();
                float y = e.getY();

                if (scale < mMidScale) {
                    setScale(mMidScale, x, y, true);
                } else if (scale < mMaxScale) {
                    setScale(mMaxScale, x, y, true);
                } else {
                    setScale(mMinScale, x, y, true);
                }
            } catch (ArrayIndexOutOfBoundsException ignore) {
            }

            return true;
        }


    };

    private class AnimatedZoomRunnable implements Runnable {
        private final float mFocalX, mFocalY;
        private final long mStartTime;
        private final float mZoomStart, mZoomEnd;

        AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                             final float focalX, final float focalY) {
            mFocalX = focalX;
            mFocalY = focalY;
            mStartTime = System.currentTimeMillis();
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
        }

        @Override
        public void run() {
            float t = interpolate();
            float scale = mZoomStart + t * (mZoomEnd - mZoomStart);
            float deltaScale = scale / getScale();

            mOnCustomGestureListener.onScale(deltaScale, mFocalX, mFocalY);

            if (t < 1f) {
                ViewCompat.postOnAnimation(mImageView, this);
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration;
            t = Math.min(1f, t);
            t = mInterpolator.getInterpolation(t);
            return t;
        }
    }

    private class FlingRunnable implements Runnable {
        private final OverScroller mScroller;
        private int mCurrentX, mCurrentY;

        FlingRunnable(Context context) {
            mScroller = new OverScroller(context);
        }

        @Override
        public void run() {
            if (mScroller.isFinished()) {
                return;
            }

            if (mScroller.computeScrollOffset()) {
                final int newX = mScroller.getCurrX();
                final int newY = mScroller.getCurrY();


                // TODO: 2017/9/30 scroll 的正负是相反的
                mSuppMatrix.postTranslate(mCurrentX - newX, mCurrentY - newY);
                checkAndDisplayMatrix();

                mCurrentX = newX;
                mCurrentY = newY;

                ViewCompat.postOnAnimation(mImageView, this);
            }
        }

        void fling(int viewWidth, int viewHeight, int velocityX,
                   int velocityY) {
            final RectF rectF = getDisplayRect();
            if (rectF == null) {
                return;
            }

            final int startX = Math.round(-rectF.left);
            final int minX, maxX, minY, maxY;

            if (viewWidth < rectF.width()) {
                minX = 0;
                maxX = Math.round(rectF.width() - viewWidth);
            } else {
                //视图的宽更大，不可拖动
                minX = maxX = startX;
            }

            final int startY = Math.round(-rectF.top);
            if (viewHeight < rectF.height()) {
                minY = 0;
                maxY = Math.round(rectF.height() - viewHeight);
            } else {
                //视图的高更大，不可拖动
                minY = maxY = startY;
            }

            mCurrentX = startX;
            mCurrentY = startY;

            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
            }
        }

        void cancelFling() {
            mScroller.forceFinished(true);
        }
    }

}
