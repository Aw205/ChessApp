package com.aw205.chessengine.engine;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.aw205.chessengine.engine.TranspositionTable.NodeType;
import com.aw205.chessengine.engine.TranspositionTable.TableEntry;

public class Search {

   private static Comparator<Integer> moveComparator = new MoveComparator();
   private static int[] pieceValue = { 100, 300, 300, 500, 900, 10 };

   public static int search(int depth) {

      int bestMove = -1;
      for (int currDepth = 1; currDepth <= depth; currDepth++) {
         bestMove = rootSearch(Integer.MIN_VALUE, Integer.MAX_VALUE, currDepth);
      }
      return bestMove;
   }

   public static int rootSearch(int alpha, int beta, int depth) {

      int bestMove = -1;
      for (Integer m : GameState.generateMoves()) {

         GameState.makeMove(m);
         int score = -negaMax(Integer.MIN_VALUE, Integer.MAX_VALUE, depth - 1);
         GameState.unmakeMove(m);

         if (score > alpha) {
            alpha = score;
            bestMove = m;
         }
      }

      return bestMove;
   }

   private static int negaMax(int alpha, int beta, int depth) {

      long entry = TranspositionTable.getEntry(GameState.zobristKey);
      if (entry != 0 && TableEntry.getPartialKey(entry) == (GameState.zobristKey >>> 32) && TableEntry.getDepth(entry) >= depth) {
       
         long nodeType = TableEntry.getFlag(entry);
         int score = TableEntry.getScore(entry);

         if (nodeType == NodeType.EXACT) {
            return score;
         } else if (nodeType == NodeType.LOWER_BOUND) {
            alpha = Math.max(alpha, score);
         } else if (nodeType == NodeType.UPPER_BOUND) {
            beta = Math.min(beta, score);
         }
         if (alpha >= beta) {
            return score;
         }
      }

      if (depth == 0) {
         return quiesce(Integer.MIN_VALUE, Integer.MAX_VALUE);
      }

      List<Integer> moves = GameState.generateMoves();
      if (moves.size() == 0) {
         int turn = (GameState.position.turn == 1) ? 1 : -1;
         return -100000 * turn;
      }

      int score = Integer.MIN_VALUE;
      for (Integer m : moves) {
         GameState.makeMove(m);
         score = Math.max(score, -negaMax(-beta, -alpha, depth - 1));
         GameState.unmakeMove(m);

         alpha = Math.max(alpha, score);
         if (alpha >= beta) {
            break;
         }
      }

      int flag = NodeType.EXACT;
      if (score < alpha) {
         flag = NodeType.UPPER_BOUND;
      } else if (score >= beta) {
         flag = NodeType.LOWER_BOUND;
      }
      long newEntry = TableEntry.encodeEntry(GameState.zobristKey >>> 32, depth, flag, score);
      TranspositionTable.storeEntry(newEntry, GameState.zobristKey);

      return score;
   }

   private static int quiesce(int alpha, int beta) {

      int standPat = evaluate();
      if (standPat >= beta) {
         return beta;
      }
      if (alpha < standPat) {
         alpha = standPat;
      }

      List<Integer> moveList = GameState.generateCaptureMoves();
      Collections.sort(moveList, Search.moveComparator);

      for (int captureMove : moveList) {
         GameState.makeMove(captureMove);
         int score = -quiesce(-beta, -alpha);
         GameState.unmakeMove(captureMove);

         if (score >= beta) {
            return beta;
         }
         if (score > alpha) {
            alpha = score;
         }
      }
      return alpha;

   }

   private static int evaluate() {

      int side = GameState.position.turn ^ 1;
      int score = 0;
      for (int i = 0; i < 5; i++) {
         score += pieceValue[i] * (Long.bitCount(GameState.piecePosition[0][i]) - Long.bitCount(GameState.piecePosition[1][i]));
      }
      int turn = (GameState.position.turn == 1) ? -1 : 1;
      score += Long.bitCount(GameState.attackedSquares[side]) * 5;
      return score * turn;

   }

   private static class MoveComparator implements Comparator<Integer> {

      @Override
      public int compare(Integer m1, Integer m2) {

         int capture_type_1 = Piece.getType(Move.getCapturedPiece(m1));
         int attacker_type_1 = Piece.getType(GameState.board[Move.getFromSquare(m1)]);
         int mvv_lva_1 = pieceValue[capture_type_1] - pieceValue[attacker_type_1];

         int capture_type_2 = Piece.getType(Move.getCapturedPiece(m2));
         int attacker_type_2 = Piece.getType(GameState.board[Move.getFromSquare(m2)]);
         int mvv_lva_2 = pieceValue[capture_type_2] - pieceValue[attacker_type_2];

         return -(mvv_lva_1 - mvv_lva_2);

      }
   }

}
