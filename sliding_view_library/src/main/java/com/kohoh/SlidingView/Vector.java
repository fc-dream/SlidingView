package com.kohoh.SlidingView;

/**
 * 向量
 */
public class Vector {
    /**
     * 向量在x轴上的坐标
     */
    private int x;
    /**
     * 向量在y轴上的坐标
     */
    private int y;
    /**
     * 向量的模
     */
    private float length;

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

    /**
     * 获取向量在x轴上的坐标
     */
    public int getX() {
        return x;
    }

    /**
     * 设置向量在x轴上的坐标
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * 获取向量在y轴上的坐标
     */
    public int getY() {
        return y;
    }

    /**
     * 设置向量在y轴上的坐标
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * 获取向量的模
     */
    public float getLength() {
        return length;
    }

    /**
     * 设置向量的模
     */
    public void setLength(float length) {
        this.length = length;
    }
}
