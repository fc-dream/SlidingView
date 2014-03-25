package com.kohoh.SlidingView;

import android.content.Context;
import android.util.Log;
import android.view.ViewConfiguration;

import com.kohoh.Exception.IllegalPosition;

import java.util.Iterator;

/**
 * 通过提供给一系列的方法来协助管理位置信息。
 * <p>此外PositionManager还提供了一些列的方法，帮助你到达
 * 到达目标位置。你可以通过{@link #guessPosition(float, float, int, int, int, int)}
 * {@link #guessPosition(int, int)} {@link #guessPosition(int, int, int, int)}得到你的目标位置。
 * </p>
 */
class PositionHelper {
    private PositionManager positionManager;
    private final boolean DEBUG = true;
    private final String TAG = "PositionHelper";
    private int flingDistance;
    private int minimumVelocity;
    private static final int MIN_DISTANCE_FOR_FLING = 25; // in dip
    private Context context;

    public PositionHelper(PositionManager positionManager, Context context) {
        this.context = context;
        this.positionManager = positionManager;
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        final float density = context.getResources().getDisplayMetrics().density;
        flingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
    }

    /**
     * 判断当前位置是否处于所有位置之一
     *
     * @param currentX 当前位置的x轴坐标
     * @param currentY 当前位置的y轴坐标
     * @return true 处于所有位置之一
     */
    public boolean isAtPosition(int currentX, int currentY) {
        return isAtPosition(new Position(currentX, currentY));
    }

    /**
     * 判断当前位置是否处于所有位置之一
     *
     * @param currentPosition 当前的坐标
     * @return true 处于所有位置之一
     */
    public boolean isAtPosition(final Position currentPosition) {
        if (currentPosition == null) {
            throw new IllegalPosition("position is null");
        }

        Iterator iterator = this.positionManager.getIterator();
        while (iterator.hasNext()) {
            Position position = (Position) iterator.next();
            if (position.getX() == currentPosition.getX() && position.getY() == currentPosition.getY()) {
                return true;
            }
        }
        return false;
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
        return this.guessPosition(new Position(currentX, currentY));
    }

    /**
     * 根据当前的位置，猜测你想要到达的位置
     *
     * @param currentPosition 当前的坐标
     * @return 猜测想要到达的位置对应的Id
     * @see #guessPosition(int, int)
     */
    public int guessPosition(final Position currentPosition) {
        if (currentPosition == null) {
            throw new IllegalPosition("position is null");
        }
        float min = Float.MAX_VALUE;
        Iterator iterator = this.positionManager.getIterator();
        int guess = PositionManager.POSITION_INITIAL;
        while (iterator.hasNext()) {
            Position position = (Position) iterator.next();
            float distance = Position.computeDistance(position, currentPosition);
            if (distance < min) {
                min = distance;
                guess = position.getId();
            }
        }
        return guess;
    }

    /**
     * 猜测目标位置
     *
     * @param endX   滑动的起始位置的x轴坐标
     * @param startY 滑动的起始位置的y轴坐标
     * @param endX   当前位置的x轴坐标
     * @param endY   当前位置的y轴坐标
     * @return 猜测想要到达的位置对应的Id
     * @see #guessPosition(Position, Position);
     */
    public int guessPosition(final int startX, final int startY
            , final int endX, final int endY) {
        return guessPosition(new Position(startX, startY), new Position(endX, endY));
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
    public int guessPosition(final Position start, final Position end) {
        if (start == null || end == null) {
            throw new IllegalPosition("position id null");
        }
        //精度取5度
        final float precision = (float) Math.abs(Math.cos(Math.PI / 2) - Math.cos(Math.PI / 18 * 19));
        int guess = positionManager.getCurrentPositionId();
        Vector vector1 = new Vector(end.getX()-start.getX(), end.getY()-start.getY());
        float maxCos = Float.MIN_VALUE;
        float minDis = Float.MAX_VALUE;
        Iterator iterator = this.positionManager.getIterator();
        while (iterator.hasNext()) {
            Position position = (Position) iterator.next();
            Vector vector2 = new Vector(position.getX()-start.getX(), position.getY()-start.getY());
            float cos = Vector.computeCos(vector1, vector2);
            //判断是否是当前位置
            if (position.getId() == positionManager.getCurrentPositionId()) {
                continue;
            }
            //判断是否在0度到45度之间
            if (!(Math.cos(Math.PI / 4) <= cos && cos <= Math.cos(0))) {
                continue;
            }
            //判断cos是否大于maxCos
            if ((cos - maxCos) > precision) {
                maxCos = cos;
                minDis = Position.computeDistance(end, position);
                guess = position.getId();
            }
            //判断cos是否等于maxCos且距离更小
            if (Math.abs(cos - maxCos) < precision && Position.computeDistance(end, position) < minDis) {
                maxCos = cos;
                minDis = Position.computeDistance(end, position);
                guess = position.getId();
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
     * @param startX    滑动起始位置的x轴坐标
     * @param startY    滑动起始位置的y轴坐标
     * @param endX      当前位置的x轴坐标
     * @param endY      当前位置的y轴坐标
     * @return 决定要到达的目标位置所对应的Id
     */
    public int guessPosition(float velocityX, float velocityY, int startX, int startY, int endX,
                             int endY) {
        return guessPosition((float) Math.sqrt(Math.pow(velocityX, 2) + Math.pow(velocityY, 2)), new
                Position(startX, startY), new Position(endX, endY));
    }

    public int guessPosition(float velocity, Position start, Position end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("position is invalid");
        }

        int desire = guessPosition(start, end);
        int guess = this.positionManager.getCurrentPositionId();

        Position position = this.positionManager.findPositionById(guess);
        float currentDistance = Position.computeDistance(start, end);
        float totalDistance = Position.computeDistance(start, position);

        if (currentDistance > flingDistance && velocity > minimumVelocity) {
            guess = desire;
        } else if (Math.round(currentDistance / totalDistance) >= 1) {
            guess = desire;
        }

        if (DEBUG) {
            Log.v(TAG, "geussPosition=" + guess);
        }

        return guess;
    }
}
