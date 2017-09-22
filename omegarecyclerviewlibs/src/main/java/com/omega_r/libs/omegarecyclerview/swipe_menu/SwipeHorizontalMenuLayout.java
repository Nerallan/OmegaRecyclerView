package com.omega_r.libs.omegarecyclerview.swipe_menu;


import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewParent;

import com.omega_r.libs.omegarecyclerview.R;
import com.omega_r.libs.omegarecyclerview.swipe_menu.swiper.LeftHorizontalSwiper;
import com.omega_r.libs.omegarecyclerview.swipe_menu.swiper.RightHorizontalSwiper;
import com.omega_r.libs.omegarecyclerview.swipe_menu.swiper.Swiper;

public class SwipeHorizontalMenuLayout extends SwipeMenuLayout {

    protected int mPreScrollX;

    protected float mPreLeftMenuFraction = -1;
    protected float mPreRightMenuFraction = -1;

    public SwipeHorizontalMenuLayout(Context context) {
        super(context);
    }

    public SwipeHorizontalMenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeHorizontalMenuLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean isIntercepted = super.onInterceptTouchEvent(ev);
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = mLastX = (int) ev.getX();
                mDownY = (int) ev.getY();
                isIntercepted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int disX = (int) (ev.getX() - mDownX);
                int disY = (int) (ev.getY() - mDownY);
                isIntercepted = Math.abs(disX) > mScaledTouchSlop && Math.abs(disX) > Math.abs(disY);
                break;
            case MotionEvent.ACTION_UP:
                isIntercepted = false;

                if (isMenuOpen() && mCurrentSwiper.isClickOnContentView(this, ev.getX())) {
                    smoothCloseMenu();
                    isIntercepted = true;
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                isIntercepted = false;

                if (!mScroller.isFinished()) mScroller.forceFinished(false);

                break;
        }
        return isIntercepted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();

        mVelocityTracker.addMovement(ev);

        int dx;
        int dy;
        int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = (int) ev.getX();
                mLastY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isSwipeEnable()) break;

                int disX = (int) (mLastX - ev.getX());
                int disY = (int) (mLastY - ev.getY());

                if (!mDragging && Math.abs(disX) > mScaledTouchSlop && Math.abs(disX) > Math.abs(disY)) {
                    ViewParent parent = getParent();

                    if(parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }

                    mDragging = true;
                }

                if (mDragging) {
                    if (mCurrentSwiper == null || shouldResetSwiper) {
                        if (disX < 0) {
                            if (mBeginSwiper != null) mCurrentSwiper = mBeginSwiper;
                            else mCurrentSwiper = mEndSwiper;
                        } else {
                            if (mEndSwiper != null) mCurrentSwiper = mEndSwiper;
                            else mCurrentSwiper = mBeginSwiper;
                        }
                    }
                    scrollBy(disX, 0);
                    mLastX = (int) ev.getX();
                    mLastY = (int) ev.getY();
                    shouldResetSwiper = false;
                }

                break;
            case MotionEvent.ACTION_UP:
                ViewParent parent = getParent();

                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }

                dx = (int) (mDownX - ev.getX());
                dy = (int) (mDownY - ev.getY());
                mDragging = false;
                mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
                int velocityX = (int) mVelocityTracker.getXVelocity();
                int velocity = Math.abs(velocityX);

                if (velocity > mScaledMinimumFlingVelocity) {
                    if (mCurrentSwiper != null) {
                        int duration = SCROLLER_DURATION;
                        if (mCurrentSwiper instanceof RightHorizontalSwiper) {
                            if (velocityX < 0) smoothOpenMenu(duration);
                            else smoothCloseMenu(duration);
                        } else {
                            if (velocityX > 0) smoothOpenMenu(duration);
                            else smoothCloseMenu(duration);
                        }
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                } else {
                    judgeOpenClose(dx, dy);
                }

                mVelocityTracker.clear();
                mVelocityTracker.recycle();
                mVelocityTracker = null;

                if (Math.abs(dx) > mScaledTouchSlop
                        || Math.abs(dy) > mScaledTouchSlop
                        || isMenuOpen()) {
                    MotionEvent motionEvent = MotionEvent.obtain(ev);
                    motionEvent.setAction(MotionEvent.ACTION_CANCEL);
                    return super.onTouchEvent(motionEvent);
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;

                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(false);
                } else {
                    dx = (int) (mDownX - ev.getX());
                    dy = (int) (mDownY - ev.getY());
                    judgeOpenClose(dx, dy);
                }

                break;
        }
        return super.onTouchEvent(ev);
    }

    private void judgeOpenClose(int dx, int dy) {
        if (mCurrentSwiper != null) {
            if (Math.abs(getScrollX()) >= (mCurrentSwiper.getMenuView().getWidth() * AUTO_OPEN_PERCENT)) {
                if (Math.abs(dx) > mScaledTouchSlop || Math.abs(dy) > mScaledTouchSlop) {
                    if (isMenuOpenNotEqual()) smoothCloseMenu();
                    else smoothOpenMenu();
                } else {
                    if (isMenuOpen()) smoothCloseMenu();
                    else smoothOpenMenu();
                }
            } else {
                smoothCloseMenu();
            }
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        Swiper.Checker checker = mCurrentSwiper.checkXY(x, y);
        shouldResetSwiper = checker.shouldResetSwiper;

        if (checker.x != getScrollX()) {
            super.scrollTo(checker.x, checker.y);
        }

        if (getScrollX() != mPreScrollX) {
            int absScrollX = Math.abs(getScrollX());
            if (mCurrentSwiper instanceof LeftHorizontalSwiper) {
                if (mSwipeSwitchListener != null) {
                    if (absScrollX == 0) {
                        mSwipeSwitchListener.beginMenuClosed(this);
                    }
                    else if (absScrollX == mBeginSwiper.getMenuWidth()) {
                        mSwipeSwitchListener.beginMenuOpened(this);
                    }
                }
                if (mSwipeFractionListener != null) {
                    float fraction = (float) absScrollX / mBeginSwiper.getMenuWidth();
                    fraction = Float.parseFloat(mDecimalFormat.format(fraction));

                    if (fraction != mPreLeftMenuFraction) {
                        mSwipeFractionListener.beginMenuSwipeFraction(this, fraction);
                    }

                    mPreLeftMenuFraction = fraction;
                }
            } else {
                if (mSwipeSwitchListener != null) {
                    if (absScrollX == 0) {
                        mSwipeSwitchListener.endMenuClosed(this);
                    }
                    else if (absScrollX == mEndSwiper.getMenuWidth()) {
                        mSwipeSwitchListener.endMenuOpened(this);
                    }
                }
                if (mSwipeFractionListener != null) {
                    float fraction = (float) absScrollX / mEndSwiper.getMenuWidth();
                    fraction = Float.parseFloat(mDecimalFormat.format(fraction));

                    if (fraction != mPreRightMenuFraction) {
                        mSwipeFractionListener.endMenuSwipeFraction(this, fraction);
                    }

                    mPreRightMenuFraction = fraction;
                }
            }
        }
        mPreScrollX = getScrollX();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int currX = Math.abs(mScroller.getCurrX());

            if (mCurrentSwiper instanceof RightHorizontalSwiper) {
                scrollTo(currX, 0);
                invalidate();
            } else {
                scrollTo(-currX, 0);
                invalidate();
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setClickable(true);
        mContentView = findViewById(R.id.ContentView);

        if (mContentView == null) {
            throw new IllegalArgumentException("Not find contentView by id ContentView");
        }

        View menuViewLeft = findViewById(R.id.MenuViewLeft);
        View menuViewRight = findViewById(R.id.MenuViewRight);

        if (menuViewLeft == null && menuViewRight == null) {
            throw new IllegalArgumentException("Not find menuView by id (MenuViewLeft, MenuViewRight)");
        }

        if (menuViewLeft != null) mBeginSwiper = new LeftHorizontalSwiper(menuViewLeft);

        if (menuViewRight != null) mEndSwiper = new RightHorizontalSwiper(menuViewRight);
    }

    public boolean isMenuOpen() {
        return (mBeginSwiper != null && mBeginSwiper.isMenuOpen(getScrollX()))
                || (mEndSwiper != null && mEndSwiper.isMenuOpen(getScrollX()));
    }

    public boolean isMenuOpenNotEqual() {
        return (mBeginSwiper != null && mBeginSwiper.isMenuOpenNotEqual(getScrollX()))
                || (mEndSwiper != null && mEndSwiper.isMenuOpenNotEqual(getScrollX()));
    }

    public void smoothOpenMenu(int duration) {
        mCurrentSwiper.autoOpenMenu(mScroller, getScrollX(), duration);
        invalidate();
    }

    public void smoothCloseMenu(int duration) {
        mCurrentSwiper.autoCloseMenu(mScroller, getScrollX(), duration);
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int parentViewWidth = getMeasuredWidthAndState();
        int contentViewWidth = getMeasuredWidthAndState();
        int contentViewHeight = getMeasuredHeightAndState();
        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
        int lGap = getPaddingLeft() + lp.leftMargin;
        int tGap = getPaddingTop() + lp.topMargin;

        mContentView.layout(lGap,
                tGap,
                lGap + contentViewWidth,
                tGap + contentViewHeight);

        if (mEndSwiper != null) {
            int menuViewWidth = getMeasuredWidthAndState();
            int menuViewHeight = getMeasuredHeightAndState();
            lp = (LayoutParams) mEndSwiper.getMenuView().getLayoutParams();
            tGap = getPaddingTop() + lp.topMargin;
            mEndSwiper.getMenuView().layout(parentViewWidth,
                    tGap,
                    parentViewWidth + menuViewWidth,
                    tGap + menuViewHeight);
        }

        if (mBeginSwiper != null) {
            int menuViewWidth = getMeasuredWidthAndState();
            int menuViewHeight = getMeasuredHeightAndState();
            lp = (LayoutParams) mBeginSwiper.getMenuView().getLayoutParams();
            tGap = getPaddingTop() + lp.topMargin;
            mBeginSwiper.getMenuView().layout(-menuViewWidth, tGap, 0, tGap + menuViewHeight);
        }
    }

    public boolean isSwipeEnable() {
        return swipeEnable;
    }
}
