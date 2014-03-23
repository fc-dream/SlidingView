package com.kohoh.SlidingView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 位置集合。
 * <p>该类内部提供了一个Map来管理所有的位置信息。该Map以位置对应的Id作为key，位置对应的坐标作为
 * value。你可以通过{@link #addPosition(int, Coordinate)}和{@link #removePosition(int)}来增加和移
 * 除位置。注意的是，该Map中默认有一个初始位
 * 置。其位置对应的Id为{@link #POSITION_INITIAL},因此请不要添加以-1为Id的位置。初始位置的坐标只
 * 能在构建PositionManager时设置 </p>
 */
public class PositionSet {

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
    private Map<Integer, Coordinate> coordinateMap;
    private int leftBound = Integer.MAX_VALUE;
    private int topBound = Integer.MIN_VALUE;
    private int rightBound = Integer.MIN_VALUE;
    private int bottomBound = Integer.MAX_VALUE;
    private Integer currentPosition = POSITION_INITIAL;
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
        return currentPosition;
    }

    /**
     * 设置当前位置
     *
     * @param currentPosition 当前位置
     */
    public void setCurrentPositionId(int currentPosition) {
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
     * 获取对应Id的位置坐标
     *
     * @param positionId 位置对应的Id
     * @return 位置对应的坐标
     */
    public Coordinate getPosition(int positionId) {
        return coordinateMap.get(positionId);
    }

    /**
     * 获取Iterator
     */
    Iterator<Map.Entry<Integer, Coordinate>> getIterator() {
        return this.coordinateMap.entrySet().iterator();
    }

    /**
     * 根据所有位置的坐标，设置所能到达的上下左右的最大范围
     */
    private void setBound() {
        this.leftBound = Integer.MAX_VALUE;
        this.topBound = Integer.MIN_VALUE;
        this.rightBound = Integer.MIN_VALUE;
        this.bottomBound = Integer.MAX_VALUE;

        Iterator iterator = coordinateMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Coordinate> entry = (Map.Entry<Integer, Coordinate>) iterator.next();
            Coordinate coordinate = entry.getValue();

            this.leftBound = Math.min(this.leftBound, coordinate.x);
            this.rightBound = Math.max(this.rightBound, coordinate.x);
            this.topBound = Math.max(this.topBound, coordinate.y);
            this.bottomBound = Math.min(this.bottomBound, coordinate.y);
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
