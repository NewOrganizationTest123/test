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

        TextView name1 = view.findViewById(R.id.Player1Name);
        TextView name2 = view.findViewById(R.id.Player2Name);
        TextView name3 = view.findViewById(R.id.Player3Name);
        TextView name4 = view.findViewById(R.id.Player4Name);
        TextView name5 = view.findViewById(R.id.Player5Name);
        TextView name6 = view.findViewById(R.id.Player6Name);


        int i = 0;
        while(i< GamePlayActivity.players.size()){
            switch (i){
                case 0:
                    name1.setText(GamePlayActivity.players.get(0));
                    i++;
                    break;
                case 1:
                    name2.setText(GamePlayActivity.players.get(1));
                    i++;
                    break;
                case 2:
                    name3.setText(GamePlayActivity.players.get(2));
                    i++;
                    break;
                case 3:
                    name4.setText(GamePlayActivity.players.get(3));
                    i++;
                    break;
                case 4:
                    name5.setText(GamePlayActivity.players.get(4));
                    i++;
                    break;
                case 5:
                    name6.setText(GamePlayActivity.players.get(5));
                    i++;
                    break;
            }

        }


        return view;
    }

}