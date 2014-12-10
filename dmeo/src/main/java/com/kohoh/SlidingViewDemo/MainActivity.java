package com.kohoh.SlidingViewDemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.kohoh.SlidingView.SlidingView;


public class MainActivity extends ActionBarActivity {

    int POSITION_TOP = 1;
    int POSITION_BOTTOM = 2;
    int POSITION_LEFT = 3;
    int POSITION_RIGHT = 4;
    private SlidingView slidingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        slidingView = (SlidingView) findViewById(R.id.mSlidingView);
        slidingView.addPosition(POSITION_TOP, 0, 500);
        slidingView.addPosition(POSITION_BOTTOM, 0, -500);
        slidingView.addPosition(POSITION_LEFT, -500, 0);
        slidingView.addPosition(POSITION_RIGHT, 500, 0);

    }
}
