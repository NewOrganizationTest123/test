package com.github.wizard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PlayersRecyclerviewAdapter extends RecyclerView.Adapter<PlayersRecyclerviewAdapter.ViewHolder> {

    private ArrayList<String> players;
    private LayoutInflater layoutInflater;
    private ItemClickListener itemClickListener;
    public String selectedPlayer;
    //public int name_index = 0;

    PlayersRecyclerviewAdapter(Context context, ArrayList<String> players) {
        this.layoutInflater = LayoutInflater.from(context);
        this.players = players;
        selectedPlayer=null;
        //name_index=names.size()-1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = layoutInflater.inflate(R.layout.players_recyclerview_textfield, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        String playername = players.get(position);
        viewHolder.playername_holder.setText(playername);
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        Button playername_holder;

        ViewHolder(View view) {
            super(view);
            playername_holder = view.findViewById(R.id.cheating_player_button);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    private String getPlayer(int id) {
        return players.get(id);
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
