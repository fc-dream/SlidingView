package com.kohoh.SlidingView;

import android.util.SparseArray;

import com.kohoh.Exception.IllegalPosition;

import java.util.Iterator;

/**
 * 位置集合。
 * <p>该类内部提供了一个Map来管理所有的位置信息。该Map以位置对应的Id作为key，位置对应的坐标作为
 * value。你可以通过{@link #addPosition(int, int, int)}和{@link #removePositionById(int)}来增加和移
 * 除位置。注意的是，该Map中默认有一个初始位
 * 置。其位置对应的Id为{@link #POSITION_INITIAL},因此请不要添加以-1为Id的位置。初始位置的坐标只
 * 能在构建PositionManager时设置 </p>
 */
public class PositionManager {

    /**
     * 构建一个PositionManager，并且设置初始位置为(0,0)
     */
    public PositionManager() {
        this(new Position(0, 0));
    }

    /**
     * 构建一个PositionManager,并设置初始位置
     *
     * @param initialPosition 初始位置对应的坐标
     */
    public PositionManager(Position initialPosition) {
        positionSparseArray = new SparseArray<Position>();
        initialPosition.setId(POSITION_INITIAL);
        addPosition(initialPosition);
    }

    public PositionManager(final int initialX, final int initialY) {
        this(new Position(POSITION_INITIAL, initialX, initialY));
    }

    public boolean isIdExisted(int id) {
        if (positionSparseArray == null) {
            throw new RuntimeException("positionSparesArray is null");
        }

        return (positionSparseArray.indexOfKey(id) >= 0);
    }

    /**
     * 初始位置对应的Id
     */
    public static final int POSITION_INITIAL = -1;
    private SparseArray<Position> positionSparseArray;
    private int leftBound = Integer.MAX_VALUE;
    private int topBound = Integer.MIN_VALUE;
    private int rightBound = Integer.MIN_VALUE;
    private int bottomBound = Integer.MAX_VALUE;
    private int currentPositionId = POSITION_INITIAL;
    private int customLeftBound = Integer.MAX_VALUE;
    private int customRightBound = Integer.MIN_VALUE;
    private int customTopBound = Integer.MIN_VALUE;
    private int customBottomBound = Integer.MAX_VALUE;

    /**
     * 获取当前位置
     *
     * @return 当前位置
     */
    public int getCurrentPositionId() {
        return currentPositionId;
    }

    /**
     * 设置当前位置
     *
     * @param currentPosition 当前位置
     */
    public boolean setCurrentPositionId(int currentPosition) {
        if (!isIdExisted(currentPositionId)) {
            return false;
        } else {
            this.currentPositionId = currentPosition;
            return true;
        }
    }

    public boolean addPosition(int id, int x, int y) {
        return addPosition(new Position(id, x, y));
    }

    public boolean addPosition(Position position) {
        if (position == null) {
            throw new IllegalPosition("position is null");
        }

        if (isIdExisted(position.getId())) {
            return false;
        }

        if (position.getId() == Integer.MIN_VALUE) {
            return false;
        }

        positionSparseArray.put(position.getId(), position);
        setBound();
        return true;
    }

    /**
     * 移除一个位置
     *
     * @param positionId 位置对应的Id
     * @return true 成功移除位置
     */
    public boolean removePositionById(int positionId) {
        if (positionId == POSITION_INITIAL) {
            throw new IllegalPosition("can't remove initial position");
        }

        if (isIdExisted(positionId)) {
            return false;
        }
        positionSparseArray.remove(positionId);
        setBound();
        return true;
    }

    /**
     * 获取对应Id的位置坐标
     *
     * @param positionId 位置对应的Id
     * @return 位置对应的坐标
     */
    public Position findPositionById(int positionId) {
        return positionSparseArray.get(positionId);
    }

    /**
     * 获取Iterator
     */
    Iterator<Position> getIterator() {
        if (positionSparseArray == null) {
            throw new RuntimeException("positionSparseArray is null");
        }

        return new Iterator<Position>() {

            int cursor = 0;
            int size = positionSparseArray.size();

            @Override
            public boolean hasNext() {
                return (cursor < size);
            }

            @Override
            public Position next() {
                Position position = positionSparseArray.valueAt(cursor);
                cursor++;
                return position;
            }

            @Override
            public void remove() {
                positionSparseArray.remove(positionSparseArray.keyAt(cursor));
            }
        };
    }

    /**
     * 根据所有位置的坐标，设置所能到达的上下左右的最大范围
     */
    private void setBound() {
        this.leftBound = Integer.MAX_VALUE;
        this.topBound = Integer.MIN_VALUE;
        this.rightBound = Integer.MIN_VALUE;
        this.bottomBound = Integer.MAX_VALUE;

        Iterator iterator = getIterator();
        while (iterator.hasNext()) {
            Position position = (Position) iterator.next();

            this.leftBound = Math.min(this.leftBound, position.getX());
            this.rightBound = Math.max(this.rightBound, position.getX());
            this.topBound = Math.max(this.topBound, position.getY());
            this.bottomBound = Math.min(this.bottomBound, position.getY());
        }

        this.leftBound = Math.min(this.leftBound, customLeftBound);
        this.rightBound = Math.max(this.rightBound, customRightBound);
        this.topBound = Math.max(this.topBound, customTopBound);
        this.bottomBound = Math.min(this.bottomBound, customBottomBound);
    }

    public void setBound(int left, int top, int right, int bottom) {
        this.customLeftBound = left;
        this.customRightBound = right;
        this.customTopBound = top;
        this.customBottomBound = bottom;

        setBound();
    }

    public void setLeftBound(int bound) {
        setBound(bound, customTopBound, customRightBound, customBottomBound);
    }

    public int getLeftBound() {
        return leftBound;
    }

    public int getTopBound() {
        return topBound;
    }

    public void setTopBound(int bound) {
        setBound(customLeftBound, bound, customRightBound, customBottomBound);
    }

    public int getRightBound() {
        return rightBound;
    }

    public void setRightBound(int bound) {
        setBound(customLeftBound, customTopBound, bound, customBottomBound);
    }

    public int getBottomBound() {
        return bottomBound;
    }

    public void setBottomBound(int bound) {
        setBound(customLeftBound, customTopBound, customRightBound, bound);
    }

}
