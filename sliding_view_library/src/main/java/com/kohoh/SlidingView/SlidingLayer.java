package com.kohoh.SlidingView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;
import com.kohoh.util.GestureUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SlidingLayer extends FrameLayout {
    private static final boolean DEBUG = false;
    private static final int MAX_SCROLLING_DURATION = 600; // in ms
    private static final int MIN_DISTANCE_FOR_FLING = 25; // in dip

    private static final Interpolator sMenuInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return (float) Math.pow(t, 5) + 1.0f;
        }
    };
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mFlingDistance;
    private PositionManager mPositionManager;
    private Scroller mScroller;
    private boolean mEnableDrag;
    private boolean isSwitching;
    private boolean mEnableSlide;
    private boolean isDragging;
    private boolean mEnableInterceptAllGesture;
    private boolean mDrawingCacheEnabled;
    private VelocityTracker mVelocityTracker;
    private static final int INVALID_POINTER = -1;
    private int mActivePointerId = INVALID_POINTER;
    private int mInitialScrollX;
    private int mInitialScrollY;
    private float mLastX;
    private float mLastY;
    private int leftSlideBound;
    private int topSlideBound;
    private int rightSlideBound;
    private int bottomSlideBound;

    public SlidingLayer(Context context) {
        this(context, null);
    }

    public SlidingLayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor for the sliding layer.<br>
     * By default this panel will
     * <ol>
     * <p/>
     * <li>Use no shadow drawable. (i.e. with width of 0)</li>
     * <li>Close when the panel is tapped</li>
     * <li>Open when the offset is tapped, but will have an offset of 0</li>
     * </ol>
     *
     * @param context  a reference to an existing context
     * @param attrs    attribute set constructed from attributes set in android .xml file
     * @param defStyle style res id
     */
    public SlidingLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Style
        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingLayer);

        int initialX = (int) ta.getDimension(R.styleable.SlidingLayer_initialX, 0);
        int initialY = (int) ta.getDimension(R.styleable.SlidingLayer_initialY, 0);
        leftSlideBound = (int) ta.getDimension(R.styleable.SlidingLayer_leftSlideBound, initialX);
        rightSlideBound = (int) ta.getDimension(R.styleable.SlidingLayer_rightSlideBound, initialX);
        topSlideBound = (int) ta.getDimension(R.styleable.SlidingLayer_topSlideBound, initialY);
        bottomSlideBound = (int) ta.getDimension(R.styleable.SlidingLayer_bottomSlideBound, initialY);

        mEnableDrag = ta.getBoolean(R.styleable.SlidingLayer_dragEnable, true);
        mEnableSlide = ta.getBoolean(R.styleable.SlidingLayer_slideEnable, true);
        mEnableInterceptAllGesture = ta.getBoolean(R.styleable.SlidingLayer_interceptAllGestureEnable, false);

        mPositionManager = new PositionManager(new Coordinate(initialX, initialY));
        ta.recycle();

        init();
    }

    //TODO 需要研究
    private void init() {
        setWillNotDraw(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(true);
        final Context context = getContext();
        mScroller = new Scroller(context, sMenuInterpolator);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        final float density = context.getResources().getDisplayMetrics().density;
        mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);

    }

    public boolean isSlideEnabled() {
        return mEnableSlide;
    }

    public void setSlideEnabled(boolean _enabled) {
        mEnableSlide = _enabled;
    }

    public boolean isDragEnable() {
        return mEnableDrag;
    }

    public void setDragEnable(boolean mEnableDrag) {
        this.mEnableDrag = mEnableDrag;
    }

    public boolean isInterceptAllGestureEnable() {
        return mEnableInterceptAllGesture;
    }

    public void setInterceptAllGestureEnable(boolean mEnableForceDrag) {
        this.mEnableInterceptAllGesture = mEnableForceDrag;
    }

    public void addPosition(Integer positionId, int x, int y) {

        mPositionManager.addPosition(positionId, new Coordinate(x, y));
    }

    public void setLeftSlideBound(int leftSlideBound) {
        this.leftSlideBound = leftSlideBound;
    }

    public void setRightSlideBound(int rightSlideBound) {
        this.rightSlideBound = rightSlideBound;
    }

    public void setTopSlideBound(int topSlideBound) {
        this.topSlideBound = topSlideBound;
    }

    public void setBottomSlideBound(int bottomSlideBound) {
        this.bottomSlideBound = bottomSlideBound;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) {
            GestureUtil.printEvent(ev, "onInterceptTouchEvent");
        }

        int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (isSwitching) {
                    stopSwitch();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isDragging && !mPositionManager.isAtPosition(getScrollX(), getScrollY())) {
                    int position = mPositionManager.guessPosition(getScrollX(), getScrollY());
                    switchPosition(position, true, false, 0);
                }
                break;
        }

        if (mEnableInterceptAllGesture) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEnableSlide || !mEnableDrag) {
            return false;
        }

        if (!isReadyToDrag(event)) {
            return false;
        }

        if (DEBUG) {
            GestureUtil.printEvent(event, "onTouchEvent");
            Log.v("onTouchEvent", "ActionPointerId = " + mActivePointerId);
        }

        final int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                int index = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                float currentX = MotionEventCompat.getX(event, index);
                float currentY = MotionEventCompat.getY(event, index);
                final float deltaX = mLastX - currentX;
                final float deltaY = mLastY - currentY;
                if (!isDragging) {
                    if (Math.abs(deltaX) >= mTouchSlop || Math.abs(deltaY) >= mTouchSlop) {
                        isDragging = true;
                    }
                }
                if (isDragging) {
                    mLastX = currentX;
                    mLastY = currentY;
                    float scrollX = getScrollX() + deltaX;
                    float scrollY = getScrollY() + deltaY;
                    scrollX = Math.min(scrollX, Math.max(leftSlideBound, mPositionManager.leftBound));
                    scrollX = Math.max(scrollX, Math.min(rightSlideBound, mPositionManager.rightBound));
                    scrollY = Math.min(scrollY, Math.max(topSlideBound, mPositionManager.topBound));
                    scrollY = Math.max(scrollY, Math.min(bottomSlideBound, mPositionManager.bottomBound));
                    scrollTo((int) scrollX, (int) scrollY);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    final int initialVelocityX = (int) VelocityTrackerCompat.getXVelocity(velocityTracker,
                            0);
                    final int initialVelocityY = (int) VelocityTrackerCompat.getYVelocity(velocityTracker,
                            0);
                    final int scrollX = getScrollX();
                    final int scrollY = getScrollY();

                    int position = mPositionManager.determineNextPosition(initialVelocityX, initialVelocityY
                            , mInitialScrollX, mInitialScrollY, scrollX, scrollY);
                    switchPosition(position, true, true, initialVelocityX, initialVelocityY);
                    endDrag();
                }
                else if(!mPositionManager.isAtPosition(getScrollX(),getScrollY()))
                {
                    int poition = mPositionManager.guessPosition(getScrollX(),getScrollY());
                    switchPosition(poition,true,false,0);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                switchPosition(mPositionManager.currentPosition, true, true, 0);
                endDrag();
                return true;
            default:
                return false;
        }
    }

    private void endDrag() {
        isDragging = false;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        mActivePointerId = INVALID_POINTER;
    }

    private boolean isReadyToDrag(MotionEvent event) {
        boolean result = true;

        if (MotionEventCompat.findPointerIndex(event, mActivePointerId) == INVALID_POINTER) {
            result = false;
            int count = MotionEventCompat.getPointerCount(event);
            for (int i = 0; i < count; i++) {
                float currentX = MotionEventCompat.getX(event, i);
                float currentY = MotionEventCompat.getY(event, i);
                if (allowSlidingFromHere(currentX, currentY)) {
                    if (mVelocityTracker != null) {
                        mVelocityTracker.clear();
                    } else {
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    mVelocityTracker.addMovement(event);
                    mActivePointerId = MotionEventCompat.getPointerId(event, i);
                    mInitialScrollX = getScrollX();
                    mInitialScrollY = getScrollY();
                    mLastX = currentX;
                    mLastY = currentY;
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Like {@link android.view.View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     */
    void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, 0);
    }

    /**
     * Like {@link android.view.View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x        the number of pixels to scroll by on the X axis
     * @param y        the number of pixels to scroll by on the Y axis
     * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
     */
    void smoothScrollTo(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            return;
        }
        int sx = getScrollX();
        int sy = getScrollY();
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            return;
        }

        final int width = getWidth();
        final int halfWidth = width / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
        final float distance = halfWidth + halfWidth * distanceInfluenceForSnapDuration(distanceRatio);

        int duration = 0;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            duration = MAX_SCROLLING_DURATION;
        }
        duration = Math.min(duration, MAX_SCROLLING_DURATION);

        mScroller.startScroll(sx, sy, dx, dy, duration);
        invalidate();
    }

    //    We want the duration of the page snap animation to be influenced by the distance that
//    the screen has to travel, however, we don't want this duration to be effected in a
//    purely linear fashion. Instead, we use this method to moderate the effect that the distance
//    of travel has on the overall snap duration.
    float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return FloatMath.sin(f);
    }

    /**
     * 判断是否可以从该处移动
     *
     * @param initialY
     * @param initialX
     * @return
     */
    private boolean allowSlidingFromHere(final float initialX, final float initialY) {
        boolean allow = true;
        if (initialX > (getRight() - getScrollX())) {
            allow = false;
        }
        if (initialX < (getLeft() - getScrollX())) {
            allow = false;
        }
        if (initialY < (getTop() - getScrollY())) {
            allow = false;
        }
        if (initialY > (getBottom() - getScrollY())) {
            allow = false;
        }
        return allow;
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished()) {
            if(!isSwitching)
            {
                isSwitching=true;
            }
            if (mScroller.computeScrollOffset()) {
                final int oldX = getScrollX();
                final int oldY = getScrollY();
                final int x = mScroller.getCurrX();
                final int y = mScroller.getCurrY();

                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                }

                // We invalidate a slightly larger area now, this was only optimised for right menu previously
                // Keep on drawing until the animation has finished. Just re-draw the necessary part
                invalidate(getLeft() + oldX, getTop() + oldY, getRight() - oldX, getBottom() - oldY);
            }
        }
        else
        {
            if(isSwitching)
            {
                isSwitching=false;
            }
        }
    }

    private void stopSwitch() {
        if (isSwitching) {
            mScroller.abortAnimation();
        }
    }

    private void completeSwitch() {
        if (isSwitching) {
            mScroller.abortAnimation();
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            if (oldX != x || oldY != y) {
                scrollTo(x, y);
            }
        }
    }

    public void switchPosition(Integer position) {
        switchPosition(position, true, true, 0);
    }

    /**
     * @param targetPosition 目标位置
     * @param smoothAnim     是否使用动画效果
     * @param forceSwitch    是否强化切换位置
     * @param velocity       切换的速率
     */
    public void switchPosition(final int targetPosition, final boolean smoothAnim, final boolean forceSwitch,
                               final int velocity) {

        if (!mEnableSlide) {
            return;
        }

        Coordinate targetCoordinate = mPositionManager.getCoordinate(targetPosition);
        Integer currentPosition = mPositionManager.currentPosition;

        if (targetCoordinate == null) {
            return;
        }

        if (!forceSwitch && targetPosition == currentPosition) {
            return;
        }

        mPositionManager.currentPosition = targetPosition;
        completeSwitch();
        if (smoothAnim) {
            smoothScrollTo(targetCoordinate.x, targetCoordinate.y, velocity);
        } else {
            scrollTo(targetCoordinate.x, targetCoordinate.y);
        }
    }

    public void switchPosition(final int targetPosition, final boolean smoothAnim, final boolean forceSwitch,
                               final int velocityX, final int velocityY) {
        int velocity = (int) Math.sqrt((velocityX * velocityX) + (velocityY * velocityY));
        switchPosition(targetPosition, smoothAnim, forceSwitch, velocity);
    }

    @Override
    public void setDrawingCacheEnabled(boolean enabled) {

        if (mDrawingCacheEnabled != enabled) {
            super.setDrawingCacheEnabled(enabled);
            mDrawingCacheEnabled = enabled;

            final int l = getChildCount();
            for (int i = 0; i < l; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    child.setDrawingCacheEnabled(enabled);
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Make sure scroll position is set correctly.
        if (w != oldw) {
            switchPosition(mPositionManager.currentPosition, false, true, 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if((isDragging||isSwitching)&&!mDrawingCacheEnabled)
        {
            setDrawingCacheEnabled(true);
        }
        super.onDraw(canvas);
        if (!isDragging && !isSwitching&&mDrawingCacheEnabled) {
            setDrawingCacheEnabled(false);
        }
    }

    class Coordinate {
        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x;
        public int y;
    }

    class PositionManager {

        public PositionManager() {
            this(new Coordinate(0, 0));
        }

        public PositionManager(Coordinate initialCoordinate) {
            coordinateMap = new HashMap<Integer, Coordinate>();
            addPosition(POSITION_INITIAL, initialCoordinate);
        }

        public static final int POSITION_INITIAL = -1;
        private Map<Integer, Coordinate> coordinateMap;
        private int leftBound = Integer.MIN_VALUE;
        private int topBound = Integer.MIN_VALUE;
        private int rightBound = Integer.MAX_VALUE;
        private int bottomBound = Integer.MAX_VALUE;
        private Integer currentPosition = POSITION_INITIAL;

        public boolean addPosition(Integer positionId, Coordinate coordinate) {
            boolean result = true;

            if (coordinateMap.containsKey(positionId)) {
                result = false;
            } else if (coordinateMap.put(positionId, coordinate) == null) {
                result = false;
            }

            setBound();

            return result;
        }

        public boolean removePosition(Integer positionId) {

            boolean result = true;
            if (coordinateMap.remove(positionId) == null) {
                result = false;
            } else {
                setBound();
            }

            return result;
        }

        private void setBound() {
            leftBound = Integer.MIN_VALUE;
            topBound = Integer.MIN_VALUE;
            rightBound = Integer.MAX_VALUE;
            bottomBound = Integer.MAX_VALUE;

            Iterator iterator = coordinateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Coordinate> entry = (Map.Entry<Integer, Coordinate>) iterator.next();
                Coordinate coordinate = entry.getValue();

                leftBound = Math.max(leftBound, coordinate.x);
                rightBound = Math.min(rightBound, coordinate.x);
                topBound = Math.max(topBound, coordinate.y);
                bottomBound = Math.min(bottomBound, coordinate.y);
            }
        }

        public Coordinate getCoordinate(Integer positionId) {
            return coordinateMap.get(positionId);
        }

        public float computeDistance(Coordinate p1, Coordinate p2) {
            return (float) Math.sqrt(Math.pow((p1.x - p2.x), 2) + Math.pow((p1.y - p2.y), 2));
        }

        public boolean isAtPosition(int currentX, int currentY) {
            Iterator iterator = coordinateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Coordinate> entry = (Map.Entry<Integer, Coordinate>) iterator.next();
                Coordinate targetCoordinate = entry.getValue();
                if (targetCoordinate.x == currentX && targetCoordinate.y == currentY) {
                    return true;
                }
            }
            return false;
        }

        public int guessPosition(int currentX, int currentY) {
            Coordinate currentCorrdinate = new Coordinate(currentX, currentY);
            float min = Float.MAX_VALUE;
            Iterator iterator = coordinateMap.entrySet().iterator();
            int targetPosition = POSITION_INITIAL;
            while (iterator.hasNext()) {
                Map.Entry<Integer, Coordinate> entry = (Map.Entry<Integer, Coordinate>) iterator.next();
                Coordinate targetCoordinate = entry.getValue();
                float distance = computeDistance(targetCoordinate, currentCorrdinate);
                if (distance < min) {
                    min = distance;
                    targetPosition = entry.getKey();
                }
            }
            return targetPosition;
        }

        public int guessPosition(final int initialX, final int initialY
                , final int currentX, final int currentY) {
            Coordinate pi = new Coordinate(initialX, initialY);
            Coordinate pc = new Coordinate(currentX, currentY);
            float dic = computeDistance(pi, pc);
            float maxCos = (float) Integer.MIN_VALUE;
            float minDis = (float) Integer.MAX_VALUE;

            Integer desirePositionId = POSITION_INITIAL;

            Iterator iterator = coordinateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Coordinate> entry = (Map.Entry<Integer, Coordinate>) iterator.next();
                if (currentPosition == entry.getKey()) {
                    continue;
                }
                Coordinate pd = entry.getValue();
                float did = computeDistance(pi, pd);
                float dcd = computeDistance(pc, pd);
                float cosa = ((dic * dic) + (did * did) - (dcd * dcd)) / (2 * dic * did);
                if (cosa > maxCos) {
                    maxCos = cosa;
                    desirePositionId = entry.getKey();
                }
                if (cosa == maxCos && dcd < minDis) {
                    minDis = dcd;
                    desirePositionId = entry.getKey();
                }
            }
            return desirePositionId;
        }

        public int determineNextPosition(float velocityX, float velocityY
                , int initialX, int initialY
                , int currentX, int currentY) {

            int nextPosition = currentPosition;
            int desirePositionId = guessPosition(initialX, initialY, currentX, currentY);
            Coordinate desireP = coordinateMap.get(desirePositionId);
            Coordinate initialP = new Coordinate(initialX, initialY);
            Coordinate currentP = new Coordinate(currentX, currentY);

            float currentDistance = computeDistance(initialP, currentP);
            float totalDistance = computeDistance(initialP, desireP);
            float velocity = (float) Math.sqrt((velocityX * velocityX) + (velocityY * velocityY));

            if (currentDistance > mFlingDistance && velocity > mMinimumVelocity && velocity != 0) {
                nextPosition = desirePositionId;
            } else {
                if (Math.round(currentDistance / totalDistance) >= 1) {
                    nextPosition = desirePositionId;
                }
            }
            return nextPosition;
        }

    }

}
