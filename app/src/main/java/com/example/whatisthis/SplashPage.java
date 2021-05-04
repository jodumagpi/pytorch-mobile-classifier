package com.example.whatisthis;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import gr.net.maroulis.library.EasySplashScreen;

public class SplashPage extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EasySplashScreen config = new EasySplashScreen(SplashPage.this)
                .withFullScreen()
                .withTargetActivity(MainActivity.class)
                .withSplashTimeOut(1000)
                .withBackgroundResource(R.mipmap.splash);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View mySplashScreen = config.create();
        setContentView(mySplashScreen);

    }
}
