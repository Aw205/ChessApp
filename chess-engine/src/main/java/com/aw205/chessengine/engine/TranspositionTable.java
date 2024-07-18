package com.aw205.chessengine.engine;

import java.util.Random;

public class TranspositionTable {

    private static final int ENTRY_SIZE = 8; // Size in bytes
    private static final int TABLE_SIZE = 256 * 1024 * 1024; // 256 MB
    private static final int NUM_ENTRIES = TABLE_SIZE / ENTRY_SIZE;
    private static long[] table = new long[NUM_ENTRIES];

    /** [Piece Index][Color][Piece Type] */
    public static long[][][] zobristPieceNums = new long[64][2][6];

    /** [0-15] - castling rights [16] - side to move [17-24] - ep file */
    public static long[] zobristPositionNums = new long[25];

    static {
        initZobristNums();
    }

    private static void initZobristNums() {

        Random rand = new Random();
        for (int i = 0; i < zobristPieceNums.length; i++) {
            for (int k = 0; k < 6; k++) {
                zobristPieceNums[i][0][k] = rand.nextLong(Long.MAX_VALUE);
                zobristPieceNums[i][1][k] = rand.nextLong(Long.MAX_VALUE);
            }
        }
        for (int i = 0; i < zobristPositionNums.length; i++) {
            zobristPositionNums[i] = rand.nextLong(Long.MAX_VALUE);
        }
    }

    public static long getHashKey() {

        long hashKey = 0;
        for (int i = 0; i < 64; i++) {
            int p = GameState.board[i];
            if (p != -1) {
                hashKey ^= zobristPieceNums[i][Piece.getColor(p)][Piece.getType(p)];
            }
        }
        hashKey ^= zobristPositionNums[GameState.position.castlingRights];
        if (GameState.position.turn == 0) {
            hashKey ^= zobristPositionNums[16];
        }
        // if(GameState.position.epSquare != 64){ //fix incremental update
        // int epFile = GameState.position.epSquare % 8;
        // hashKey |= zobristPositionNums[17 + epFile];
        // }
        return hashKey;
    }

    public static long getEntry(long key) {

        return table[(int) (key % NUM_ENTRIES)];
    }

    public static void storeEntry(long entry, long key) {

        table[(int) (key % NUM_ENTRIES)] = entry;
    }

    public class TableEntry {

        private static final int KEY_SHIFT = 0; // 32 bits
        private static final int DEPTH_SHIFT = 32; // 5 bits
        private static final int FLAG_SHIFT = 37; // 2 bits
        private static final int SCORE_SHIFT = 39; // 14 bits

        public static long encodeEntry(long partialKey, int depth, int flag, int score) {
            return (partialKey << KEY_SHIFT) | (depth << DEPTH_SHIFT) | (flag << FLAG_SHIFT) | (score << SCORE_SHIFT);
        }

        public static long getPartialKey(long entry) {
            return (entry >>> KEY_SHIFT) & 0xFFFFFFFF;
        }

        public static long getDepth(long entry) {
            return (entry >>> DEPTH_SHIFT) & 0x1F;
        }

        public static long getFlag(long entry) {
            return (entry >>> FLAG_SHIFT) & 0x3;
        }

        public static int getScore(long entry) {
            return (int) ((entry >>> DEPTH_SHIFT) & 0x3FFF);
        }
    }

    public class NodeType {

        public static int EXACT = 0;
        public static int LOWER_BOUND = 1;
        public static int UPPER_BOUND = 2;

    }

}
