package com.kohoh.SlidingView;

/**
 * 封装位置的坐标信息
 */
public class Position {
    /**
     * x轴的坐标
     */
    private int x;
    /**
     * y轴的坐标
     */
    private int y;

    /**
     * 位置对应的Id，用于识别该位置。
     */
    private int id;

    /**
     * 构建一个无id的位置
     * <p>准确的说会设置一个默认的值作为id。以该值为id的位置将无法被添加到{@link PositionManager}中
     * </p>
     */
    public Position(int x, int y) {
        this(Integer.MIN_VALUE, x, y);
    }

    /**
     * 构建一个位置
     */
    public Position(int id, int x, int y) {
        this.x=x;
        this.y=y;
        this.id = id;
    }

    /**
     * 计算两坐标之间的距离
     *
     * @param c1 坐标1
     * @param c2 坐标2
     * @return 坐标c1与坐标c2间的距离
     */
    static public float computeDistance(Position c1, Position c2) {
        return (float) Math.sqrt(Math.pow((c1.x - c2.x), 2) + Math.pow((c1.y - c2.y), 2));
    }

    /**
     * 获取位置对应的id
     */
    public int getId() {
        return id;
    }

    /**
     * 设置位置对应的id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * 设置位置对应的x轴的坐标
     */
    public int getX() {
        return x;
    }

    /**
     * 获取位置对应的x轴的坐标
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * 设置位置对应的y轴的坐标
     */
    public int getY() {
        return y;
    }

    /**
     * 获取位置对应的y轴的坐标
     */
    public void setY(int y) {
        this.y = y;
    }
}
