package com.kohoh.Exception;

/**
 * Created by kohoh on 14-3-24.
 */
public class IllegalPosition extends RuntimeException {

    public IllegalPosition()
    {
        super();
    }

    public IllegalPosition(String message)
    {
        super(message);
    }
}
