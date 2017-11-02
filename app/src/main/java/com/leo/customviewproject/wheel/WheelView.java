package com.leo.customviewproject.wheel;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.Px;
import android.support.v4.util.Pools;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.OverScroller;

import com.leo.customviewproject.R;
import com.leo.customviewproject.utils.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2017/10/31 下午8:35.
 * leo linxiaotao1993@vip.qq.com
 */
@SuppressWarnings("unused")
public final class WheelView extends View {

    private List<String> mDataSources;
    private int mVisibilityCount = AUTO_VISIBILITY_COUNT;
    @Px
    private int mTextSize = (int) DisplayUtils.sp2px(16);
    @Px
    private int mTextVerticalSpacing = (int) DisplayUtils.dp2px(10);
    @ColorInt
    private int mNormalTextColor = Color.LTGRAY;
    @ColorInt
    private int mSelectedTextColor = Color.BLACK;
    @ColorInt
    private int mSelectedLineColor = Color.BLACK;
    private int mTextGravity = Gravity.CENTER;
    private CallBack mCallBack;
    private int mSelectPosition;

    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDrawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mContentRect = new Rect();
    //textRect,selectRect 都以 ContentRect 为基准
    private final Rect mTextRect = new Rect();
    private final Rect mSelctedRect = new Rect();
    /** 单个文字最大的尺寸 */
    private int mMaxTextWidth, mMaxTextHeight;
    private int mDistanceY;
    private int mMaximumDistanceY;
    private int mMinimumDistanceY;
    private boolean mNeedCalculate;
    /** touch */
    private boolean mIsBeingDragged;
    private final int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private final int mMaximumFlingVelocity;
    private final int mMinimumFlingVelocity;
    private final OverScroller mScroller;
    private int mActivePointerId = INVALID_POINTER;
    private float mDownY;
    private float mLastY;
    private boolean mIsBeingFling;
    private float mFlingY;
    private ValueAnimator mRunAnimator;

    private static final int DEFALUT_VISIBILITY_COUNT = 5;
    private static final int AUTO_VISIBILITY_COUNT = -1;
    private static final int INVALID_POINTER = -1;
    private static final String TAG = "WheelView";

    private static final Pools.Pool<Rect> RECT_POOL = new Pools.SimplePool<>(20);

    private static Rect acquireRect() {
        Rect rect = RECT_POOL.acquire();
        if (rect == null) {
            rect = new Rect();
        }
        return rect;
    }

    private static void releaseRect(Rect rect) {
        rect.setEmpty();
        RECT_POOL.release(rect);
    }

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            mDataSources = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                mDataSources.add("测试" + i);
            }
        }

        final ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mScroller = new OverScroller(context);

        init(context, attrs);
    }

    public void setSelectPosition(int position) {
        if (!hasDataSource() || mSelectPosition == position) {
            return;
        }
        if (position > (mDataSources.size() - 1) || position < 0) {
            return;
        }
        if (mMaximumDistanceY == 0) {
            calculate();
        }
        mSelectPosition = position;
        int newDistance = position * mTextRect.height();
        animChangeDistanceY(newDistance);
    }

    public void setDataSources(List<String> dataSources) {
        if (mDataSources == dataSources) {
            return;
        }
        mDataSources = dataSources;
        setSelectPosition(0);
        requestLayout();
        postInvalidateOnAnimation();
    }

    public void setVisibilityCount(int visibilityCount) {
        if (mVisibilityCount == visibilityCount) {
            return;
        }
        mVisibilityCount = visibilityCount;
        requestLayout();
        postInvalidateOnAnimation();
    }

    public void setTextSize(@Px int textSize) {
        if (mTextSize == textSize) {
            return;
        }
        mTextSize = textSize;
        requestLayout();
        postInvalidateOnAnimation();
    }

    public void setTextVerticalSpacing(int textVerticalSpacing) {
        if (mTextVerticalSpacing == textVerticalSpacing) {
            return;
        }
        mTextVerticalSpacing = textVerticalSpacing;
        requestLayout();
        postInvalidateOnAnimation();
    }

    public void setNormalTextColor(@ColorInt int normalTextColor) {
        if (mNormalTextColor == normalTextColor) {
            return;
        }
        mNormalTextColor = normalTextColor;
        postInvalidateOnAnimation();
    }

    public void setSelectedTextColor(@ColorInt int selectedTextColor) {
        if (mSelectedTextColor == selectedTextColor) {
            return;
        }
        mSelectedTextColor = selectedTextColor;
        postInvalidateOnAnimation();
    }

    public void setSelectedLineColor(@ColorInt int selectedLineColor) {
        if (mSelectedLineColor == selectedLineColor) {
            return;
        }
        mSelectedLineColor = selectedLineColor;
        postInvalidateOnAnimation();
    }

    public void setTextGravity(int textGravity) {
        if (mTextGravity == textGravity) {
            return;
        }
        mTextGravity = textGravity;
        postInvalidateOnAnimation();
    }

    public void setCallBack(CallBack callBack) {
        mCallBack = callBack;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wantWith = getPaddingLeft() + getPaddingRight();
        int wantHeight = getPaddingTop() + getPaddingBottom();
        calculateTextSize();
        wantWith += mTextRect.width();
        if (mVisibilityCount > 0) {
            wantHeight += mTextRect.height() * mVisibilityCount;
        } else {
            wantHeight += mTextRect.height() * DEFALUT_VISIBILITY_COUNT;
        }
        setMeasuredDimension(resolveSize(wantWith, widthMeasureSpec), resolveSize(wantHeight, heightMeasureSpec));
        mNeedCalculate = true;
    }


    private void calculate() {
        mContentRect.set(getPaddingLeft(), getPaddingTop(), getMeasuredWidth() - getPaddingRight(),
                getMeasuredHeight() - getPaddingBottom());
        if (mVisibilityCount > 0) {
            mTextRect.set(0, 0, mContentRect.width(),
                    (int) (mContentRect.height() * 1.0 / mVisibilityCount));
        } else {
            mTextRect.set(0, 0, mContentRect.width(),
                    mMaxTextHeight + 2 * mTextVerticalSpacing);
        }
        final int contentCentY = mContentRect.centerY();
        int position = contentCentY / mTextRect.height();
        if (contentCentY % mTextRect.height() > 0) {
            position++;
        }
        mSelctedRect.set(0, mTextRect.height() * (position - 1),
                mContentRect.width(), mTextRect.height() * position);
        calculateDistanceY();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mNeedCalculate) {
            mNeedCalculate = false;
            calculate();
        }

        canvas.clipRect(mContentRect);

        if (hasDataSource()) {
            int selctPosition = mDistanceY / mTextRect.height();
            final int remainder = mDistanceY % mTextRect.height();
            if (remainder > mTextRect.height() / 2f) {
                selctPosition++;
            }
            selctPosition = Math.min(selctPosition, mDataSources.size() - 1);
            if (!mIsBeingDragged && !mIsBeingFling && mSelectPosition != selctPosition &&
                    (mRunAnimator == null || !mRunAnimator.isRunning())) {
                if (mCallBack != null) {
                    mCallBack.onPositionSelect(selctPosition);
                }
                mSelectPosition = selctPosition;
            }
            final int drawCount = mContentRect.height() / mTextRect.height() + 2;
            int invisibleCount = 0;
            int dy = -mDistanceY;
            //这里所有的计算偏移都以 ContentRect 为基准
            if (mDistanceY > mSelctedRect.top) {
                invisibleCount = (mDistanceY - mSelctedRect.top) / mTextRect.height();
                dy = -(mDistanceY - invisibleCount * mTextRect.height());
            }
            int saveCount = canvas.save();
            //padding top
            canvas.translate(mContentRect.left, mContentRect.top);
            canvas.translate(0, mSelctedRect.top);
            canvas.translate(0, dy);
            for (int i = 0; (i < drawCount && mDataSources.size() > (invisibleCount + i));
                 i++) {
                final int position = invisibleCount + i;
                String text = mDataSources.get(position);
                if (i > 0) {
                    canvas.translate(0, mTextRect.height());
                }

                final PointF pointF = calculateTextGravity(text);
                mTextPaint.setTextSize(mTextSize);
                if (position == selctPosition) {
                    mTextPaint.setColor(mSelectedTextColor);
                } else {
                    mTextPaint.setColor(mNormalTextColor);
                }
                canvas.drawText(text, pointF.x, pointF.y, mTextPaint);
            }
            canvas.restoreToCount(saveCount);
        }

        int saveCount = canvas.save();
        mDrawPaint.setColor(mSelectedLineColor);
        canvas.translate(mContentRect.left, mContentRect.top);
        canvas.drawLine(mSelctedRect.left, mSelctedRect.top, mSelctedRect.right, mSelctedRect.top, mDrawPaint);
        canvas.drawLine(mSelctedRect.left, mSelctedRect.bottom, mSelctedRect.right, mSelctedRect.bottom, mDrawPaint);
        canvas.restoreToCount(saveCount);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!hasDataSource()) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initOrResetVelocityTracker();
                mIsBeingFling = false;
                mScroller.computeScrollOffset();
                if (mIsBeingDragged = !mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                if (mRunAnimator != null) {
                    mRunAnimator.cancel();
                }
                if (mIsBeingDragged) {
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                mActivePointerId = event.getPointerId(0);
                mDownY = event.getY(0);
                mLastY = mDownY;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int actionIndex = event.getActionIndex();
                mActivePointerId = event.getPointerId(actionIndex);
                mDownY = event.getY(actionIndex);
                mLastY = mDownY;
                break;
            case MotionEvent.ACTION_MOVE:
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    break;
                }
                final int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "onTouchEvent: invalid pointer index");
                    break;
                }
                final float moveY = event.getY(pointerIndex);
                if (!mIsBeingDragged && Math.abs(mDownY - moveY) > mTouchSlop) {
                    mIsBeingDragged = true;
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (mIsBeingDragged) {
                    mDistanceY += mLastY - moveY;
                    if (mDistanceY > mMaximumDistanceY) {
                        mDistanceY = mMaximumDistanceY;
                    }
                    if (mDistanceY < mMinimumDistanceY) {
                        mDistanceY = mMinimumDistanceY;
                    }
                    postInvalidateOnAnimation();
                }
                mLastY = moveY;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                if (Math.abs(velocityTracker.getYVelocity()) > mMinimumFlingVelocity) {
                    mScroller.fling(0, 0, 0, (int) velocityTracker.getYVelocity(),
                            0, 0, -mMaximumDistanceY, mMaximumDistanceY);
                    mFlingY = mScroller.getStartY();
                    mIsBeingFling = true;
                    postInvalidateOnAnimation();
                } else {
                    correctionDistanceY();
                }
                mActivePointerId = INVALID_POINTER;
                mIsBeingDragged = false;
                resetVelocityTracker();
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsBeingFling = false;
                mActivePointerId = INVALID_POINTER;
                mIsBeingDragged = false;
                resetVelocityTracker();
                correctionDistanceY();
                break;
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            mDistanceY += mFlingY - mScroller.getCurrY();
            if (mDistanceY > mMaximumDistanceY) {
                mDistanceY = mMaximumDistanceY;
            }
            if (mDistanceY < mMinimumDistanceY) {
                mDistanceY = mMinimumDistanceY;
            }
            mFlingY = mScroller.getCurrY();
            postInvalidateOnAnimation();
        } else if (mIsBeingFling) {
            mIsBeingFling = false;
            correctionDistanceY();
        }
    }

    public interface CallBack {

        void onPositionSelect(int position);
    }

    private void init(Context context, AttributeSet attributeSet) {
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.WheelView);
        try {
            setTextSize(typedArray.getDimensionPixelSize(R.styleable.WheelView_textSize, mTextSize));
            setTextVerticalSpacing(typedArray.getDimensionPixelSize(R.styleable.WheelView_textVerticalSpacing, mTextVerticalSpacing));
            setNormalTextColor(typedArray.getColor(R.styleable.WheelView_normalTextColor, mNormalTextColor));
            setSelectedTextColor(typedArray.getColor(R.styleable.WheelView_selectedTextColor, mSelectedTextColor));
            setSelectedLineColor(typedArray.getColor(R.styleable.WheelView_selectedLineColor, mSelectedLineColor));
            setTextGravity(typedArray.getInt(R.styleable.WheelView_textGravity, mTextGravity));
            setSelectPosition(typedArray.getInt(R.styleable.WheelView_selectPosition, mSelectPosition));
            setVisibilityCount(typedArray.getInt(R.styleable.WheelView_visibilityCount, mVisibilityCount));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (typedArray != null) {
                typedArray.recycle();
            }
        }
    }

    private void calculateTextSize() {
        mMaxTextWidth = 0;
        mMaxTextHeight = 0;
        if (!hasDataSource()) {
            return;
        }
        mTextPaint.setTextSize(mTextSize);
        final Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mMaxTextHeight = (int) (fontMetrics.bottom - fontMetrics.top);
        for (String text : mDataSources) {
            mMaxTextWidth = (int) Math.max(mTextPaint.measureText(text), mMaxTextWidth);
        }
        mTextRect.set(0, 0, mMaxTextWidth, mMaxTextHeight + 2 * mTextVerticalSpacing);
        calculateDistanceY();
    }

    private void calculateDistanceY() {
        mMaximumDistanceY = 0;
        mMinimumDistanceY = 0;
        if (!hasDataSource()) {
            return;
        }
        mMaximumDistanceY = mTextRect.height() * (mDataSources.size() - 1);
    }

    private PointF calculateTextGravity(String text) {
        PointF pointF = new PointF();
        final Rect textSizeRect = acquireRect();
        mTextPaint.getTextBounds(text, 0, text.length(), textSizeRect);
        switch (mTextGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                pointF.y = mTextRect.top + textSizeRect.height() - Math.abs(textSizeRect.bottom);
                break;
            case Gravity.BOTTOM:
                pointF.y = textSizeRect.bottom;
                break;
            default:
            case Gravity.CENTER_VERTICAL:
                pointF.y = mTextRect.exactCenterY() + textSizeRect.height() / 2f
                        - Math.abs(textSizeRect.bottom);
                break;
        }
        switch (mTextGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
            case Gravity.START:
                pointF.x = mTextRect.left;
                break;
            case Gravity.END:
            case Gravity.RIGHT:
                pointF.x = mTextRect.bottom - textSizeRect.width();
                break;
            default:
            case Gravity.CENTER_HORIZONTAL:
                pointF.x = mTextRect.exactCenterX() - textSizeRect.width() / 2f;
                break;
        }
        releaseRect(textSizeRect);
        return pointF;
    }

    private void correctionDistanceY() {
        if (mDistanceY % mTextRect.height() != 0) {
            int position = mDistanceY / mTextRect.height();
            int remainder = mDistanceY % mTextRect.height();
            if (remainder >= mTextRect.height() / 2f) {
                position++;
            }
            int newDistanceY = position * mTextRect.height();
            animChangeDistanceY(newDistanceY);
        }
    }

    private void animChangeDistanceY(int newDistanceY) {
        if (newDistanceY > mMaximumDistanceY) {
            newDistanceY = mMaximumDistanceY;
        }
        if (newDistanceY < mMinimumDistanceY) {
            newDistanceY = mMinimumDistanceY;
        }
        if (newDistanceY != mDistanceY) {
            if (mRunAnimator != null && mRunAnimator.isRunning()) {
                mRunAnimator.cancel();
            }
            mRunAnimator = ValueAnimator.ofInt(mDistanceY, newDistanceY);
            mRunAnimator.addUpdateListener(animation -> {
                mDistanceY = (int) animation.getAnimatedValue();
                postInvalidateOnAnimation();
            });
            mRunAnimator.start();
        }
    }

    private void onSecondaryPointerUp(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        if (pointerId == mActivePointerId) {
            actionIndex = actionIndex == 0 ? 1 : 0;
            mActivePointerId = event.getPointerId(actionIndex);
            mDownY = event.getY(actionIndex);
            mLastY = mDownY;
            mVelocityTracker.clear();
        }
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void resetVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    private boolean hasDataSource() {
        return mDataSources != null && !mDataSources.isEmpty();
    }


}
