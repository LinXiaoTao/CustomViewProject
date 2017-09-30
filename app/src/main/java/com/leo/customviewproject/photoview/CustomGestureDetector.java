package com.leo.customviewproject.photoview;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

/**
 * Created on 2017/9/29 下午4:11.
 * leo linxiaotao1993@vip.qq.com
 */

final class CustomGestureDetector {

    private final Context mContext;

    private final OnGestureListener mListener;

    private final float mMinimumVelocity;
    private final float mTouchSlop;

    private final ScaleGestureDetector mDetector;


    private int mActivePointerId = INVALID_POINTER_ID;
    private int mActivePointerIndex = 0;

    private VelocityTracker mVelocityTracker;

    private float mLastTouchX;
    private float mLastTouchY;

    private boolean mIsDragging;


    private static final int INVALID_POINTER_ID = -1;

    private static final float MAX_SCALE_FACTOR = 10f;
    private static final float MIN_SCALE_FACTOR = 0.1f;

    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            float scaleFactor = detector.getScaleFactor();

            scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(scaleFactor, MAX_SCALE_FACTOR));

            if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor)) {
                return false;
            }

            mListener.onScale(scaleFactor, detector.getFocusX(), detector.getFocusY());

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            System.out.println("onScaleBegin");
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            System.out.println("onScaleEnd");
            super.onScaleEnd(detector);
        }
    };

    CustomGestureDetector(Context context, OnGestureListener listener) {
        mContext = context;
        mListener = listener;

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mTouchSlop = configuration.getScaledTouchSlop();

        mDetector = new ScaleGestureDetector(context, mScaleGestureListener);
    }

    boolean onTouchEvent(MotionEvent event) {
        try {
            mDetector.onTouchEvent(event);
            return processTouchEvent(event);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    boolean isDragging() {
        return mIsDragging;
    }

    boolean isScaling() {
        return mDetector.isInProgress();
    }

    private boolean processTouchEvent(MotionEvent event) {
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN:

                //记录活动的手指
                mActivePointerId = event.getPointerId(0);

                mVelocityTracker = VelocityTracker.obtain();
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);
                }

                mLastTouchX = getActiveX(event);
                mLastTouchY = getActiveY(event);
                mIsDragging = false;


                break;
            case MotionEvent.ACTION_MOVE:

                final float x = getActiveX(event);
                final float y = getActiveY(event);
                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                if (!mIsDragging) {
                    //计算手指滑动的距离有没有超过临界值
                    mIsDragging = Math.sqrt((dx * dx) + (dy * dy)) >= mTouchSlop;
                }

                if (mIsDragging) {
                    mListener.onDrag(dx, dy);
                    mLastTouchX = x;
                    mLastTouchY = y;

                    if (mVelocityTracker != null) {
                        mVelocityTracker.addMovement(event);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:

                mActivePointerId = INVALID_POINTER_ID;
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                break;
            case MotionEvent.ACTION_UP:

                mActivePointerId = INVALID_POINTER_ID;
                if (mIsDragging) {
                    if (mVelocityTracker != null) {
                        mLastTouchX = getActiveX(event);
                        mLastTouchY = getActiveY(event);

                        mVelocityTracker.addMovement(event);
                        mVelocityTracker.computeCurrentVelocity(1000);

                        final float vX = mVelocityTracker.getXVelocity();
                        final float vY = mVelocityTracker.getYVelocity();

                        if (Math.max(Math.abs(vX), Math.abs(vY)) >= mMinimumVelocity) {
                            // TODO: 2017/9/29 速度的正负
                            mListener.onFling(mLastTouchX, mLastTouchY, -vX, -vY);
                        }
                    }
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                break;
            case MotionEvent.ACTION_POINTER_UP://单个手指抬起

                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    //抬起的刚好是活动的手指
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = event.getPointerId(newPointerIndex);
                    mLastTouchX = event.getX(newPointerIndex);
                    mLastTouchY = event.getY(newPointerIndex);
                }

                break;
        }

        mActivePointerIndex = event.findPointerIndex(mActivePointerId != INVALID_POINTER_ID ? mActivePointerId : 0);

        return true;
    }

    private float getActiveX(MotionEvent event) {
        try {
            return event.getX(mActivePointerIndex);
        } catch (Exception e) {
            return event.getX();
        }
    }

    private float getActiveY(MotionEvent event) {
        try {
            return event.getY(mActivePointerIndex);
        } catch (Exception e) {
            return event.getY();
        }
    }

    interface OnGestureListener {

        void onDrag(float dx, float dy);

        void onFling(float startX, float startY, float velocityX,
                     float velocityY);

        void onScale(float scaleFactor, float focusX, float focusY);
    }

}
