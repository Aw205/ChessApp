package com.aw205.chessengine.engine;

import java.util.HashMap;
import java.util.Map;

public class MoveLogic {

	public static long capture_mask = -1;
	public static long push_mask = -1;
	public static long king_mask = 0; // so king doesn't move onto attacked squares of sliding pieces
	public static long pinned = 0;

	public static long[][] castleOccupiedSquares = new long[2][2]; // squares between king and rook
	public static long[][] castleAttackedSquares = new long[2][2]; // squares between the king and 2 squares to
																	// left/right
	public static int[][] castleTargetSquares = { { 2, 6 }, { 58, 62 } };
	public static int[][] initialRookSquares = { { 0, 7 }, { 56, 63 } };

	public static Map<Integer, Long> squareToBB = new HashMap<Integer, Long>();
	public static Map<Long, Integer> BBtoSquare = new HashMap<Long, Integer>();
	// public static Map<Long,

	public static long[] diagMasks = new long[16];
	public static long[] antiDiagMasks = new long[16];
	public static long[] rankMasks = new long[8];
	public static long[] fileMasks = new long[8];

	public static long[][] pawnAttacks = new long[2][65];
	public static long[] knightAttacks = new long[64];
	public static long[] kingAttacks = new long[64];

	public static long[][] inBetween = new long[64][64];
	public static long[][] squaresToLine = new long[64][64]; // indicies are from-to, returns the line/diag it resides
																// on

	public static mType[][] pawnMoves = new mType[64][64];

	public static long xRayRookAttacks(int square, long blockers) {

		long attacks = rook_moves(square, GameState.occupied);
		blockers &= attacks;
		return attacks ^ rook_moves(square, GameState.occupied ^ blockers);

	}

	public static long xRayBishopAttacks(int square, long blockers) {

		long attacks = bishop_moves(square, GameState.occupied);
		blockers &= attacks;
		return attacks ^ bishop_moves(square, GameState.occupied ^ blockers);

	}

	public static long sliding_moves(int square, long occupied, long mask) {

		long piece_pos = squareToBB.get(square);
		long piece_pos_rev = Long.reverse(piece_pos);
		long occupied_rev = Long.reverse(occupied & mask);

		long left = (occupied & mask) - 2 * piece_pos;
		long right = Long.reverse((occupied_rev - 2 * piece_pos_rev));
		long slidingAttacks = (left ^ right) & mask;

		return slidingAttacks;
	}

	public static long bishop_moves(int square, long occupied) {
		int row = square / 8;
		int col = square % 8;
		int diagIndex = (row - col) & 15;
		int antiDiagIndex = (row + col) ^ 7;

		return sliding_moves(square, occupied, diagMasks[diagIndex])
				| sliding_moves(square, occupied, antiDiagMasks[antiDiagIndex]);

	}

	// hyperbola quintessence
	public static long rook_moves(int square, long occupied) {

		return sliding_moves(square, occupied, rankMasks[square / 8])
				| sliding_moves(square, occupied, fileMasks[square % 8]);

	}

	public static long filterLegalMoves(Type type, Colour color, long pin_mask, long moves) {
		if (type == Type.KING) {
			moves &= ~king_mask; // ensure king doesn't move onto line of checkers
			moves &= ~GameState.attackedSquares[color.opposite().ordinal()];
			return moves;
		}
		return moves &= (push_mask | capture_mask) & pin_mask;
	}

	/**
	 * Generates pseudo-legal moves which still may leave their own king in check.
	 * This method filters out moves that land on pieces of their own color.
	 * 
	 * @param moves
	 * @param pColor
	 * @return
	 */
	public static long filterPseudoLegalMoves(long moves, Colour pColor) {

		long piecePositions = GameState.colorPositions[pColor.ordinal()];
		moves &= ~piecePositions;
		return moves;
	}

	private static long pawn_attacks(long square, Colour color) {

		long east = (square << 1) & ~fileMasks[0];
		long west = (square >> 1) & ~fileMasks[7];
		return (color == Colour.WHITE) ? (east | west) << 8 : (east | west) >> 8;

	}

	public static long single_pawn_push(int square, Colour color) {

		long pawn_move = MoveLogic.squareToBB.get(square);
		long push = (color == Colour.WHITE) ? pawn_move << 8 : pawn_move >> 8;
		return push & ~GameState.occupied;

	}

	public static long double_pawn_push(int square, Colour color) {

		long rankMask = (color == Colour.WHITE) ? rankMasks[3] : rankMasks[4];
		long push = single_pawn_push(square, color);
		long double_push = (color == Colour.WHITE) ? push << 8 : push >> 8;
		return double_push & ~GameState.occupied & rankMask;

	}

	public static long getPawnPushTargets(){

		long pawns = GameState.piecePosition[GameState.position.turn][Type.PAWN.ordinal()];
		long rankMask = rankMasks[3+GameState.position.turn];
		long single =  ((pawns << 8) >>> (GameState.position.turn << 4)) & ~GameState.occupied;
		return  single | (((single << 8) >>> (GameState.position.turn << 4)) & ~GameState.occupied & rankMask);

	}

	/**
	 * @param color     - color of the pieces doing the attacking
	 * @param kingIndex - index of the king being attacked
	 * @return - bitboard of attacker positions
	 */
	public static long getAttackersToKing(int color, int kingIndex) {

		long rookPos, bishopPos, pawnPos, knightPos;

		pawnPos = GameState.piecePosition[color][Type.PAWN.ordinal()];
		knightPos = GameState.piecePosition[color][Type.KNIGHT.ordinal()];
		rookPos = bishopPos = GameState.piecePosition[color][Type.QUEEN.ordinal()];
		bishopPos |= GameState.piecePosition[color][Type.BISHOP.ordinal()];
		rookPos |= GameState.piecePosition[color][Type.ROOK.ordinal()];

		return (pawnAttacks[color ^ 1][kingIndex] & pawnPos) | (knightAttacks[kingIndex] & knightPos)
				| (bishop_moves(kingIndex, GameState.occupied) & bishopPos)
				| (rook_moves(kingIndex, GameState.occupied) & rookPos);

	}

	public static void findAbsolutePins() {

		long occQ = GameState.piecePosition[GameState.position.turn ^ 1][Type.QUEEN.ordinal()];
		long occRQ = GameState.piecePosition[GameState.position.turn ^ 1][Type.ROOK.ordinal()] | occQ;
		long occBQ = GameState.piecePosition[GameState.position.turn ^ 1][Type.BISHOP.ordinal()] | occQ;

		long kingBB = GameState.piecePosition[GameState.position.turn][Type.KING.ordinal()];
		int kingPos = BBtoSquare.get(kingBB);

		long pinner = xRayRookAttacks(kingPos, GameState.colorPositions[GameState.position.turn])
				& occRQ;
		pinned = 0;

		while (pinner != 0) {
			int square = Long.numberOfTrailingZeros(pinner);
			pinned |= inBetween[square][kingPos] & GameState.colorPositions[GameState.position.turn];
			pinner &= pinner - 1;
		}

		pinner = xRayBishopAttacks(kingPos, GameState.colorPositions[GameState.position.turn])
				& occBQ;

		while (pinner != 0) {
			int square = Long.numberOfTrailingZeros(pinner);
			pinned |= inBetween[square][kingPos] & GameState.colorPositions[GameState.position.turn];
			pinner &= pinner - 1;
		}
	}

	public static void updateCheckMasks(int kingIndex, long attackers) {

		if (Long.bitCount(attackers) > 1) {
			push_mask = 0;
			capture_mask = 0;

			long lsb = attackers & -attackers;
			long msb = attackers & (attackers - 1l);
			long[] checkers = { lsb, msb };

			for (long c : checkers) {
				int idx = BBtoSquare.get(c);
				Type t = GameState.board[idx].type;
				if (t == Type.BISHOP || t == Type.ROOK || t == Type.QUEEN) {
					king_mask |= (squaresToLine[kingIndex][idx] ^ c);
				}
			}
			return;
		}

		capture_mask = attackers;
		push_mask = 0;

		int attackerIndex = BBtoSquare.get(attackers);
		Type t = GameState.board[attackerIndex].type;
		if (t == Type.BISHOP || t == Type.ROOK || t == Type.QUEEN) {
			push_mask = inBetween[kingIndex][attackerIndex];
			king_mask = squaresToLine[kingIndex][attackerIndex] ^ attackers;
		}

	}

	private static long knight_moves(long pos) {

		long east, west, attacks = 0;

		east = (pos << 1) & ~fileMasks[0];
		west = (pos >> 1) & ~fileMasks[7];
		attacks = (east | west) << 16 & (~rankMasks[0] & ~rankMasks[1]);
		attacks |= (east | west) >> 16 & (~rankMasks[7] & ~rankMasks[6]);
		east = (east << 1) & ~fileMasks[0];
		west = (west >> 1) & ~fileMasks[7];
		attacks |= (east | west) << 8 & (~rankMasks[0]);
		attacks |= (east | west) >> 8 & (~rankMasks[7]);

		return attacks;

	}

	private static long king_moves(long square) {

		long east = (square << 1) & ~fileMasks[0];
		long west = (square >> 1) & ~fileMasks[7];
		long attack = (east | west);

		attack |= (attack << 8 | attack >>> 8);

		return attack | (square >>> 8) | (square << 8);

	}

	private static void initPawnMoves() {

		for (int i = 0; i < 64; i++) {
			for (int j = 0; j < 64; j++) {

				boolean isCapture = (i - j) % 8 != 0;

				if (Math.abs(i - j) == 16) {
					pawnMoves[i][j] = mType.DOUBLE_PUSH;
				} else if (j > 55 || j < 8) {
					pawnMoves[i][j] = mType.PROMO;
					if (isCapture) {
						pawnMoves[i][j] = mType.PROMO_CAPTURE;
					}
				} else if (isCapture) {
					pawnMoves[i][j] = mType.CAPTURE;
				} else {
					pawnMoves[i][j] = mType.QUIET;
				}
				// if (x != 8 && j % 8 == x && (((j - i) > 0 && j / 8 == 5) || ((j - i) < 0 && j
				// / 8 == 2))) {
			}
		}

	}

	public static void precompute() {

		initPawnMoves();

		pawnAttacks[0][64] = 0l;
		pawnAttacks[1][64] = 0l;

		for (int row = 0; row < 8; row++) {

			rankMasks[row] = 0xFFL << (row * 8);
			fileMasks[row] = 0x101010101010101L << row;
		}

		squareToBB.put(64, 0l); // for en passant

		for (int i = 0; i < 64; i++) {

			int row = i / 8, col = i % 8;
			long BB = 1L << i;
			squareToBB.put(i, BB);
			BBtoSquare.put(BB, i);

			int diagIndex = (row - col) & 15;
			int antiDiagIndex = (row + col) ^ 7;

			diagMasks[diagIndex] |= BB;
			antiDiagMasks[antiDiagIndex] |= BB;

			knightAttacks[i] = knight_moves(BB);
			kingAttacks[i] = king_moves(BB);

			pawnAttacks[Colour.WHITE.ordinal()][i] = pawn_attacks(BB, Colour.WHITE);
			pawnAttacks[Colour.BLACK.ordinal()][i] = pawn_attacks(BB, Colour.BLACK);

		}

		for (int from = 0; from < 64; from++) {
			for (int to = from + 1; to < 64; to++) {

				int fromRow = from / 8, fromCol = from % 8, toRow = to / 8, toCol = to % 8;
				long occupied = squareToBB.get(from) | squareToBB.get(to);
				long currentMask = 0;

				if (fromRow == toRow) {
					currentMask = rankMasks[fromRow];

				} else if (fromCol == toCol) {
					currentMask = fileMasks[fromCol];

				} else if ((fromRow - toRow) * (fromCol - toCol) > 0) {
					int diagIndex = (toRow - toCol) & 15;
					currentMask = diagMasks[diagIndex];

				} else {
					int antiDiagIndex = (toRow + toCol) ^ 7;
					currentMask = antiDiagMasks[antiDiagIndex];
				}

				long inBetweenSquares = sliding_moves(from, occupied, currentMask)
						& sliding_moves(to, occupied, currentMask);

				inBetween[from][to] = inBetween[to][from] = inBetweenSquares;
				squaresToLine[from][to] = squaresToLine[to][from] = currentMask;

			}
		}
		castleOccupiedSquares[0][0] = inBetween[0][4];
		castleOccupiedSquares[0][1] = inBetween[4][7];
		castleOccupiedSquares[1][0] = inBetween[56][60];
		castleOccupiedSquares[1][1] = inBetween[60][63];

		castleAttackedSquares[0][0] = inBetween[1][4];
		castleAttackedSquares[0][1] = inBetween[4][7];
		castleAttackedSquares[1][0] = inBetween[57][60];
		castleAttackedSquares[1][1] = inBetween[60][63];
	}
}
