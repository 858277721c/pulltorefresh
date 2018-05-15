/*
 * Copyright (C) 2017 zhengjun, fanwe (http://www.fanwe.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fanwe.lib.pulltorefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Scroller;

import com.fanwe.lib.gesture.FGestureManager;
import com.fanwe.lib.gesture.FScroller;
import com.fanwe.lib.gesture.FTouchHelper;

public class FPullToRefreshView extends BasePullToRefreshView
{
    public FPullToRefreshView(Context context)
    {
        super(context);
        init(null);
    }

    public FPullToRefreshView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs);
    }

    public FPullToRefreshView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private FGestureManager mGestureManager;
    private FScroller mScroller;

    private void init(AttributeSet attrs)
    {
        initScroller();
        initGestureManager();
    }

    private void initScroller()
    {
        mScroller = new FScroller(new Scroller(getContext()));
        mScroller.setCallback(new FScroller.Callback()
        {
            @Override
            public void onScrollStateChanged(boolean isFinished)
            {
                if (isFinished)
                {
                    if (mIsDebug)
                    {
                        Log.e(getDebugTag(), "onScroll finished:" + " " + getState());
                    }

                    switch (getState())
                    {
                        case REFRESHING:
                            notifyRefreshCallback();
                            break;
                        case PULL_TO_REFRESH:
                        case FINISH:
                            setState(State.RESET);
                            break;
                    }
                }
            }

            @Override
            public void onScroll(int dx, int dy)
            {
                moveViews(dy, true);

                if (mIsDebug)
                {
                    int top = 0;
                    if (getDirection() == Direction.FROM_HEADER)
                    {
                        top = getHeaderView().getTop();
                    } else
                    {
                        top = getFooterView().getTop();
                    }
                    Log.i(getDebugTag(), "onScroll:" + top + " " + getState());
                }
            }
        });
    }

    private void initGestureManager()
    {
        mGestureManager = new FGestureManager(new FGestureManager.Callback()
        {
            @Override
            public boolean shouldInterceptTouchEvent(MotionEvent event)
            {
                final boolean canPull = canPull();
                if (mIsDebug)
                {
                    Log.i(getDebugTag(), "shouldInterceptTouchEvent:" + canPull);
                }
                return canPull;
            }

            @Override
            public void onTagInterceptChanged(boolean tagIntercept)
            {
                FTouchHelper.requestDisallowInterceptTouchEvent(FPullToRefreshView.this, tagIntercept);
            }

            @Override
            public boolean shouldConsumeTouchEvent(MotionEvent event)
            {
                final boolean canPull = canPull();
                if (mIsDebug)
                {
                    Log.i(getDebugTag(), "shouldConsumeTouchEvent:" + canPull);
                }
                return canPull;
            }

            @Override
            public void onTagConsumeChanged(boolean tagConsume)
            {
                FTouchHelper.requestDisallowInterceptTouchEvent(FPullToRefreshView.this, tagConsume);
            }

            @Override
            public boolean onConsumeEvent(MotionEvent event)
            {
                saveDirectionWhenMove();

                final int dy = (int) mGestureManager.getTouchHelper().getDeltaYFrom(FTouchHelper.EVENT_LAST);
                final int dyConsume = getComsumedDistance(dy);
                moveViews(dyConsume, true);

                updateStateByMoveDistance();
                return true;
            }

            @Override
            public void onConsumeEventFinish(MotionEvent event, VelocityTracker velocityTracker)
            {
                if (mIsDebug)
                {
                    Log.e(getDebugTag(), "onConsumeEventFinish:" + event.getAction());
                }

                if (getState() == State.RELEASE_TO_REFRESH)
                {
                    setState(State.REFRESHING);
                }
                flingViewByState();
            }
        });
    }

    private void saveDirectionWhenMove()
    {
        if (mGestureManager.getTouchHelper().isMoveBottomFrom(FTouchHelper.EVENT_DOWN))
        {
            setDirection(Direction.FROM_HEADER);
            mScroller.setMaxScrollDistance(getHeaderView().getHeight());
        } else if (mGestureManager.getTouchHelper().isMoveTopFrom(FTouchHelper.EVENT_DOWN))
        {
            setDirection(Direction.FROM_FOOTER);
            mScroller.setMaxScrollDistance(getFooterView().getHeight());
        }
    }

    private boolean canPull()
    {
        final boolean checkDegree = mGestureManager.getTouchHelper().getDegreeYFrom(FTouchHelper.EVENT_DOWN) < 40;
        final boolean checkPull = canPullFromHeader() || canPullFromFooter();
        final boolean checkState = getState() == State.RESET;

        return checkDegree && checkPull && checkState;
    }

    private boolean canPullFromHeader()
    {
        final boolean checkIsMoveBottom = mGestureManager.getTouchHelper().isMoveBottomFrom(FTouchHelper.EVENT_DOWN);
        final boolean checkMode = getMode() == Mode.BOTH || getMode() == Mode.PULL_FROM_HEADER;
        final boolean checkIsScrollToTop = FTouchHelper.isScrollToTop(getRefreshView());
        final boolean checkPullCondition = checkPullConditionHeader();

        return checkIsMoveBottom && checkMode && checkIsScrollToTop && checkPullCondition;
    }

    private boolean canPullFromFooter()
    {
        final boolean checkIsMoveTop = mGestureManager.getTouchHelper().isMoveTopFrom(FTouchHelper.EVENT_DOWN);
        final boolean checkMode = getMode() == Mode.BOTH || getMode() == Mode.PULL_FROM_FOOTER;
        final boolean checkIsScrollToBottom = FTouchHelper.isScrollToBottom(getRefreshView());
        final boolean checkPullCondition = checkPullConditionFooter();

        return checkIsMoveTop && checkMode && checkIsScrollToBottom && checkPullCondition;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        return mGestureManager.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return mGestureManager.onTouchEvent(event);
    }

    @Override
    public void computeScroll()
    {
        if (mScroller.computeScrollOffset())
        {
            invalidate();
        }
    }

    @Override
    protected boolean isViewIdle()
    {
        return mScroller.isFinished() && !mGestureManager.isTagConsume();
    }

    @Override
    protected void flingViewByState()
    {
        int endY = 0;
        View view = null;

        boolean isScrollViewStarted = false;
        switch (getState())
        {
            case RESET:
            case PULL_TO_REFRESH:
            case FINISH:
                if (getDirection() == Direction.FROM_HEADER)
                {
                    view = getHeaderView();
                    endY = getTopHeaderViewReset();
                } else
                {
                    view = getFooterView();
                    endY = getTopFooterViewReset();
                }

                if (mScroller.scrollToY(view.getTop(), endY, -1))
                {
                    if (mIsDebug)
                    {
                        Log.i(getDebugTag(), "flingViewByState:" + view.getTop() + " -> " + endY + " " + getState());
                    }

                    isScrollViewStarted = true;
                    invalidate();
                }
                break;
            case RELEASE_TO_REFRESH:
            case REFRESHING:
                if (getDirection() == Direction.FROM_HEADER)
                {
                    view = getHeaderView();
                    endY = getTopHeaderViewReset() + getHeaderView().getRefreshHeight();
                } else
                {
                    view = getFooterView();
                    endY = getTopFooterViewReset() - getFooterView().getRefreshHeight();
                }

                if (mScroller.scrollToY(view.getTop(), endY, -1))
                {
                    if (mIsDebug)
                    {
                        Log.i(getDebugTag(), "flingViewByState:" + view.getTop() + " -> " + endY + " " + getState());
                    }

                    isScrollViewStarted = true;
                    invalidate();
                }
                break;
        }

        //通知刷新回调
        if (getState() == State.REFRESHING)
        {
            if (isScrollViewStarted)
            {
                //如果滚动触发成功，则滚动结束会通知刷新回调
            } else
            {
                //如果滚动未触发成功，则立即通知刷新回调
                notifyRefreshCallback();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        mScroller.abortAnimation();
    }
}
