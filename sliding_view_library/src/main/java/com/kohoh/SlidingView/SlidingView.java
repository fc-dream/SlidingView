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

public class SlidingView extends FrameLayout {
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

    /**
     * 构建一个SlidingView。<br>
     * 这会生成一个的默认样式如下的SlidingView
     * <ol>
     * <p/>
     * <li>允许滑动</li>
     * <li>允许拖拽</li>
     * <li>不允许强制拦截手势</li>
     * <li>初始位置为[0,0]</li>
     * <li>滑动范围被限制在距离初始位置周围0个坐标范围内</li>
     * </ol>
     *
     * @param context 想关联的Context
     */
    public SlidingView(Context context) {
        this(context, null);
    }

    /**
     * 构建一个SlidingView,并设置他的初始位置<br>
     * 这会生成一个的默认样式如下的SlidingView
     * <ol>
     * <p/>
     * <li>允许滑动</li>
     * <li>允许拖拽</li>
     * <li>不允许强制拦截手势</li>
     * <li>滑动范围被限制在距离初始位置周围0个坐标范围内</li>
     * </ol>
     *
     * @param context  想关联的Context
     * @param initialX 初始位置的x轴坐标
     * @param initialY 初始位置的y轴坐标
     */
    public SlidingView(Context context, int initialX, int initialY) {
        this(context, null);
        if (mPositionManager.removePosition(mPositionManager.POSITION_INITIAL)) {
            mPositionManager.addPosition(mPositionManager.POSITION_INITIAL, new Coordinate(initialX, initialY));
        }
    }

    /**
     * 构建一个SlidingView，一般当你在.xml中声明该类时，系统会调用这个构建函数。<br>
     * 如果你没有设置额外的属性，那么他的默认样式是
     * <ol>
     * <p/>
     * <li>允许滑动</li>
     * <li>允许拖拽</li>
     * <li>不允许强制拦截手势</li>
     * <li>初始位置为[0,0]</li>
     * <li>滑动范围被限制在距离初始位置周围0个坐标范围内</li>
     * </ol>
     *
     * @param context 想关联的Context
     * @param attrs   从.xml文件中获取的和布局相关的属性集合
     */
    public SlidingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * 构建一个SlidingView，一般当你在.xml中声明该类时，系统会调用这个构建函数。<br>
     * 如果你没有设置额外的属性，那么他的默认样式是
     * <ol>
     * <p/>
     * <li>允许滑动</li>
     * <li>允许拖拽</li>
     * <li>不允许强制拦截手势</li>
     * <li>初始位置为[0,0]</li>
     * <li>滑动范围被限制在距离初始位置周围0个坐标范围内</li>
     * </ol>
     *
     * @param context  想关联的Context
     * @param attrs    从.xml文件中获取的和布局相关的属性集合
     * @param defStyle 默认的设计样式的Id
     */
    public SlidingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingView);
        int initialX = (int) ta.getDimension(R.styleable.SlidingView_initialX, 0);
        int initialY = (int) ta.getDimension(R.styleable.SlidingView_initialY, 0);
        leftSlideBound = (int) ta.getDimension(R.styleable.SlidingView_leftSlideBound, initialX);
        rightSlideBound = (int) ta.getDimension(R.styleable.SlidingView_rightSlideBound, initialX);
        topSlideBound = (int) ta.getDimension(R.styleable.SlidingView_topSlideBound, initialY);
        bottomSlideBound = (int) ta.getDimension(R.styleable.SlidingView_bottomSlideBound, initialY);
        mEnableDrag = ta.getBoolean(R.styleable.SlidingView_dragEnable, true);
        mEnableSlide = ta.getBoolean(R.styleable.SlidingView_slideEnable, true);
        mEnableInterceptAllGesture = ta.getBoolean(R.styleable.SlidingView_interceptAllGestureEnable, false);
        mPositionManager = new PositionManager(new Coordinate(initialX, initialY));
        ta.recycle();

        init();
    }

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

    /**
     * 判断SlidingView是否可以滑动
     * <p>如果返回false，无论是调用代码进行开合还是拖拽都无法滑动SlidingView</p>
     *
     * @return fals 不可以滑动
     * @see #setSlideEnable(boolean)
     */
    public boolean isSlideEnable() {
        return mEnableSlide;
    }

    /**
     * 设置SlidingView是否可以滑动
     * <p>如果设置为false，无论是调用代码进行开合还是拖拽都无法滑动SlidingView</p>
     *
     * @param enable false 为不可滑动
     * @see #isSlideEnable()
     */
    public void setSlideEnable(boolean enable) {
        mEnableSlide = enable;
    }

    /**
     * 判断SlidingView是否可以被拖拽
     * <p>如果返回false，则SlidingView是不可以被拖拽的</p>
     *
     * @return false 不可被拖拽
     * @see #setDragEnable(boolean)
     */
    public boolean isDragEnable() {
        return mEnableDrag;
    }

    /**
     * 设置SlidingView是否可以被拖拽
     * <p>如果设置false，则SlidingView是不可以被拖拽的</p>
     *
     * @param enable flase 不可被拖拽
     * @see #isDragEnable()
     */
    public void setDragEnable(boolean enable) {
        this.mEnableDrag = enable;
    }

    /**
     * 判断是否进行强制拦截手势
     * <p>如果返回true，则对于所有手势进行拦截<br>
     * 嵌套在内部的view将会接受不到任何手势<p/>
     *
     * @return true 进行强制拦截手势
     */
    public boolean isInterceptAllGestureEnable() {
        return mEnableInterceptAllGesture;
    }

    /**
     * 设置是否进行强制拦截手势
     * <p>如果设置true，则对于所有手势进行拦截<br>
     * 嵌套在内部的view将会接受不到任何手势<p/>
     *
     * @param enable true 进行强制拦截手势
     */
    public void setInterceptAllGestureEnable(boolean enable) {
        this.mEnableInterceptAllGesture = enable;
    }

    /**
     * 增加一个SlidingView的目标位置
     * <p>SlidingView的默认坐标系是以视图左上角的点位原点。
     * <br>y轴方向向上增大，方向向下减小。
     * <br>x轴方向向左增大，方向向右减小。
     * <br>单位为像素。
     * <br>如果你想要知道为什么会有这么奇葩的坐标系，那是因为我是以scrollX和scrollY为参考的。</br></p>
     *
     * @param positionId 目标位置的Id号，通过这个Id你可以简单的标识目标位置。请不要将Id设置为-1，这已经被占用。
     * @param x          目标位置的x轴坐标，单位像素
     * @param y          目标位置的y轴坐标，单位像素
     */
    public void addPosition(int positionId, int x, int y) {

        mPositionManager.addPosition(positionId, new Coordinate(x, y));
    }

    /**
     * 获取SlidingView的初始位置坐标
     * <p><br>采用的坐标系和{@link #addPosition(int, int, int)}的相同，单位为像素。
     * <br>通过{@link #SlidingView(android.content.Context, int, int)}或者
     * <br>.xml中的initialX和initialY属性，可以设置SlidingView的初始位置</p>
     *
     * @return SlidingView的初始位置坐标，如果为null的话，则没有这是初始位置坐标。
     */
    public Coordinate getInitialPosition() {
        return mPositionManager.getCoordinate(mPositionManager.POSITION_INITIAL);
    }

    /**
     * 设置SlidingView的左侧滑动范围
     * <p>取值范围为[initialX,~）,如果设置的范围小于initialX,则最终会视为无效。
     * <br>采用的坐标系和{@link #addPosition(int, int, int)}的相同，单位为像素。
     * <br>initialX和initialY是SlidingView的初始位置对应的坐标。
     * <br>通过{@link #getInitialPosition()}可是获取SlidingView的初始位置。
     * <br>通过{@link #SlidingView(android.content.Context, int, int)}或者
     * <br>.xml中的initialX和initialY属性，可以设置SlidingView的初始位置</p>
     *
     * @param leftSlideBound 左侧滑动范围
     */
    public void setLeftSlideBound(int leftSlideBound) {
        this.leftSlideBound = leftSlideBound;
    }

    /**
     * 设置SlidingView的右侧滑动范围
     * <p>取值范围为(~,initialX],如果设置的范围大于initialX,则最终会视为无效。
     * <br>采用的坐标系和{@link #addPosition(int, int, int)}的相同，单位为像素。
     * <br>initialX和initialY是SlidingView的初始位置对应的坐标。
     * <br>通过{@link #getInitialPosition()}可是获取SlidingView的初始位置。
     * <br>通过{@link #SlidingView(android.content.Context, int, int)}或者
     * <br>.xml中的initialX和initialY属性，可以设置SlidingView的初始位置</p>
     *
     * @param rightSlideBound 右侧滑动范围
     */
    public void setRightSlideBound(int rightSlideBound) {
        this.rightSlideBound = rightSlideBound;
    }

    /**
     * 设置SlidingView的上侧滑动范围
     * <p>取值范围为[initial,~）,如果设置的范围小于initialY,则最终会视为无效。
     * <br>采用的坐标系和{@link #addPosition(int, int, int)}的相同，单位为像素。
     * <br>initialX和initialY是SlidingView的初始位置对应的坐标。
     * <br>通过{@link #getInitialPosition()}可是获取SlidingView的初始位置。
     * <br>通过{@link #SlidingView(android.content.Context, int, int)}或者
     * <br>.xml中的initialX和initialY属性，可以设置SlidingView的初始位置</p>
     *
     * @param topSlideBound 顶侧滑动范围
     */
    public void setTopSlideBound(int topSlideBound) {
        this.topSlideBound = topSlideBound;
    }

    /**
     * 设置SlidingView的底侧滑动范围
     * <p>取值范围为(~,initialY],如果设置的范围大于initialY,则最终会视为无效。
     * <br>采用的坐标系和{@link #addPosition(int, int, int)}的相同，单位为像素。
     * <br>initialX和initialY是SlidingView的初始位置对应的坐标。
     * <br>通过{@link #getInitialPosition()}可是获取SlidingView的初始位置。
     * <br>通过{@link #SlidingView(android.content.Context, int, int)}或者
     * <br>.xml中的initialX和initialY属性，可以设置SlidingView的初始位置</p>
     *
     * @param bottomSlideBound 底侧滑动范围
     */
    public void setBottomSlideBound(int bottomSlideBound) {
        this.bottomSlideBound = bottomSlideBound;
    }

    //TODO 增加ignoreView功能，可以忽视某些区域的触摸事件

    /**
     * <p>主要的逻辑是这样的
     * <ol>
     * <li>当收到ACTION_DOWN事件且正在切换位置（也就是调用了{@link #switchPosition(Integer)}），
     * <br>那么停止切换，停留在现在位置。然后分发事件。</li>
     * <li>当收到ACTION_UP或ACTION_CANCLE</li>
     * </ol></p>
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) {
            GestureUtil.printEvent(ev, "onInterceptTouchEvent");
        }

        int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            /*
             *如果正在切换位置（也就是调用了switchPosition(Integer)），
             *那么停止切换，停留在现在位置。
            */
            //TODO 没有对当前的位置是否符合要求进行判断，有BUG
            case MotionEvent.ACTION_DOWN:
                if (isSwitching) {
                    stopSwitch();
                }
                break;
            /*
             *如果没有在拖拽，而且也没有到达任何一个目标位置
             * 那么就切换到最近的一个目标位置。
             */
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isDragging && !mPositionManager.isAtPosition(getScrollX(), getScrollY())) {
                    int position = mPositionManager.guessPosition(getScrollX(), getScrollY());
                    switchPosition(position, true, false, 0);
                }
                break;
        }

        //只有在设置了进行强制拦截手势的情况下，才会对手势进行拦截
        if (mEnableInterceptAllGesture) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //如果不允许滑动或者不允许拖拽，那么久乘早结束吧。
        if (!mEnableSlide || !mEnableDrag) {
            return false;
        }

        //准备好拖拽前的准备工作，顺便判断一下这个触摸事件符不符合拖拽的条件
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
                //上面已经对是否满足拖拽的条件进行了判断，所以这里直接返回true
                return true;
            case MotionEvent.ACTION_MOVE:
                int index = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                float currentX = MotionEventCompat.getX(event, index);
                float currentY = MotionEventCompat.getY(event, index);
                final float deltaX = mLastX - currentX;
                final float deltaY = mLastY - currentY;
                //第一次拖拽的距离一定要大于mTouchSlop这个阈值，如果大于这个阈值那么久开始拖拽
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
                    /*
                     *将拖拽的距离限定在滑动范围内，滑动的范围由2个值决定
                     * 一个是SlidingView自身设置的可滑动范围
                     * 另一个是根据所有目标位置计算出的滑动范围
                     * 取这俩个值中范围大的一个
                     */
                    scrollX = Math.min(scrollX, Math.max(leftSlideBound, mPositionManager.leftBound));
                    scrollX = Math.max(scrollX, Math.min(rightSlideBound, mPositionManager.rightBound));
                    scrollY = Math.min(scrollY, Math.max(topSlideBound, mPositionManager.topBound));
                    scrollY = Math.max(scrollY, Math.min(bottomSlideBound, mPositionManager.bottomBound));
                    scrollTo((int) scrollX, (int) scrollY);
                    return true;
                }
                return false;
            //最后一个手指离开时，SlidingView也许没有到达任意一个目标位置，因此需要对此处理。
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

                    //根据滑动的速率、初始位置、现在的位置，决定要到达的目标位置
                    int position = mPositionManager.determineNextPosition(initialVelocityX, initialVelocityY
                            , mInitialScrollX, mInitialScrollY, scrollX, scrollY);
                    switchPosition(position, true, true, initialVelocityX, initialVelocityY);
                    endDrag();
                } else if (!mPositionManager.isAtPosition(getScrollX(), getScrollY())) {
                    //如果没有滑动，但也不再任意一个目标位置，那么就找一个最近的位置作为要到达的目标位置
                    int poition = mPositionManager.guessPosition(getScrollX(), getScrollY());
                    switchPosition(poition, true, false, 0);
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
            if (!isSwitching) {
                isSwitching = true;
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
        } else {
            if (isSwitching) {
                isSwitching = false;
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
        if ((isDragging || isSwitching) && !mDrawingCacheEnabled) {
            setDrawingCacheEnabled(true);
        }
        super.onDraw(canvas);
        if (!isDragging && !isSwitching && mDrawingCacheEnabled) {
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
