package com.aw205.chessengine.engine;

public class Move {

    private static final int FROM_SHIFT = 0; // 6 bits
    private static final int TO_SHIFT = 6; // 6 bits
    private static final int TYPE_SHIFT = 12; // 3 bits
    private static final int PROMO_SHIFT = 15; // 3 bits
    private static final int CAPTURE_SHIFT = 18; // 4 bits

    public static int encodeMove(int from, int to, int moveType, int promoType, int captured) {
        return (from << FROM_SHIFT) | (to << TO_SHIFT) | (moveType << TYPE_SHIFT) | (promoType << PROMO_SHIFT)
                | (captured << CAPTURE_SHIFT);
    }

    public static int getFromSquare(int move) {
        return (move >>> FROM_SHIFT) & 0x3F;
    }

    public static int getToSquare(int move) {
        return (move >>> TO_SHIFT) & 0x3F;
    }

    public static int getMoveType(int move) {
        return (move >>> TYPE_SHIFT) & 0x7;
    }

    public static int getPromoType(int move) {
        return (move >>> PROMO_SHIFT) & 0x7;
    }

    public static int getCapturedPiece(int move) {
        return (move >>> CAPTURE_SHIFT) & 0xF;
    }

}