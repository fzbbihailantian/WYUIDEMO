package com.fzb.ballnaimation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.fzb.ballnaimation.view.LoadView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LoadView loadView = findViewById(R.id.loadview);
        loadView.startAllAnimator();
    }
}
