package com.example.smartchatbot.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartchatbot.R;
import com.example.smartchatbot.auth.LoginActivity;
import com.example.smartchatbot.auth.SignupActivity;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout slideHandle, slideButton;
    private TextView slideText;

    private float dX;
    private int maxRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        slideHandle = findViewById(R.id.slideHandle);
        slideButton = findViewById(R.id.slideButton);
        slideText = findViewById(R.id.slideText);

        // Maximum right movement for handle
        slideButton.post(() -> maxRight = slideButton.getWidth() - slideHandle.getWidth());

        slideHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    // Hide text as soon as sliding starts
                    slideText.setVisibility(View.INVISIBLE);
                    break;

                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX;
                    if (newX >= 0 && newX <= maxRight) {
                        v.setX(newX);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (v.getX() >= maxRight * 0.85) {
                        // Completed slide → Go to SignupActivity
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Reset handle & show text again
                        v.animate().x(0).setDuration(200).start();
                        slideText.setVisibility(View.VISIBLE);
                    }
                    break;
            }
            return true;
        });
    }
}
