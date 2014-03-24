package com.kohoh.SlidingView;

import com.kohoh.Exception.IllegalCoordinate;

/**
 * Created by kohoh on 14-3-24.
 */
public class Position
{
    int id;
    int x;
    int y;

    public Position(int id,int x,int y)
    {
        this.id=id;
        this.x = x;
        this.y = y;
    }

    public Position(int id,Coordinate coordinate) {
        if(coordinate==null)
        {
            throw new IllegalCoordinate("coordinate is null");
        }

        this.id = id;
        this.x=coordinate.x;
        this.y=coordinate.y;
    }

    public Coordinate getCoordinate() {
        return new Coordinate(this.x, this.y);
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
