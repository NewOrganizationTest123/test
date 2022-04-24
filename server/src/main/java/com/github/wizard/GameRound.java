package com.github.wizard;

public class GameRound {
    public int estimates[] = new int[ServerMain.MAX_PLAYERS];
    public int stiche[] = new int[ServerMain.MAX_PLAYERS];
    public int valuesOfStiche[] = new int[ServerMain.MAX_PLAYERS];
    public Stich cardsInTheMiddle;
    public Color trumpf;

    public GameRound(Color trumpf) {
        this.trumpf = trumpf;
        cardsInTheMiddle = new Stich(trumpf);
    }

    /**
     * play the gicen card
     *
     * @param card
     * @param cardsToPlay how many cards there should be to call the stich complete
     * @return true if the stich is done
     */
    public boolean PlayCard(Card card, byte cardsToPlay, Player player) {
        cardsInTheMiddle.playCArd(card, player);
        return cardsInTheMiddle.cardsCounter == cardsToPlay;
    }

}
