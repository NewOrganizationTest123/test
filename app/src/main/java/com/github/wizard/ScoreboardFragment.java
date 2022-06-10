package com.github.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.w3c.dom.Text;

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
        TextView points1 = view.findViewById(R.id.Player1Points);
        TextView points2 = view.findViewById(R.id.Player2Points);
        TextView points3 = view.findViewById(R.id.Player3Points);
        TextView points4 = view.findViewById(R.id.Player4Points);
        TextView points5 = view.findViewById(R.id.Player5Points);
        TextView points6 = view.findViewById(R.id.Player6Points);
        TextView round = view.findViewById(R.id.Roundscounter);

        int points1int = Integer.parseInt(points1.getText().toString());
        int points2int = Integer.parseInt(points2.getText().toString());
        int points3int = Integer.parseInt(points3.getText().toString());
        int points4int = Integer.parseInt(points4.getText().toString());
        int points5int = Integer.parseInt(points5.getText().toString());
        int points6int = Integer.parseInt(points6.getText().toString());

        int i = 0;
        while (i < GamePlayActivity.getPlayers().size()) {
            switch (i) {
                case 0:
                    name1.setText(GamePlayActivity.getPlayers().get(0).getName());
                    points1.setText(GamePlayActivity.getPlayers().get(0).getPoints());
                    i++;
                    break;
                case 1:
                    name2.setText(GamePlayActivity.getPlayers().get(1).getName());
                    points2.setText(GamePlayActivity.getPlayers().get(1).getPoints());
                    i++;
                    break;
                case 2:
                    name3.setText(GamePlayActivity.getPlayers().get(2).getName());
                    points3.setText(GamePlayActivity.getPlayers().get(2).getPoints());
                    i++;
                    break;
                case 3:
                    name4.setText(GamePlayActivity.getPlayers().get(3).getName());
                    points4.setText(GamePlayActivity.getPlayers().get(3).getPoints());
                    i++;
                    break;
                case 4:
                    name5.setText(GamePlayActivity.getPlayers().get(4).getName());
                    points5.setText(GamePlayActivity.getPlayers().get(4).getPoints());
                    i++;
                    break;
                case 5:
                    name6.setText(GamePlayActivity.getPlayers().get(5).getName());
                    points6.setText(GamePlayActivity.getPlayers().get(5).getPoints());
                    i++;
                    break;
            }

        }

        return view;
    }

    public void winningplayerhighlighted(){

        TextView name1 = view.findViewById(R.id.Player1Name);
        TextView name2 = view.findViewById(R.id.Player2Name);
        TextView name3 = view.findViewById(R.id.Player3Name);
        TextView name4 = view.findViewById(R.id.Player4Name);
        TextView name5 = view.findViewById(R.id.Player5Name);
        TextView name6 = view.findViewById(R.id.Player6Name);
        TextView points1 = view.findViewById(R.id.Player1Points);
        TextView points2 = view.findViewById(R.id.Player2Points);
        TextView points3 = view.findViewById(R.id.Player3Points);
        TextView points4 = view.findViewById(R.id.Player4Points);
        TextView points5 = view.findViewById(R.id.Player5Points);
        TextView points6 = view.findViewById(R.id.Player6Points);
        TextView round = view.findViewById(R.id.Roundscounter);

        int points1int = Integer.parseInt(points1.getText().toString());
        int points2int = Integer.parseInt(points2.getText().toString());
        int points3int = Integer.parseInt(points3.getText().toString());
        int points4int = Integer.parseInt(points4.getText().toString());
        int points5int = Integer.parseInt(points5.getText().toString());
        int points6int = Integer.parseInt(points6.getText().toString());
        int max = Math.max(points1int, Math.max(points2int, Math.max(points3int, Math.max(points4int, Math.max(points5int, points6int)))));


        if (max == points1int) {
            points1.setBackgroundColor(android.R.color.holo_green_light);
            points1.setTextColor(android.R.color.black);

        } else if (max == points2int) {
            points2.setBackgroundColor(android.R.color.holo_green_light);
            points2.setTextColor(android.R.color.black);

        } else if (max == points3int) {
            points3.setBackgroundColor(android.R.color.holo_green_light);
            points3.setTextColor(android.R.color.black);

        } else if (max == points4int) {
            points4.setBackgroundColor(android.R.color.holo_green_light);
            points4.setTextColor(android.R.color.black);

        } else if (max == points5int) {
            points5.setBackgroundColor(android.R.color.holo_green_light);
            points5.setTextColor(android.R.color.black);

        } else if (max == points6int) {
            points6.setBackgroundColor(android.R.color.holo_green_light);
            points6.setTextColor(android.R.color.black);

        }
    }

}
