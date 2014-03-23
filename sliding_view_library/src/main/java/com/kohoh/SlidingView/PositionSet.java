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

    public int getLeftBound() {
        return leftBound;
    }

    public int getTopBound() {
        return topBound;
    }

    public int getRightBound() {
        return rightBound;
    }

    public int getBottomBound() {
        return bottomBound;
    }


}
