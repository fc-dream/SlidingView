package com.kohoh.SlidingView;

/**
 * 向量
 */
public class Vector {
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
