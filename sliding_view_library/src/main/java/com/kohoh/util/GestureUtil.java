package com.kohoh.util;

import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by KOHOH1992 on 14-3-9.
 */
public class GestureUtil {

    static public void printEvent(MotionEvent event,String TAG)
    {
        Log.v(TAG, "------------------------------------------");
        int actionMasked= MotionEventCompat.getActionMasked(event);
        switch (actionMasked)
        {
            case MotionEvent.ACTION_DOWN:
                Log.v(TAG,"ACTION_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.v(TAG,"ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.v(TAG,"ACTION_UP");
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.v(TAG,"ACTION_CANCEL");
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.v(TAG,"ACTION_POINTER_DOWN");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.v(TAG,"ACTION_POINTER_UP");
                break;
            default:
                Log.v(TAG,"don't understand");
                break;
        }


        Log.v(TAG,"ActionMasked = "+actionMasked);
        int action=event.getAction();
        Log.v(TAG,"Action = "+action);
        int count= MotionEventCompat.getPointerCount(event);
        int index=MotionEventCompat.getActionIndex(event);
        Log.v(TAG,"ActionIndex = "+index);
        Log.v(TAG,"Count = "+count);


        for(int i=0;i<count ; i++)
        {
            float x=MotionEventCompat.getX(event,i);
            float y=MotionEventCompat.getY(event,i);
            Log.v(TAG,"the pointer"+i+"  x="+x+"  y="+y);
            int id=MotionEventCompat.getPointerId(event,i);
            Log.v(TAG,"the pointer"+i+"  id="+id);
        }
        Log.v(TAG, "------------------------------------------");
    }
}
