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

    private int id;

    /**
     * 构建一个坐标，并设置他的值。
     *
     * @param x x轴的坐标
     * @param y y轴的坐标
     */
    public Position(int x, int y) {
        this(Integer.MIN_VALUE, x, y);
    }

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
