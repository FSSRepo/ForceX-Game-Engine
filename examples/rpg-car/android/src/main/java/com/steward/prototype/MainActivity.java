package com.steward.prototype;

import android.os.*;
import com.forcex.android.ForceXApp;

public class MainActivity extends ForceXApp
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		addFolder("proto");
        initialize(new PrototypeUNI(),true);
    }
}
