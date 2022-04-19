package com.github.wizard;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.wizard.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Button start_game_button = findViewById(R.id.start_game_button);
        Button join_game_button = findViewById(R.id.join_game_button);

        start_game_button.setOnClickListener(e->{
            Intent intent = new Intent(this, MainActivity.class);
            String message = "new game";
            intent.putExtra("com.example.wizard.MESSAGE", message);
            startActivity(intent);
        });

        join_game_button.setOnClickListener(e->{
            Intent intent = new Intent(this, MainActivity.class);
            String message = "join game";
            intent.putExtra(EXTRA_MESSAGE, message);
            startActivity(intent);
        });

    }


}
