package com.kohoh.SlidingView;

import android.util.SparseArray;

import com.kohoh.Exception.IllegalPosition;

import java.util.Iterator;

public class PositionManager {

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

    /**
     * 构建一个PositionManager,并设置初始位置
     *
     * @param initialX 初始位置的x轴坐标
     * @param initialY 初始位置的y轴坐标
     */
    public PositionManager(final int initialX, final int initialY) {
        this(new Position(POSITION_INITIAL, initialX, initialY));
    }

    /**
     * 判断该id是否已经被占用
     *
     * @return true 已经被占用
     */
    public boolean isIdExisted(int id) {
        if (positionSparseArray == null) {
            throw new RuntimeException("positionSparesArray is null");
        }

        return (positionSparseArray.indexOfKey(id) >= 0);
    }


    /**
     * 获取当前位置所对应的id
     *
     * @return 当前位置所对应的id
     */
    public int getCurrentPositionId() {
        return currentPositionId;
    }

    /**
     * 设置当前位置所对应的id
     *
     * @param currentPositionId 当前位置所对应的id
     */
    public boolean setCurrentPositionId(int currentPositionId) {
        if (!isIdExisted(this.currentPositionId)) {
            return false;
        } else {
            this.currentPositionId = currentPositionId;
            return true;
        }
    }

    /**
     * 增加一个位置
     *
     * @param id 位置对应的id
     * @param x  位置对应的x轴的坐标
     * @param y  位置对应的y轴的坐标
     * @return false 增加失败
     */
    public boolean addPosition(int id, int x, int y) {
        return addPosition(new Position(id, x, y));
    }

    /**
     * 增加一个位置
     *
     * @return false 增加失败
     */
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
     * 根据位置对应的id移除一个位置
     *
     * @param positionId 位置对应的Id
     * @return false 移除失败
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
     * 根据位置对应的id获取位置
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
     * 设置所能到达的上下左右的最大范围
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

    /**
     * 设置所能到达的上下左右的最大范围
     */
    public void setBound(int left, int top, int right, int bottom) {
        this.customLeftBound = left;
        this.customRightBound = right;
        this.customTopBound = top;
        this.customBottomBound = bottom;

        setBound();
    }

    /**
     * 设置左侧最大范围
     */
    public void setLeftBound(int bound) {
        setBound(bound, customTopBound, customRightBound, customBottomBound);
    }

    /**
     * 获取左侧最大范围
     */
    public int getLeftBound() {
        return leftBound;
    }

    /**
     * 获取上侧最大范围
     */
    public int getTopBound() {
        return topBound;
    }

    /**
     * 设置上侧最大范围
     */
    public void setTopBound(int bound) {
        setBound(customLeftBound, bound, customRightBound, customBottomBound);
    }

    /**
     * 获取右侧最大范围
     * @return
     */
    public int getRightBound() {
        return rightBound;
    }

    /**
     * 设置右侧最大范围
     */
    public void setRightBound(int bound) {
        setBound(customLeftBound, customTopBound, bound, customBottomBound);
    }

    /**
     * 获取下侧最大范围
     */
    public int getBottomBound() {
        return bottomBound;
    }

    /**
     * 设置下侧最大范围
     */
    public void setBottomBound(int bound) {
        setBound(customLeftBound, customTopBound, customRightBound, bound);
    }
}
