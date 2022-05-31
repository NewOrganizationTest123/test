package com.github.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class ScoreboardFragment extends Fragment {

    View view;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_scoreboard, container, false);

        TextView name=view.findViewById(R.id.Namestext);//todo @silvio this is how to access something in the layout
        for (String player : GamePlayActivity.players) {
            name.setText(player);//todo @silvio this is how you loop over the players array to fill your view...
        }

        return view;
    }

}