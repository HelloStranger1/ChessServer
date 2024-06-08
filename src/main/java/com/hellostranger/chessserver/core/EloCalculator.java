package com.hellostranger.chessserver.core;

public class EloCalculator {
    public final static int kFactor = 32;

    public static int calculateEloAfterWin(int elo1, int elo2) {
        double expectedScore1 = 1.0 / (1 + Math.pow(10, (elo2 - elo1) / 400.0));
        int eloChange = (int) Math.round(kFactor * (1 - expectedScore1));
        return elo1 + eloChange;
    }

    public static int calculateEloAfterLoss(int elo1, int elo2) {
        double expectedScore1 = 1.0 / (1 + Math.pow(10, (elo2 - elo1) / 400.0));
        int eloChange = (int) Math.round(kFactor * (0 - expectedScore1));
        return elo1 + eloChange;
    }

    public static int calculateEloAfterDraw(int elo1, int elo2) {
        double expectedScore1 = 1.0 / (1 + Math.pow(10, (elo2 - elo1) / 400.0));
        int eloChange = (int) Math.round(kFactor * (0.5 - expectedScore1));
        return elo1 + eloChange;
    }
}
