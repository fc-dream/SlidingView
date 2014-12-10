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

import com.kohoh.Exception.IllegalPosition;
import com.kohoh.util.GestureUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SlidingView extends FrameLayout {
    protected static final Interpolator sMenuInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return (float) Math.pow(t, 5) + 1.0f;
        }
    };
    protected static final int INVALID_POINTER = -1;
    protected int mActivePointerId = INVALID_POINTER;
    private static final boolean DEBUG = false;
    private static final String TAG = "SlidingView";
    private static final int MAX_SCROLLING_DURATION = 600; // in ms
    protected int mTouchSlop;
    protected int mMaximumVelocity;
    protected PositionManager mPositionManager;
    protected PositionHelper mPositionHelper;
    protected Scroller mScroller;
    protected boolean mEnableDrag;
    protected boolean isSwitching;
    protected boolean mEnableSlide;
    protected boolean isDragging;
    protected boolean mEnableInterceptAllGesture;
    protected boolean mSlidingCacheEnabled;
    protected VelocityTracker mVelocityTracker;
    protected int mInitialScrollX;
    protected int mInitialScrollY;
    protected float mLastX;
    protected float mLastY;
    protected Set<View> mIgnoreViewSet;
    protected onSwitchedListener mSwitchedListener;

    /**
     * 构建一个SlidingView。
     *
     * @param context 想关联的Context
     * @see #SlidingView(android.content.Context, int, int)
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
     * </ol>
     *
     * @param context  想关联的Context
     * @param initialX 初始位置的x轴坐标
     * @param initialY 初始位置的y轴坐标
     */
    public SlidingView(Context context, int initialX, int initialY) {
        super(context);
        init();
        mPositionManager = new PositionManager(initialX, initialY);
        mPositionHelper = new PositionHelper(mPositionManager, context);
        mEnableDrag = true;
        mEnableSlide = true;
        mEnableInterceptAllGesture = false;
    }

    /**
     * 构建一个SlidingView，一般当你在.xml中声明该类时，系统会调用这个构建函数。
     *
     * @param context 想关联的Context
     * @param attrs   从.xml文件中获取的和布局相关的属性集合
     * @see #SlidingView(android.content.Context, int, int)
     */
    public SlidingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * 构建一个SlidingView，一般当你在.xml中声明该类时，系统会调用这个构建函数。
     *
     * @param context  想关联的Context
     * @param attrs    从.xml文件中获取的和布局相关的属性集合
     * @param defStyle 默认的设计样式的Id
     * @see #SlidingView(android.content.Context, int, int)
     */
    public SlidingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingView);
        int initialX = (int) ta.getDimension(R.styleable.SlidingView_initialX, 0);
        int initialY = (int) ta.getDimension(R.styleable.SlidingView_initialY, 0);
        mPositionManager = new PositionManager(initialX, initialY);
        mPositionHelper = new PositionHelper(mPositionManager, context);
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
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        //TODO 在开启硬件加速的情况下不似乎不需要开启绘制缓存
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
     * 注意：此时嵌套在内部的view将会接受不到任何手势<p/>
     *
     * @return true 进行强制拦截手势
     */
    public boolean isInterceptAllGestureEnable() {
        return mEnableInterceptAllGesture;
    }

    /**
     * 设置是否进行强制拦截手势
     * <p>如果设置true，则对于所有手势进行拦截<br>
     * 注意：此时嵌套在内部的view将会接受不到任何手势<p/>
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
     * <br>x轴方向向右增大，方向向左减小。
     * <br>单位为像素。</p>
     *
     * @param id 目标位置的Id号，通过这个Id你可以简单的标识目标位置。请不要将Id设置为
     *           {@link PositionManager#POSITION_INITIAL},或设置为{@link Integer#MIN_VALUE}也是无效的
     * @param x  目标位置的x轴坐标，单位像素
     * @param y  目标位置的y轴坐标，单位像素
     */
    public void addPosition(int id, int x, int y) {
        mPositionManager.addPosition(id, x, y);
    }

    /**
     * 获取SlidingView的初始位置坐标
     * 初始位置只能通过通过{@link #SlidingView(android.content.Context, int, int)}或者.xml中的
     * initialX和initialY属性设置</p>
     *
     * @return SlidingView的初始位置坐标。
     */
    public Position getInitialPosition() {
        return this.mPositionManager.findPositionById(PositionManager.POSITION_INITIAL);
    }

    /**
     * 获取SlidingView的左侧滑动范围
     *
     * @return 左侧滑动范围
     * @see #setLeftSlideBound(int)
     */
    public int getLeftSlideBound() {
        return mPositionManager.getLeftBound();
    }

    /**
     * 设置SlidingView的左侧滑动范围
     * <p>取值范围为(~,initialX],如果设置的范围大于initialX,则最终会视为无效。
     * 可以设置SlidingView的初始位置</p>
     *
     * @param bound 左侧滑动范围
     */
    public void setLeftSlideBound(int bound) {
        mPositionManager.setLeftBound(bound);
    }

    /**
     * 获取SlidingView的右侧滑动范围
     *
     * @return 右侧滑动范围
     * @see #setRightSlideBound(int)
     */
    public int getRightSlideBound() {
        return mPositionManager.getRightBound();
    }

    /**
     * 设置SlidingView的右侧滑动范围
     * <p>取值范围为[initialX,~),如果设置的范围小于initialX,则最终会视为无效。
     * 可以设置SlidingView的初始位置</p>
     *
     * @param bound 右侧滑动范围
     */
    public void setRightSlideBound(int bound) {
        mPositionManager.setRightBound(bound);
    }

    /**
     * 获取SlidingView的上侧滑动范围
     *
     * @return 顶侧滑动范围
     * @see #setTopSlideBound(int)
     */
    public int getTopSlideBound() {
        return mPositionManager.getTopBound();
    }

    /**
     * 设置SlidingView的上侧滑动范围
     * <p>取值范围为[initial,~）,如果设置的范围小于initialY,则最终会视为无效。</p>
     *
     * @param bound 顶侧滑动范围
     */
    public void setTopSlideBound(int bound) {
        mPositionManager.setTopBound(bound);
    }

    /**
     * 获取SlidingView的底侧滑动范围
     *
     * @return SlidingView的底侧滑动范围
     * @see #setBottomSlideBound(int)
     */
    public int getBottomSlideBound() {
        return mPositionManager.getBottomBound();
    }

    /**
     * 设置SlidingView的底侧滑动范围
     * <p>取值范围为(~,initialY],如果设置的范围大于initialY,则最终会视为无效。</p>
     *
     * @param bound 底侧滑动范围
     */
    public void setBottomSlideBound(int bound) {
        mPositionManager.setBottomBound(bound);
    }

    public void setBound(int left, int top, int right, int bottom) {
        mPositionManager.setBound(left, top, right, bottom);
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
                Position currentPosition = transformPosition(getScrollX(), getScrollY());
                if (!isDragging && !mPositionHelper.isAtPosition(currentPosition)) {
                    int position = mPositionHelper.guessPosition(currentPosition);
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
                    scrollX = Math.min(scrollX, (0 - mPositionManager.getLeftBound()));
                    scrollX = Math.max(scrollX, (0 - mPositionManager.getRightBound()));
                    scrollY = Math.min(scrollY, mPositionManager.getTopBound());
                    scrollY = Math.max(scrollY, mPositionManager.getBottomBound());
                    scrollTo((int) scrollX, (int) scrollY);
                    return true;
                }
                return false;
            //最后一个手指离开时，SlidingView也许没有到达任意一个目标位置，因此需要对此处理。
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    final float velocityX = VelocityTrackerCompat.getXVelocity(
                            mVelocityTracker, mActivePointerId);
                    final float velocityY = VelocityTrackerCompat.getYVelocity(
                            mVelocityTracker, mActivePointerId);
                    float velocity = (float) Math.sqrt(Math.pow(velocityX, 2) + Math.pow(velocityY, 2));
                    Position start = transformPosition(mInitialScrollX, mInitialScrollY);
                    Position end = transformPosition(getScrollX(), getScrollY());
                    //根据滑动的速率、初始位置、现在的位置，决定要到达的目标位置
                    int position = mPositionHelper.guessPosition(velocity, start, end);
                    switchPosition(position, true, true, (int) velocityX, (int) velocityY);
                } else if (!mPositionHelper.isAtPosition(transformPosition(getScrollX(), getScrollY()))) {
                    //如果没有滑动，但也不再任意一个目标位置，那么就找一个最近的位置作为要到达的目标位置
                    Position position = transformPosition(getScrollX(), getScrollY());
                    int poition = mPositionHelper.guessPosition(position);
                    switchPosition(poition, true, true, 0);
                }
                endDrag();
                return true;
            case MotionEvent.ACTION_CANCEL:
                switchPosition(mPositionManager.getCurrentPositionId(), true, true, 0);
                endDrag();
                return true;
            default:
                return false;
        }
    }

    /**
     * 当要结束拖拽时，调用此方法。
     * <p>在结束拖拽后，需要重制某些拖拽时使用的参数。这样才能保证下次拖拽顺利。</p>
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
                Position position = transformPosition(getScrollX(), getScrollY());
                if (mPositionHelper.isAtPosition(position)) {
                    if (mSwitchedListener != null) {
                        mSwitchedListener.onSwitched(mPositionManager.getCurrentPositionId());
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
     * @param targetPositionId 目标位置对应的Id
     */
    public void switchPosition(int targetPositionId) {
        switchPosition(targetPositionId, true, true, 0);
    }

    /**
     * 将SlidingView切换到指定的目标位置
     *
     * @param targetPositionId 目标位置对应的Id
     * @param smoothAnim       是否使用动画效果
     * @param forceSwitch      是否强化切换位置
     * @param velocity         切换的速率
     */
    public void switchPosition(final int targetPositionId, final boolean smoothAnim,
                               final boolean forceSwitch, final int velocity) {

        if (!mEnableSlide) {
            return;
        }

        Position targetPosition = transformPosition(this.mPositionManager.findPositionById(
                targetPositionId));
        int currentPositionId = mPositionManager.getCurrentPositionId();

        if (targetPosition == null) {
            return;
        }

        if (!forceSwitch && targetPositionId == currentPositionId) {
            return;
        }

        mPositionManager.setCurrentPositionId(targetPositionId);
        completeSwitch();
        if (smoothAnim) {
            smoothScrollTo(targetPosition.getX(), targetPosition.getY(), velocity);
        } else {
            scrollTo(targetPosition.getY(), targetPosition.getY());
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

    /**
     * 决定是否开启绘制缓存。
     * <p>在开启绘制缓存的情况下，会减少绘制的操作。进而使得滑动更加的平滑。<br>这里只是开启内部子视
     * 图的绘制缓存。SlidingView在滑动过程中不断调用{@link #invalidate()},所以开启绘制缓存完全没有必
     * 要。反倒是会增加运算的开销</p>
     */
    private void setSlidingCacheEnable(boolean enable) {

        if (mSlidingCacheEnabled != enable) {
            mSlidingCacheEnabled = enable;

            final int l = getChildCount();
            for (int i = 0; i < l; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    child.setDrawingCacheEnabled(enable);
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

    /**
     * 设置滑动完成监听器
     *
     * @param Listener
     */
    public void setSwitchedListener(onSwitchedListener Listener) {
        this.mSwitchedListener = Listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Make sure scroll position is set correctly.
        if (w != oldw) {
            switchPosition(mPositionManager.getCurrentPositionId(), false, true, 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //如果正在拖动或者切换，那么开启绘制缓存
        if ((isDragging || isSwitching) && !mSlidingCacheEnabled) {
            setSlidingCacheEnable(true);
        }
        super.onDraw(canvas);
        //如果没有在拖动，也没有在切换，那么关闭绘制缓存
        if (!isDragging && !isSwitching && mSlidingCacheEnabled) {
            setSlidingCacheEnable(false);
        }
    }

    /**
     * 将位置进行坐标系的转换
     * <p>进行转换是因为{@link PositionManager}采用的坐标系和scroll的坐标系不同</p>
     *
     * @param oldPosition
     * @return
     */
    private Position transformPosition(final Position oldPosition) {
        if (oldPosition == null) {
            throw new IllegalPosition("position is null");
        }

        return new Position((0 - oldPosition.getX()), oldPosition.getY());
    }

    /**
     * 将坐标进行坐标系的转换
     *
     * @see #transformPosition(Position)
     */
    private Position transformPosition(final int x, final int y) {
        return this.transformPosition(new Position(x, y));
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
