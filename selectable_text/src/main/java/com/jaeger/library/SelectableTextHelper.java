package com.jaeger.library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Created by Jaeger on 16/8/30.
 * <p>
 * Email: chjie.jaeger@gmail.com
 * GitHub: https://github.com/laobie
 */
public class SelectableTextHelper {

    private final static int DEFAULT_SELECTION_LENGTH = 1;
    private static final int DEFAULT_SHOW_DURATION = 100;

    private CursorHandle mStartHandle;
    private CursorHandle mEndHandle;
    private OperateWindow mOperateWindow;
    public SelectionInfo mSelectionInfo = new SelectionInfo();
    private OnSelectListener mSelectListener;

    private Context mContext;
    private TextView mTextView;
    private Spannable mSpannable;

    private int mSelectedColor;
    private int mCursorHandleColor;
    private int mCursorHandleSize;
    private Object mSpan;
    private boolean isHideWhenScroll;
    public boolean isHide = true;
    private Paint.FontMetricsInt metricsInt = new Paint.FontMetricsInt();

    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

    private int lastTouchX;
    private int lastTouchY;

    public SelectableTextHelper(Builder builder) {
        mTextView = builder.mTextView;
        mContext = mTextView.getContext();
        mSelectedColor = builder.mSelectedColor;
        mCursorHandleColor = builder.mCursorHandleColor;
        mCursorHandleSize = TextLayoutUtil.dp2px(mContext, builder.mCursorHandleSizeInDp);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        mTextView.setText(mTextView.getText(), TextView.BufferType.SPANNABLE);

        mTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                lastTouchX = (int) event.getX();
                lastTouchY = (int) event.getY();
                return false;
            }
        });

        mTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showSelectView(lastTouchX,lastTouchY);
                return true;
            }
        });

        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetSelectionInfo();
                hideSelectView();
            }
        });
        mTextView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                destroy();
            }
        });

        mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (isHideWhenScroll) {
                    isHideWhenScroll = false;
                    postShowSelectView(DEFAULT_SHOW_DURATION);
                }
                return true;
            }
        };
        mTextView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);

        mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (!isHideWhenScroll && !isHide) {
                    isHideWhenScroll = true;
                    if (mOperateWindow != null) {
                        mOperateWindow.dismiss();
                    }
                    if (mStartHandle != null) {
                        mStartHandle.dismiss();
                    }
                    if (mEndHandle != null) {
                        mEndHandle.dismiss();
                    }
                }
            }
        };
        mTextView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);

        mOperateWindow = new OperateWindow(mContext);
    }

    private void postShowSelectView(int duration) {
        mTextView.removeCallbacks(mShowSelectViewRunnable);
        if (duration <= 0) {
            mShowSelectViewRunnable.run();
        } else {
            mTextView.postDelayed(mShowSelectViewRunnable, duration);
        }
    }

    private final Runnable mShowSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHide) return;
            if (mOperateWindow != null) {
                mOperateWindow.show();
            }
            if (mStartHandle != null) {
                showCursorHandle(mStartHandle);
            }
            if (mEndHandle != null) {
                showCursorHandle(mEndHandle);
            }
        }
    };

    private void hideSelectView() {
        isHide = true;
        if (mStartHandle != null) {
            mStartHandle.dismiss();
        }
        if (mEndHandle != null) {
            mEndHandle.dismiss();
        }
        if (mOperateWindow != null) {
            mOperateWindow.dismiss();
        }
    }

    private void resetSelectionInfo() {
        mSelectionInfo.mSelectionContent = null;
        if (mSpannable != null && mSpan != null) {
            mSpannable.removeSpan(mSpan);
            mSpan = null;
        }
    }


    public void showSelectView() {
        //注意 lastTouchX 有没有赋值
        showSelectView(lastTouchX, lastTouchY);
    }

    public void showSelectView(int x, int y) {
        hideSelectView();
        resetSelectionInfo();
        isHide = false;
        if (mStartHandle == null) mStartHandle = new CursorHandle(true);
        if (mEndHandle == null) mEndHandle = new CursorHandle(false);

        int startOffset = TextLayoutUtil.getPreciseOffset(mTextView, x, y);
        int endOffset = startOffset + DEFAULT_SELECTION_LENGTH;
        if (mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }
        if (mSpannable == null || startOffset >= mTextView.getText().length() || startOffset < 0) {
            return;
        }
        selectText(startOffset, endOffset);
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
        mOperateWindow.show();
    }

    private void showCursorHandle(CursorHandle cursorHandle) {
        Layout layout = mTextView.getLayout();
        int offset = cursorHandle.isLeft ? mSelectionInfo.mStart : mSelectionInfo.mEnd;
        cursorHandle.show((int) layout.getPrimaryHorizontal(offset), layout.getLineBottom(layout.getLineForOffset(offset)));
    }

    private void selectText(int startPos, int endPos) {
        if (startPos != -1) {
            mSelectionInfo.mStart = startPos;
        }
        if (endPos != -1) {
            mSelectionInfo.mEnd = endPos;
        }
        if (mSelectionInfo.mStart > mSelectionInfo.mEnd) {
            int temp = mSelectionInfo.mStart;
            mSelectionInfo.mStart = mSelectionInfo.mEnd;
            mSelectionInfo.mEnd = temp;
        }

        if (mSpannable != null) {
            if (mSpan == null) {
                mSpan = new BgColorSpan(mSelectedColor);
            }
            mSelectionInfo.mSelectionContent = mSpannable.subSequence(mSelectionInfo.mStart, mSelectionInfo.mEnd).toString();
            mSpannable.setSpan(mSpan, mSelectionInfo.mStart, mSelectionInfo.mEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
    }

    public void setSelectListener(OnSelectListener selectListener) {
        mSelectListener = selectListener;
    }

    public void destroy() {
        resetSelectionInfo();
        hideSelectView();
    }

    /**
     * Operate windows : copy, select all
     */
    private class OperateWindow {

        private PopupWindow mWindow;
        private int[] mTempCoors = new int[2];

        private int mWidth;
        private int mHeight;

        public OperateWindow(final Context context) {
            View contentView = LayoutInflater.from(context).inflate(R.layout.layout_operate_windows, null);
            contentView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            mWidth = contentView.getMeasuredWidth();
            mHeight = contentView.getMeasuredHeight();
            mWindow =
                    new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
            mWindow.setClippingEnabled(false);
            contentView.findViewById(R.id.feedback).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSelectListener != null) {
                        mSelectListener.onTextSelected(mSelectionInfo.mSelectionContent);
                    }
                    SelectableTextHelper.this.resetSelectionInfo();
                    SelectableTextHelper.this.hideSelectView();
                }
            });
//            contentView.findViewById(R.id.tv_copy).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    ClipboardManager clip = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
//                    clip.setPrimaryClip(
//                        ClipData.newPlainText(mSelectionInfo.mSelectionContent, mSelectionInfo.mSelectionContent));
//                    if (mSelectListener != null) {
//                        mSelectListener.onTextSelected(mSelectionInfo.mSelectionContent);
//                    }
//                    SelectableTextHelper.this.resetSelectionInfo();
//                    SelectableTextHelper.this.hideSelectView();
//                }
//            });
//            contentView.findViewById(R.id.tv_select_all).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    hideSelectView();
//                    selectText(0, mTextView.getText().length());
//                    isHide = false;
//                    showCursorHandle(mStartHandle);
//                    showCursorHandle(mEndHandle);
//                    mOperateWindow.show();
//                }
//            });
        }

        public void show() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();

            float startX = layout.getPrimaryHorizontal(mSelectionInfo.mStart) + mTempCoors[0] + mTextView.getPaddingLeft();

            int startLine = layout.getLineForOffset(mSelectionInfo.mStart);
            int posY = layout.getLineTop(startLine) + mTempCoors[1] - mHeight - 16;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mWindow.setElevation(8f);
            }

            float arrowMargin = mWidth * 0.15f;
            float posX = startX - arrowMargin;
            int screenWidth = TextLayoutUtil.getScreenWidth(mContext);
            if (posX + mWidth > screenWidth) {
                posX = screenWidth - mWidth;
            } else if (posX < 0) {
                posX = 0;
            }

            View contentView = mWindow.getContentView();
            View arrow = contentView.findViewById(R.id.arrow);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) arrow.getLayoutParams();

            params.gravity = Gravity.NO_GRAVITY;

            float leftMargin = startX - posX;
            if (leftMargin < arrowMargin) {
                leftMargin = arrowMargin;
            } else if (leftMargin > mWidth - arrowMargin) {
                leftMargin = mWidth - arrowMargin;
            }

            params.leftMargin = (int) (leftMargin + 0.5);

            arrow.setLayoutParams(params);

            mWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, (int) (posX + 0.5), Math.max(posY, 16));
        }

        public void dismiss() {
            mWindow.dismiss();
        }

        public boolean isShowing() {
            return mWindow.isShowing();
        }
    }

    private class CursorHandle extends View {

        private PopupWindow mPopupWindow;
        private Paint mPaint;

        private int mCircleRadius = mCursorHandleSize / 2;
        private int mWidth = mCircleRadius * 2;
        private int mHeight = mCircleRadius * 2;
        private int mPadding = 25;
        private boolean isLeft;

        public CursorHandle(boolean isLeft) {
            super(mContext);
            this.isLeft = isLeft;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(mCursorHandleColor);

            mPopupWindow = new PopupWindow(this);
            mPopupWindow.setClippingEnabled(false);
            mPopupWindow.setWidth(mWidth + mPadding * 2);
            mPopupWindow.setHeight(mHeight + mPadding / 2);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(mCircleRadius + mPadding, mCircleRadius, mCircleRadius, mPaint);
            if (isLeft) {
                canvas.drawRect(mCircleRadius + mPadding, 0, mCircleRadius * 2 + mPadding, mCircleRadius, mPaint);
            } else {
                canvas.drawRect(mPadding, 0, mCircleRadius + mPadding, mCircleRadius, mPaint);
            }
        }

        private int mAdjustX;
        private int mAdjustY;

        private int mBeforeDragStart;
        private int mBeforeDragEnd;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mBeforeDragStart = mSelectionInfo.mStart;
                    mBeforeDragEnd = mSelectionInfo.mEnd;
                    mAdjustX = (int) event.getX();
                    mAdjustY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mOperateWindow.show();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mOperateWindow.dismiss();
                    int rawX = (int) event.getRawX();
                    int rawY = (int) event.getRawY();
                    update(rawX + mAdjustX - mWidth, rawY + mAdjustY - mHeight);
                    break;
            }
            return true;
        }

        private void changeDirection() {
            isLeft = !isLeft;
            invalidate();
        }

        public void dismiss() {
            mPopupWindow.dismiss();
        }

        private int[] mTempCoors = new int[2];

        public void update(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int oldOffset;
            if (isLeft) {
                oldOffset = mSelectionInfo.mStart;
            } else {
                oldOffset = mSelectionInfo.mEnd;
            }

            y -= mTempCoors[1];

            int offset = TextLayoutUtil.getHysteresisOffset(mTextView, x, y, oldOffset);

            if (offset != oldOffset) {
                resetSelectionInfo();
                if (isLeft) {
                    if (offset > mBeforeDragEnd) {
                        CursorHandle handle = getCursorHandle(false);
                        changeDirection();
                        handle.changeDirection();
                        mBeforeDragStart = mBeforeDragEnd;
                        selectText(mBeforeDragEnd, offset);
                        handle.updateCursorHandle();
                    } else {
                        selectText(offset, -1);
                    }
                    updateCursorHandle();
                } else {
                    if (offset < mBeforeDragStart) {
                        CursorHandle handle = getCursorHandle(true);
                        handle.changeDirection();
                        changeDirection();
                        mBeforeDragEnd = mBeforeDragStart;
                        selectText(offset, mBeforeDragStart);
                        handle.updateCursorHandle();
                    } else {
                        selectText(mBeforeDragStart, offset);
                    }
                    updateCursorHandle();
                }
            }
        }

        private void updateCursorHandle() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            if (isLeft) {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.mStart) - mWidth + getExtraX(),
                        layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mStart)) + getExtraY(), -1, -1);
            } else {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.mEnd) + getExtraX(),
                        layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mEnd)) + getExtraY(), -1, -1);
            }
        }

        public void show(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int offset = isLeft ? mWidth : 0;
            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, x - offset + getExtraX(), y + getExtraY());
        }

        public int getExtraX() {
            return mTempCoors[0] - mPadding + mTextView.getPaddingLeft();
        }

        public int getExtraY() {
            Layout layout = mTextView.getLayout();
            int lineBottom = layout.getLineBottom(0) - layout.getPaint().getFontMetricsInt(metricsInt);
            return mTempCoors[1] + mTextView.getPaddingTop() - lineBottom;
        }
    }

    private CursorHandle getCursorHandle(boolean isLeft) {
        if (mStartHandle.isLeft == isLeft) {
            return mStartHandle;
        } else {
            return mEndHandle;
        }
    }

    public static class Builder {
        private TextView mTextView;
        private int mCursorHandleColor = 0xFF1379D6;
        private int mSelectedColor = 0xFFAFE1F4;
        private float mCursorHandleSizeInDp = 24;

        public Builder(TextView textView) {
            mTextView = textView;
        }

        public Builder setCursorHandleColor(@ColorInt int cursorHandleColor) {
            mCursorHandleColor = cursorHandleColor;
            return this;
        }

        public Builder setCursorHandleSizeInDp(float cursorHandleSizeInDp) {
            mCursorHandleSizeInDp = cursorHandleSizeInDp;
            return this;
        }

        public Builder setSelectedColor(@ColorInt int selectedBgColor) {
            mSelectedColor = selectedBgColor;
            return this;
        }

        public SelectableTextHelper build() {
            return new SelectableTextHelper(this);
        }
    }
}


