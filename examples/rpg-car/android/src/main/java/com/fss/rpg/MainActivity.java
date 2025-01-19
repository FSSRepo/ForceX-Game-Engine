package com.fss.rpg;

import android.os.*;
import com.forcex.android.ForceXApp;

public class MainActivity extends ForceXApp
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(new RPGCarGame(),true);
    }
}
