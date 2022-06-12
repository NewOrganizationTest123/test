package com.github.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class ScoreboardFragment extends Fragment {

    View view;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        List<ClientPlayer> players = new ArrayList<>();
        players = GamePlayActivity.getPlayers();

        int i = 0;
        while (i < players.size()) {
            switch (i) {
                case 0:
                    name1.setText(players.get(0).getName());
                    points1.setText(players.get(0).getPoints());
                    i++;
                    break;
                case 1:
                    name2.setText(players.get(1).getName());
                    points2.setText(players.get(1).getPoints());
                    i++;
                    break;
                case 2:
                    name3.setText(players.get(2).getName());
                    points3.setText(players.get(2).getPoints());
                    i++;
                    break;
                case 3:
                    name4.setText(players.get(3).getName());
                    points4.setText(players.get(3).getPoints());
                    i++;
                    break;
                case 4:
                    name5.setText(players.get(4).getName());
                    points5.setText(players.get(4).getPoints());
                    i++;
                    break;
                case 5:
                    name6.setText(players.get(5).getName());
                    points6.setText(players.get(5).getPoints());
                    i++;
                    break;
            }
        }

        return view;
    }

    public void winningplayerhighlighted() {

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

        ClientPlayer winner = GamePlayActivity.getPlayers().get(0);
        for (ClientPlayer p : GamePlayActivity.getPlayers())
            if (Integer.parseInt(p.getPoints()) > Integer.parseInt(winner.getPoints())) winner = p;

        if (GamePlayActivity.getPlayers().indexOf(winner) == 0) {
            points1.setBackgroundColor(android.R.color.holo_green_light);
            points1.setTextColor(android.R.color.black);

        } else if (GamePlayActivity.getPlayers().indexOf(winner) == 1) {
            points2.setBackgroundColor(android.R.color.holo_green_light);
            points2.setTextColor(android.R.color.black);

        } else if (GamePlayActivity.getPlayers().indexOf(winner) == 2) {
            points3.setBackgroundColor(android.R.color.holo_green_light);
            points3.setTextColor(android.R.color.black);

        } else if (GamePlayActivity.getPlayers().indexOf(winner) == 3) {
            points4.setBackgroundColor(android.R.color.holo_green_light);
            points4.setTextColor(android.R.color.black);

        } else if (GamePlayActivity.getPlayers().indexOf(winner) == 4) {
            points5.setBackgroundColor(android.R.color.holo_green_light);
            points5.setTextColor(android.R.color.black);

        } else if (GamePlayActivity.getPlayers().indexOf(winner) == 5) {
            points6.setBackgroundColor(android.R.color.holo_green_light);
            points6.setTextColor(android.R.color.black);
        }
    }
}
