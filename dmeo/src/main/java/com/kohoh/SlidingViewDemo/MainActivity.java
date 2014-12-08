package com.kohoh.SlidingViewDemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.kohoh.SlidingView.SlidingView;


public class MainActivity extends ActionBarActivity {

    private SlidingView slidingView;
    int POSITION_TOP = 1;
    int POSITION_BOTTOM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        slidingView = (SlidingView) findViewById(R.id.mSlidingView);
        slidingView.addPosition(POSITION_TOP, 0, 500);
        slidingView.addPosition(POSITION_BOTTOM, 0, -500);

    }
}
