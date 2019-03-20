package com.example.zhien.oneapplecation;

import android.support.v4.app.Fragment;

/**
 * The main class of the program
 */

public class MainActivity extends FragmentHostActivity {

//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .add(android.R.id.content, new MainFragment()).commit();
//        }
//    }
//
//

    @Override
    protected Fragment getFragment() {
        return BLEnoBC.newInstance();
    }
}