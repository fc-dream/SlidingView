package com.kohoh.SlidingView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SlidingView extends FrameLayout {
    private static final boolean DEBUG = false;
    private static final String TAG = "SlidingView";
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
    private PositionSet mPositionSet;
    private PositionHelper mPositionHelper;
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
    private Set<View> mIgnoreViewSet;
    private onSwitchedListener mSwitchedListener;
    public static final int POSITION_INITIAL = PositionSet.POSITION_INITIAL;

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
        this(context, 0, 0);
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
        super(context);
        init();
        mPositionSet = new PositionSet(new Coordinate(initialX, initialY));
        mPositionHelper = new PositionHelper(mPositionSet);
        leftSlideBound = initialX;
        rightSlideBound = initialX;
        topSlideBound = initialY;
        bottomSlideBound = initialY;
        mEnableDrag = true;
        mEnableSlide = true;
        mEnableInterceptAllGesture = false;
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
        mPositionSet = new PositionSet(new Coordinate(initialX, initialY));
        mPositionHelper = new PositionHelper(mPositionSet);
        leftSlideBound = (int) ta.getDimension(R.styleable.SlidingView_leftSlideBound, initialX);
        rightSlideBound = (int) ta.getDimension(R.styleable.SlidingView_rightSlideBound, initialX);
        topSlideBound = (int) ta.getDimension(R.styleable.SlidingView_topSlideBound, initialY);
        bottomSlideBound = (int) ta.getDimension(R.styleable.SlidingView_bottomSlideBound, initialY);
        mEnableDrag = ta.getBoolean(R.styleable.SlidingView_dragEnable, true);
        mEnableSlide = ta.getBoolean(R.styleable.SlidingView_slideEnable, true);
        mEnableInterceptAllGesture = ta.getBoolean(R.styleable.SlidingView_interceptAllGestureEnable, false);
        ta.recycle();

        init();
    }

    /**
     * 初始化SlidingView所需要做的工作
     */
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

        mPositionSet.addPosition(positionId, new Coordinate(x, y));
    }

    /**
     * 获取SlidingView的初始位置坐标
     * <p>采用的坐标系和{@link #addPosition(int, int, int)}的相同，单位为像素。通过
     * {@link #SlidingView(android.content.Context, int, int)}或者.xml中的initialX和initialY属性，
     * 可以设置SlidingView的初始位置</p>
     *
     * @return SlidingView的初始位置坐标，如果为null的话，则没有这是初始位置坐标。
     */
    public Coordinate getInitialPosition() {
        return mPositionHelper.getCoordinate(mPositionSet.POSITION_INITIAL);
    }

    /**
     * 设置SlidingView的左侧滑动范围
     * <p>取值范围为[initialX,~）,如果设置的范围小于initialX,则最终会视为无效。采用的坐标系和
     * {@link #addPosition(int, int, int)}的相同，单位为像素。initialX和initialY是SlidingView的初始
     * 位置对应的坐标。通过{@link #getInitialPosition()}可是获取SlidingView的初始位置。通过
     * {@link #SlidingView(android.content.Context, int, int)}或者.xml中的initialX和initialY属性，
     * 可以设置SlidingView的初始位置</p>
     *
     * @param leftSlideBound 左侧滑动范围
     */
    public void setLeftSlideBound(int leftSlideBound) {
        this.leftSlideBound = leftSlideBound;
    }

    /**
     * 获取SlidingView的左侧滑动范围
     *
     * @return 左侧滑动范围
     * @see #setLeftSlideBound(int)
     */
    public int getLeftSlideBound() {
        return this.leftSlideBound;
    }

    /**
     * 设置SlidingView的右侧滑动范围
     * <p>取值范围为(~,initialX],如果设置的范围大于initialX,则最终会视为无效。采用的坐标系和
     * {@link #addPosition(int, int, int)}的相同，单位为像素。initialX和initialY是SlidingView的初始
     * 位置对应的坐标。通过{@link #getInitialPosition()}可是获取SlidingView的初始位置。通过
     * {@link #SlidingView(android.content.Context, int, int)}或者.xml中的initialX和initialY属性，
     * 可以设置SlidingView的初始位置</p>
     *
     * @param rightSlideBound 右侧滑动范围
     */
    public void setRightSlideBound(int rightSlideBound) {
        this.rightSlideBound = rightSlideBound;
    }

    /**
     * 获取SlidingView的右侧滑动范围
     *
     * @return 右侧滑动范围
     * @see #setRightSlideBound(int)
     */
    public int getRightSlideBound() {
        return this.rightSlideBound;
    }

    /**
     * 设置SlidingView的上侧滑动范围
     * <p>取值范围为[initial,~）,如果设置的范围小于initialY,则最终会视为无效。采用的坐标系和
     * {@link #addPosition(int, int, int)}的相同，单位为像素。initialX和initialY是SlidingView的初始
     * 位置对应的坐标。通过{@link #getInitialPosition()}可是获取SlidingView的初始位置。通过
     * {@link #SlidingView(android.content.Context, int, int)}或者.xml中的initialX和initialY属性，
     * 可以设置SlidingView的初始位置</p>
     *
     * @param topSlideBound 顶侧滑动范围
     */
    public void setTopSlideBound(int topSlideBound) {
        this.topSlideBound = topSlideBound;
    }

    /**
     * 获取SlidingView的上侧滑动范围
     *
     * @return 顶侧滑动范围
     * @see #setTopSlideBound(int)
     */
    public int getTopSlideBound() {
        return this.topSlideBound;
    }

    /**
     * 设置SlidingView的底侧滑动范围
     * <p>取值范围为(~,initialY],如果设置的范围大于initialY,则最终会视为无效。采用的坐标系和
     * {@link #addPosition(int, int, int)}的相同，单位为像素。initialX和initialY是SlidingView的初始
     * 位置对应的坐标。通过{@link #getInitialPosition()}可是获取SlidingView的初始位置。
     * 通过{@link #SlidingView(android.content.Context, int, int)}或者.xml中的initialX和initialY属性，
     * 可以设置SlidingView的初始位置</p>
     *
     * @param bottomSlideBound 底侧滑动范围
     */
    public void setBottomSlideBound(int bottomSlideBound) {
        this.bottomSlideBound = bottomSlideBound;
    }

    /**
     * 获取SlidingView的底侧滑动范围
     *
     * @return SlidingView的底侧滑动范围
     * @see #setBottomSlideBound(int)
     */
    public int getBottomSlideBound() {
        return this.bottomSlideBound;
    }

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
                if (!isDragging && !mPositionHelper.isAtPosition(getScrollX(), getScrollY())) {
                    int position = mPositionHelper.guessPosition(getScrollX(), getScrollY());
                    switchPosition(position, true, true, 0);
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

        //做好好拖拽前的准备工作
        prepareToDrag(event);
        mVelocityTracker.addMovement(event);

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
                    if ((Math.abs(deltaX) >= mTouchSlop || Math.abs(deltaY) >= mTouchSlop)
                            && allowSlidingFromHere(mLastX, mLastY)) {
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
                    scrollX = Math.min(scrollX, Math.max(leftSlideBound, mPositionSet.leftBound));
                    scrollX = Math.max(scrollX, Math.min(rightSlideBound, mPositionSet.rightBound));
                    scrollY = Math.min(scrollY, Math.max(topSlideBound, mPositionSet.topBound));
                    scrollY = Math.max(scrollY, Math.min(bottomSlideBound, mPositionSet.bottomBound));
                    scrollTo((int) scrollX, (int) scrollY);
                    return true;
                }
                return false;
            //最后一个手指离开时，SlidingView也许没有到达任意一个目标位置，因此需要对此处理。
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    final int initialVelocityX = (int) VelocityTrackerCompat.getXVelocity(
                            mVelocityTracker, mActivePointerId);
                    final int initialVelocityY = (int) VelocityTrackerCompat.getYVelocity(
                            mVelocityTracker, mActivePointerId);
                    final int scrollX = getScrollX();
                    final int scrollY = getScrollY();

                    //根据滑动的速率、初始位置、现在的位置，决定要到达的目标位置
                    int position = mPositionHelper.determineTargetPosition(initialVelocityX,
                            initialVelocityY, mInitialScrollX, mInitialScrollY, scrollX, scrollY);
                    if (DEBUG) {
                        Log.v(TAG, "determineTargetPosition=" + position);
                    }
                    switchPosition(position, true, true, initialVelocityX, initialVelocityY);
                } else if (!mPositionHelper.isAtPosition(getScrollX(), getScrollY())) {
                    //如果没有滑动，但也不再任意一个目标位置，那么就找一个最近的位置作为要到达的目标位置
                    int poition = mPositionHelper.guessPosition(getScrollX(), getScrollY());
                    switchPosition(poition, true, true, 0);
                }
                endDrag();
                return true;
            case MotionEvent.ACTION_CANCEL:
                switchPosition(mPositionSet.getCurrentPosition(), true, true, 0);
                endDrag();
                return true;
            default:
                return false;
        }
    }

    /**
     * 当要结束拖拽时，调用此方法。
     * <p>在结束拖拽后，需要充值某些拖拽时使用的参数。这样才能保证下次拖拽顺利。</p>
     */
    private void endDrag() {
        isDragging = false;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        mActivePointerId = INVALID_POINTER;
    }

    /**
     * 做好拖拽前的准备工作。
     * <p>SlidingView的拖拽都是以一个Id为{@link #mActivePointerId}的手指为参照进行的。除了在第一次
     * 拖拽前需要做好拖拽的准备工作。如果我们跟踪的手指离开屏幕，但是还有其他手指在屏幕上时，我们还
     * 要寻找另外一根手指作为跟踪手指。并重新做好拖拽的准备。如果拖拽没有准备好，就不能进行拖拽。</p>
     */
    private void prepareToDrag(MotionEvent event) {
        if (MotionEventCompat.findPointerIndex(event, mActivePointerId) == INVALID_POINTER) {
            int activePointerIndex = MotionEventCompat.getActionIndex(event);
            float currentX = MotionEventCompat.getX(event, activePointerIndex);
            float currentY = MotionEventCompat.getY(event, activePointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            } else {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mActivePointerId = MotionEventCompat.getPointerId(event, activePointerIndex);
            mInitialScrollX = getScrollX();
            mInitialScrollY = getScrollY();
            mLastX = currentX;
            mLastY = currentY;
        }
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
     */
    private boolean allowSlidingFromHere(final float initialX, final float initialY) {
        boolean allow = true;
        if (whetherInIgnoreView(initialX, initialY)) {
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
                if (mPositionHelper.isAtPosition(getScrollX(), getScrollY())) {
                    if (mSwitchedListener != null) {
                        mSwitchedListener.onSwitched(mPositionSet.getCurrentPosition());
                    }
                }
            }
        }
    }

    /**
     * 停止位置切换，SlidingMenu将会停留在当前的这个位置。
     */
    private void stopSwitch() {
        if (isSwitching) {
            mScroller.abortAnimation();
        }
    }

    /**
     * 跳过动画的部分，直接完成位置切换
     */
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

    /**
     * 将SlidingView切换到指定的目标位置
     * <p>默认开启动画效果，且进行强行切换</p>
     *
     * @param targetPosition 目标位置对应的Id
     */
    public void switchPosition(Integer targetPosition) {
        switchPosition(targetPosition, true, true, 0);
    }

    /**
     * 将SlidingView切换到指定的目标位置
     *
     * @param targetPosition 目标位置对应的Id
     * @param smoothAnim     是否使用动画效果
     * @param forceSwitch    是否强化切换位置
     * @param velocity       切换的速率
     */
    public void switchPosition(final int targetPosition, final boolean smoothAnim,
                               final boolean forceSwitch, final int velocity) {

        if (!mEnableSlide) {
            return;
        }

        Coordinate targetCoordinate = mPositionHelper.getCoordinate(targetPosition);
        Integer currentPosition = mPositionSet.getCurrentPosition();

        if (targetCoordinate == null) {
            return;
        }

        if (!forceSwitch && targetPosition == currentPosition) {
            return;
        }

        mPositionSet.setCurrentPosition(targetPosition);
        completeSwitch();
        if (smoothAnim) {
            smoothScrollTo(targetCoordinate.x, targetCoordinate.y, velocity);
        } else {
            scrollTo(targetCoordinate.x, targetCoordinate.y);
        }
    }

    /**
     * 将SlidingView切换到指定的目标位置
     *
     * @param targetPosition 目标位置对应的Id
     * @param smoothAnim     是否使用动画效果
     * @param forceSwitch    是否强化切换位置
     * @param velocityX      x轴的切换速率
     * @param velocityY      y轴的切换速率
     */
    public void switchPosition(final int targetPosition, final boolean smoothAnim,
                               final boolean forceSwitch, final int velocityX, final int velocityY) {
        int velocity = (int) Math.sqrt((velocityX * velocityX) + (velocityY * velocityY));
        switchPosition(targetPosition, smoothAnim, forceSwitch, velocity);
    }

    //TODO 硬件加速和绘制缓存之间，到底使用哪一个更好。对于不同的版本如何选择。

    /**
     * 决定是否开启绘制缓存。
     * 根据我的理解，在不使用硬件加速的情况下。开启绘制缓存，有助于滑动的平滑
     * 但是这似乎并非针对所有的系统版本。我翻过源码，现在知道的是
     * 在API leave10似乎没有这方面的优化。据此推断API leave10之前也没有优化
     * 在API leave19有相关的优化。
     * 限于时间问题没有再进一步的研究，放在以后和硬件加速的开关一起考虑吧
     */
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

    /**
     * 忽视指定视图所在区域的拖拽手势
     * <p>被调用之后，无法在被忽视的视图所在的区域进行拖拽</p>
     *
     * @param view 要忽视拖拽手势的视图
     * @return false 忽视失败
     */
    public boolean addIgnoreView(View view) {
        boolean handle = false;

        if (mIgnoreViewSet == null) {
            mIgnoreViewSet = new HashSet<View>();
        }

        if (view != null && view.getVisibility() == VISIBLE) {
            handle = mIgnoreViewSet.add(view);
        }

        return handle;
    }

    /**
     * 忽视指定视图所在区域的拖拽手势
     * <p>被调用之后，无法在被忽视的视图所在的区域进行拖拽</p>
     *
     * @param id 要忽视拖拽手势的视图对应的id
     * @return false 忽视失败
     */
    public boolean addIgnoreView(int id) {
        View view = findViewById(id);
        return addIgnoreView(view);
    }

    /**
     * 取消忽视指定视图所在区域的拖拽手势
     * <P>指定视图所在的区域又可以重新进行拖拽了</P>
     *
     * @param view 要取消忽视所在区域拖拽手势的视图。
     * @return false 取消忽视失败
     */
    public boolean removeIgnoreView(View view) {
        boolean handle = false;

        if (mIgnoreViewSet != null) {
            handle = mIgnoreViewSet.remove(view);
        }
        return handle;
    }

    /**
     * 取消忽视指定视图所在区域的拖拽手势
     * <P>指定视图所在的区域又可以重新进行拖拽了</P>
     *
     * @param id 要取消忽视所在区域拖拽手势的视图对应的id。
     * @return false 取消忽视失败
     */
    public boolean removeIgnoreView(int id) {
        View view = findViewById(id);
        return removeIgnoreView(view);
    }

    /**
     * 判断该坐标位置是否在要忽视拖拽的视图所在的区域内
     *
     * @param x x轴所在坐标
     * @param y y轴所在坐标
     * @return true 忽视该位置的拖拽
     */
    private boolean whetherInIgnoreView(float x, float y) {
        boolean handle = false;
        View view;

        if (mIgnoreViewSet != null) {
            Iterator<View> iterator = mIgnoreViewSet.iterator();
            while (iterator.hasNext()) {
                view = iterator.next();
                if (view != null) {
                    Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                    handle = rect.contains((int) (x + getScrollX()), (int) (y + getScrollY()));
                    if (handle == true) {
                        break;
                    }
                }
            }
        }
        return handle;
    }

    public void setSwitchedListener(onSwitchedListener Listener) {
        this.mSwitchedListener = Listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Make sure scroll position is set correctly.
        if (w != oldw) {
            switchPosition(mPositionSet.getCurrentPosition(), false, true, 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //如果正在拖动或者切换，那么开启绘制缓存
        if ((isDragging || isSwitching) && !mDrawingCacheEnabled) {
            setDrawingCacheEnabled(true);
        }
        super.onDraw(canvas);
        //如果没有在拖动，也没有在切换，那么关闭绘制缓存
        if (!isDragging && !isSwitching && mDrawingCacheEnabled) {
            setDrawingCacheEnabled(false);
        }
    }

    /**
     * 封装位置的坐标信息
     */
    static class Coordinate {
        /**
         * x轴的坐标
         */
        public int x;
        /**
         * y轴的坐标
         */
        public int y;

        /**
         * 构建一个坐标，并设置他的值。
         *
         * @param x x轴的坐标
         * @param y y轴的坐标
         */
        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * 计算两坐标之间的距离
         *
         * @param c1 坐标1
         * @param c2 坐标2
         * @return 坐标c1与坐标c2间的距离
         */
        static public float computeDistance(Coordinate c1, Coordinate c2) {
            return (float) Math.sqrt(Math.pow((c1.x - c2.x), 2) + Math.pow((c1.y - c2.y), 2));
        }
    }

    /**
     * 向量
     */
    static class Vector {
        /**
         * 向量在x轴上的坐标
         */
        int x;
        /**
         * 向量在y轴上的坐标
         */
        int y;
        /**
         * 向量的模
         */
        float length;

        /**
         * @param x 向量在x轴上的坐标
         * @param y 向量在y轴上的坐标
         */
        public Vector(int x, int y) {
            this.x = x;
            this.y = y;
            //计算向量的模
            this.length = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        }

        /**
         * 已知两点求向量
         *
         * @param start 起点
         * @param end   终点
         */
        public Vector(Coordinate start, Coordinate end) {
            this(end.x - start.x, end.y - start.y);
        }

        /**
         * 求两向量间的夹角的余玄
         *
         * @param v1 向量1
         * @param v2 向量2
         * @return 夹角余玄
         */
        static public float computeCos(Vector v1, Vector v2) {
            float cos1 = v1.x / v1.length;
            float sin1 = v1.y / v1.length;
            float cos2 = v2.x / v2.length;
            float sin2 = v2.y / v2.length;

            float cos = cos1 * cos2 + sin1 * sin2;
            return cos;
        }
    }

    /**
     * 通过提供给一系列的方法来协助管理位置信息。
     * <p>此外PositionManager还提供了一些列的方法，帮助你到达
     * 到达目标位置。你可以通过{@link #determineTargetPosition(float, float, int, int, int, int)}
     * {@link #guessPosition(int, int)} {@link #guessPosition(int, int, int, int)}得到你的目标位置。
     * 通过{@link #getCoordinate(int)}获取对应位置的坐标。</p>
     */
    class PositionHelper {
        private PositionSet positionSet;

        public PositionHelper(PositionSet positionSet) {
            this.positionSet = positionSet;
        }

        /**
         * 判断当前位置是否处于所有位置之一
         *
         * @param currentX 当前位置的x轴坐标
         * @param currentY 当前位置的y轴坐标
         * @return true 处于所有位置之一
         */
        public boolean isAtPosition(int currentX, int currentY) {
            Iterator iterator = positionSet.coordinateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Coordinate> entry = (Map.Entry<Integer, Coordinate>) iterator.next();
                Coordinate targetCoordinate = entry.getValue();
                if (targetCoordinate.x == currentX && targetCoordinate.y == currentY) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 获取对应Id的位置坐标
         *
         * @param positionId 位置对应的Id
         * @return 位置对应的坐标
         */
        public Coordinate getCoordinate(int positionId) {
            return positionSet.coordinateMap.get(positionId);
        }

        /**
         * 根据当前的位置，猜测你想要到达的位置
         * <p>此处的算法是寻找最近的一个位置作为想要到达的位置</p>
         *
         * @param currentX 当前位置的x轴坐标
         * @param currentY 当前位置的y轴坐标
         * @return 猜测想要到达的位置对应的Id
         */
        public int guessPosition(int currentX, int currentY) {
            Coordinate currentCorrdinate = new Coordinate(currentX, currentY);
            float min = Float.MAX_VALUE;
            Iterator iterator = positionSet.coordinateMap.entrySet().iterator();
            int targetPosition = POSITION_INITIAL;
            while (iterator.hasNext()) {
                Map.Entry<Integer, Coordinate> entry = (Map.Entry<Integer, Coordinate>) iterator.next();
                Coordinate targetCoordinate = entry.getValue();
                float distance = Coordinate.computeDistance(targetCoordinate, currentCorrdinate);
                if (distance < min) {
                    min = distance;
                    targetPosition = entry.getKey();
                }
            }
            return targetPosition;
        }

        /**
         * 猜测目标位置
         *
         * @param endX   滑动的起始位置的x轴坐标
         * @param startY 滑动的起始位置的y轴坐标
         * @param endX   当前位置的x轴坐标
         * @param endY   当前位置的y轴坐标
         * @return 猜测想要到达的位置对应的Id
         * @see #guessPosition(Coordinate, Coordinate);
         */
        public int guessPosition(final int startX, final int startY
                , final int endX, final int endY) {
            return guessPosition(new Coordinate(startX, startY), new Coordinate(endX, endY));
        }

        /**
         * 猜测目标位置
         * <p>根据移动向量，猜测要到达的位置。移动向量由start和end得到。计算从start到各个位置的向量
         * 与移动向量的夹角。取满足一下条件的位置作为所猜测的位置
         * <ol>
         * <li>夹角在0度到45度之间</li>
         * <li>夹角最小</li>
         * <li>从end到猜测位置的距离最短</li>
         * <li>不为当前位置</li>
         * </ol>
         * 如果不满足以上条件，则返回当前位置。</p>
         *
         * @param start 向量的起点
         * @param end   向量的终点
         * @return
         */
        public int guessPosition(Coordinate start, Coordinate end) {
            //精度取5度
            final float precision = (float) Math.abs(Math.cos(Math.PI / 2) - Math.cos(Math.PI / 18 * 19));
            int guess = positionSet.getCurrentPosition();
            Vector vector1 = new Vector(start, end);
            float maxCos = Float.MIN_VALUE;
            float minDis = Float.MAX_VALUE;
            Iterator<Map.Entry<Integer, Coordinate>> iterator = positionSet.coordinateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Coordinate> entry = iterator.next();
                Coordinate coordinate = entry.getValue();
                Vector vector2 = new Vector(start, coordinate);
                float cos = Vector.computeCos(vector1, vector2);
                //判断是否是当前位置
                if (entry.getKey() == positionSet.getCurrentPosition()) {
                    continue;
                }
                //判断是否在0度到45度之间
                if (!(Math.cos(Math.PI / 4) <= cos && cos <= Math.cos(0))) {
                    continue;
                }
                //判断cos是否大于maxCos
                if ((cos - maxCos) > precision) {
                    maxCos = cos;
                    minDis = Coordinate.computeDistance(end, coordinate);
                    guess = entry.getKey();
                }
                //判断cos是否等于maxCos且距离更小
                if (Math.abs(cos - maxCos) < precision && Coordinate.computeDistance(end, coordinate) < minDis) {
                    maxCos = cos;
                    minDis = Coordinate.computeDistance(end, coordinate);
                    guess = entry.getKey();
                }
            }
            return guess;
        }

        /**
         * 决定要到达的目标位置
         * <p>算法首先根据滑动的起始位置和现在的位置，猜测一个想要到达的目标位置。然后判断滑动的速度
         * 是否到达一个阈值，如果是则目标位置就是所猜测的位置。 如果速度没有达到阈值，判断已经滑动的
         * 距离是否已经占总距离的一半，如果是，则目标位置就是所猜测的位置。否则目标位置就是滑动前的
         * 位置。</p>
         *
         * @param velocityX x轴的滑动速度
         * @param velocityY y轴的滑动速度
         * @param initialX  滑动起始位置的x轴坐标
         * @param initialY  滑动起始位置的y轴坐标
         * @param currentX  当前位置的x轴坐标
         * @param currentY  当前位置的y轴坐标
         * @return 决定要到达的目标位置所对应的Id
         */
        public int determineTargetPosition(float velocityX, float velocityY
                , int initialX, int initialY
                , int currentX, int currentY) {

            int nextPosition = positionSet.getCurrentPosition();
            int desirePositionId = guessPosition(initialX, initialY, currentX, currentY);
            if (DEBUG) {
                Log.v(TAG, "geussPosition=" + desirePositionId);
            }
            Coordinate desireP = positionSet.coordinateMap.get(desirePositionId);
            Coordinate initialP = new Coordinate(initialX, initialY);
            Coordinate currentP = new Coordinate(currentX, currentY);

            float currentDistance = Coordinate.computeDistance(initialP, currentP);
            float totalDistance = Coordinate.computeDistance(initialP, desireP);
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

    /**
     * 位置集合。
     * <p>该类内部提供了一个Map来管理所有的位置信息。该Map以位置对应的Id作为key，位置对应的坐标作为
     * value。你可以通过{@link #addPosition(int, Coordinate)}和{@link #removePosition(int)}来增加和移
     * 除位置。注意的是，该Map中默认有一个初始位
     * 置。其位置对应的Id为{@link #POSITION_INITIAL},因此请不要添加以-1为Id的位置。初始位置的坐标只
     * 能在构建PositionManager时设置 </p>
     */
    class PositionSet {

        /**
         * 构建一个PositionManager，并且设置初始位置为(0,0)
         */
        public PositionSet() {
            this(new Coordinate(0, 0));
        }

        /**
         * 构建一个PositionManager,并设置初始位置
         *
         * @param initialCoordinate 初始位置对应的坐标
         */
        public PositionSet(Coordinate initialCoordinate) {
            coordinateMap = new HashMap<Integer, Coordinate>();
            addPosition(POSITION_INITIAL, initialCoordinate);
        }

        /**
         * 初始位置对应的Id
         */
        public static final int POSITION_INITIAL = -1;
        Map<Integer, Coordinate> coordinateMap;
        private int leftBound = Integer.MIN_VALUE;
        private int topBound = Integer.MIN_VALUE;
        private int rightBound = Integer.MAX_VALUE;
        private int bottomBound = Integer.MAX_VALUE;
        private Integer currentPosition = POSITION_INITIAL;

        /**
         * 获取当前位置
         *
         * @return 当前位置
         */
        public Integer getCurrentPosition() {
            return currentPosition;
        }

        /**
         * 设置当前位置
         *
         * @param currentPosition 当前位置
         */
        public void setCurrentPosition(Integer currentPosition) {
            this.currentPosition = currentPosition;
        }

        /**
         * 增加一个位置
         *
         * @param positionId 位置所对应的Id。请不要设置为-1，-1默认为初始位置对应的Id。
         * @param coordinate 位置所对应的坐标
         * @return true 成功增加位置
         */
        public boolean addPosition(int positionId, Coordinate coordinate) {
            boolean result = true;

            if (coordinateMap.containsKey(positionId)) {
                result = false;
            } else if (coordinateMap.put(positionId, coordinate) == null) {
                result = false;
            }

            setBound();

            return result;
        }

        /**
         * 移除一个位置
         *
         * @param positionId 位置对应的Id
         * @return true 成功移除位置
         */
        public boolean removePosition(int positionId) {
            if (positionId == POSITION_INITIAL) {
                return false;
            }
            if (coordinateMap.remove(positionId) == null) {
                return false;
            } else {
                setBound();
                return true;
            }
        }

        /**
         * 根据所有位置的坐标，设置所能到达的上下左右的最大范围
         */
        private void setBound() {
            leftBound = Integer.MIN_VALUE;
            topBound = Integer.MIN_VALUE;
            rightBound = Integer.MAX_VALUE;
            bottomBound = Integer.MAX_VALUE;

            Iterator iterator = coordinateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Coordinate> entry = (Map.Entry<Integer, Coordinate>) iterator.next();
                Coordinate coordinate = entry.getValue();

                leftBound = (int) Math.max(leftBound, coordinate.x);
                rightBound = (int) Math.min(rightBound, coordinate.x);
                topBound = (int) Math.max(topBound, coordinate.y);
                bottomBound = (int) Math.min(bottomBound, coordinate.y);
            }
        }

    }

    /**
     * 切换完成监听器，监听切换完成事件
     */
    public interface onSwitchedListener {
        /**
         * 当切换完成时会触发该方法
         *
         * @param currentPosition 当前位置
         */
        public void onSwitched(int currentPosition);
    }

}
