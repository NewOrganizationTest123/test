package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class TutorialActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial);

        TextView title = (TextView) findViewById(R.id.titlewindow);
        TextView tv = (TextView) findViewById(R.id.tutorialtext);
        Button btn = (Button) findViewById(R.id.backbtn);
        btn.setOnClickListener(this);

        title.setText("Spielregeln und Vorbereitungen\n");
        StringBuilder sb = new StringBuilder();

        String text = "Für Wizard benötigen Sie mindestens zwei Mitspieler, um auf eine Spielerzahl von 3 bis 6 Leuten zu kommen. Außerdem brauchen Sie einen Stift und natürlich das Kartendeck inklusive Block, auf dem Sie die Spielstände notieren. Sinn des Spiels ist es, in jeder Runde exakt viel Stiche zu machen, wie vor der Runde angesagt. Keine Sorge, in diesem Kapitel erklären wir Ihnen genauer, wie das funktioniert. \n" +
                "•\tIn der ersten Runde teilen Sie jedem Mitspieler eine Karte aus, in der zweiten Runde zwei Karten, in der dritten Runde drei und so weiter - bis am Ende alle Karten im Spiel sind. Eine weitere Karte wird aufgedeckt auf den Kartenstapel gelegt. Die Farbe dieser Karte ist für eine Runde lang Trumpf. \n" +
                "•\tHaben alle Spieler ihre Karten erhalten, sagt jeder Mitspieler im Uhrzeigersinn (der Spieler neben dem Austeilenden beginnt) an, wie viele Stiche er pro Runde machen wird. Diese Ansage ist entscheidend für die Punktevergabe. \n" +
                "•\tEs gibt vier verschiedene Farben: Menschen (blau), Elfen (grün), Zwerge (rot), Riesen (gelb). Von jeder Farbe gibt es 13 nummerierte Karten. Dabei ist die 1 die schwächste und die 13 die stärkste Karte. Zudem gibt es vier Narren und Zauberer. Die Narren sind schwächer als jede 1, die Zauberer stärker als jeder Trumpf und gewinnen den Stich. \n" +
                "•\tDerjenige, der im Uhrzeigersinn neben dem Austeiler sitzt, legt die erste Karte. Dann gilt - ähnlich wie beim Schafkopf - Farbe zugeben. Legt Spieler 1 eine grüne Karte, müssen alle darauffolgenden Spieler auch Grün legen - außer sie haben keine grüne Karte mehr auf der Hand oder legen einen Zauberer oder Narr. \n" +
                "•\tWird ein Zauberer als erste Karte gelegt, wird keine Farbe zugegeben. Beim Narr sagt die zweite Karte die Farbe an. Sind nur grüne Karten im Spiel, sticht die Person mit der höchsten grünen Karte. Ist Trumpf im Spiel, sticht der höchste Trumpf. \n" +
                "•\tSind Zauberer im Spiel, sticht der erste Zauberer, der gelegt wurde. Narren sowie Karten anderer Farben sind wertlos und verlieren den Stich. Wer den Stich gewonnen hat, ist danach an der Reihe, als erstes eine Karte zu legen. \n" +
                "Ansagen und Auswerten \n" +
                "Vor jeder Runde muss,wie schon kurz erwähnt, jeder Mitspieler im Uhrzeigersinn (der Spieler neben dem Austeilenden beginnt) ansagen, wie viele Stiche er pro Runde machen wird. Diese Ansage ist entscheidend für die Punktevergabe.\n" +
                "•\tBehalten Sie mit Ihrer Ansage Recht, bekommen Sie 20 Punkte allein dafür, dass Sie richtig angesagt haben. Sagen Sie also an, keinen Stich zu machen und behalten Recht, so haben Sie 20 Punkte gewonnen, die Sie auf dem Spielblock notieren. \n" +
                "•\tFür jeden Stich, den Sie angesagt haben, erhalten Sie weitere 10 Punkte. Haben Sie also 2 Stiche angesagt und erreicht, bekommen Sie 20 + 10 + 10 Punkte: also 40. \n" +
                "•\tLiegen Sie falsch, werden -10 Punkte pro Stich von Ihrem derzeitigen Spielstand abgezogen. Sagen Sie also 2 Stiche an und machen 4, so erhalten Sie -20 Punkte. \n" +
                "•\tDas Spiel ist beendet, wenn alle 60 Karten im Spiel sind. In der letzten Runde gibt es dabei keinen Trumpf, denn keine Karte bleibt übrig, um als Trumpf aufgedeckt zu werden. Gewonnen hat der Spieler mit der höchsten Punktzahl. \n" +
                "Varianten und Zusatzregelungen\n" +
                "Nun gibt es bei diesem beliebten Kartenspiel nicht nur die Grundregeln, sondern auch Zusatzregeln. Diese Varianten können Sie in Ihr Spiel mit einbeziehen, wenn Sie schon öfters gespielt haben und \"frischen Wind\" in die Wizard-Runde bringen möchten.\n" +
                "•\tHellsehen: In Runde 1 hält jeder Spieler seine Karte aufgedeckt vor die Stirn, ohne sie vorher anzusehen. Sie können zwar nicht ihre eigene Karte, dafür aber die der Mitspieler sehen und so einschätzen, ob Sie den Stich machen werden. \n" +
                "•\tPlus/minus Eins: Bei dieser Variante darf die Anzahl der angesagten Stiche nicht mit der Zahl der möglichen Stiche übereinstimmen. Geht es zum Beispiel in einer Runde um 4 Stiche, dann müssen die Spieler insgesamt mindestens 5 Stiche oder maximal 3 Stiche ansagen. \n" +
                "•\tVerdeckter Tipp: Die Ansage der Stiche verläuft geheim, indem jeder Spieler seinen Tipp auf einen Notizzettel schreibt. Aufgedeckt werden die Ansagen, nachdem jeder getippt hat, aber bevor man zu spielen beginnt. \n" +
                "•\tGeheime Vorhersage: Alle Spieler schreiben ihre Ansage auf einen Zettel. Erst wenn sowohl das Vorhersagen, als auch die Runde vorbei sind, werden die Tipps offenbart - so bleibt das Spiel unvoreingenommen. \n" +
                "\n";

        sb.append(text);
        tv.setText(sb.toString());
    }

    @Override
    public void onClick(View view) {
        startActivity(new Intent(TutorialActivity.this, MenuActivity.class));
    }
}