package com.github.wizard;

enum Color {
    RED,
    GREEN,
    BLUE,
    YELLOW
}


public class Card {
    public Color color;
    //+inf:=Zauberer
    public int value;

    public Card(Color color, int value) {
        this.color = color;
        this.value = value;
    }//-1:=Narr

    @Override
    public String toString() {
        return value + "*" + color.name();
    }
}
