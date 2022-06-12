package com.github.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class ScoreboardFragment extends Fragment {

    View view;

    private final int[] playerNameIds = {
        R.id.Player1Name,
        R.id.Player2Name,
        R.id.Player3Name,
        R.id.Player4Name,
        R.id.Player5Name,
        R.id.Player6Name
    };
    private final int[] playerPointIds = {
        R.id.Player1Points,
        R.id.Player2Points,
        R.id.Player3Points,
        R.id.Player4Points,
        R.id.Player5Points,
        R.id.Player6Points
    };
    private int roundId = R.id.Roundscounter;

    private List<TextView> playerNameViews;
    private List<TextView> playerPointViews;

    private List<TextView> getViewsFromIds(int[] ids) {
        List<TextView> views = new ArrayList<>(6);
        for (int id : ids) {
            views.add(view.findViewById(id));
        }
        return views;
    }

    private void setupPlayerNameViews() {
        this.playerNameViews = getViewsFromIds(playerNameIds);
    }

    private void setupPlayerPointViews() {
        this.playerPointViews = getViewsFromIds(playerPointIds);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_scoreboard, container, false);

        setupPlayerNameViews();
        setupPlayerPointViews();

        List<ClientPlayer> players = GamePlayActivity.getPlayers();

        for (int i = 0; i < players.size(); i++) {
            playerNameViews.get(i).setText(players.get(i).getName());
            playerPointViews.get(i).setText(players.get(i).getPoints());
        }

        return view;
    }

    public void winningplayerhighlighted() {

        // ensure that the views are fetched
        setupPlayerPointViews();

        ClientPlayer winner = GamePlayActivity.getPlayers().get(0);
        for (ClientPlayer p : GamePlayActivity.getPlayers()) {
            if (Integer.parseInt(p.getPoints()) > Integer.parseInt(winner.getPoints())) winner = p;
        }

        int winnerIndex = GamePlayActivity.getPlayers().indexOf(winner);
        TextView winnerPointView = playerPointViews.get(winnerIndex);

        winnerPointView.setBackgroundColor(
                ContextCompat.getColor(getContext(), android.R.color.holo_green_light));
    }
}
