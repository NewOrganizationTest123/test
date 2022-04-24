package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity implements View.OnClickListener {

    Button spielen;
    Button einstellungen;
    Button anleitung;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);

        spielen = (Button) findViewById(R.id.spielbutton);
        einstellungen = (Button) findViewById(R.id.einstellungbutton);
        anleitung = (Button) findViewById(R.id.anleitungbutton);
        spielen.setOnClickListener(this);
        einstellungen.setOnClickListener(this);
        anleitung.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.spielbutton:
                startActivity(new Intent(MenuActivity.this, SpielnameActivity.class));
                break;
            case R.id.einstellungbutton:
                startActivity(new Intent(MenuActivity.this, EinstellungenActivity.class));
                break;
            case R.id.anleitungbutton:
                startActivity(new Intent(MenuActivity.this, AnleitungActivity.class));
                break;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
