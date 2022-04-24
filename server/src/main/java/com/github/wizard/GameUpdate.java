package com.github.wizard;

public interface GameUpdate {
    public void OnGameBoardUpdate(GameRound round);

    public void CardPlayRequest();

    /**
     * The client should then display a popup displaying who made the stich and how much it was worth
     *
     * @param player
     * @param value
     */
    public void OnStichMade(Player player, int value);

    /**
     * notifies the players that color c was selected as trumpf
     *
     * @param c
     */
    public void OnTrumpfSelected(Color c);

    void GetEstimate();

    void OnRoundFinished(int round);
}
