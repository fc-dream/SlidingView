package com.kohoh.Exception;

/**
 * Created by kohoh on 14-3-24.
 */
public class IllegalCoordinate extends RuntimeException {

    public IllegalCoordinate()
    {
        super();
    }

    public IllegalCoordinate(String message)
    {
        super(message);
    }
}
