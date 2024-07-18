package com.aw205.chessengine.engine;

public class Piece {

	private static final int COLOR_SHIFT = 0; // 1 bits
	private static final int TYPE_SHIFT = 1; // 3 bit

	public static int encodePiece(int color, int type) {
		return (color << COLOR_SHIFT) | (type << TYPE_SHIFT);
	}

	public static int getColor(int piece) {
		return (piece >>> COLOR_SHIFT) & 0x1;
	}

	public static int getType(int piece) {
		return (piece >>> TYPE_SHIFT) & 0x7;
	}

	public static long generateLegalPawnMoves(int squareIndex, long attackedSquares,int kingIndex) {

		long pseudoLegalMoves = MoveLogic.filterPseudoLegalMoves(attackedSquares);

		pseudoLegalMoves &= GameState.occupied;
		pseudoLegalMoves |= MoveLogic.single_pawn_push(squareIndex, GameState.position.turn);
		pseudoLegalMoves |= MoveLogic.double_pawn_push(squareIndex, GameState.position.turn);

		long pinMask = calcPinMask(squareIndex,kingIndex);
		return MoveLogic.filterLegalMoves(0,pinMask,pseudoLegalMoves);
	}


	/**
	 * Used for Quiesence search
	 */
	public static long generateLegalPawnCaptureMoves(int squareIndex, long attackedSquares,int kingIndex){

		long pseudoLegalMoves = MoveLogic.filterPseudoLegalMoves(attackedSquares) & GameState.occupied;
		long pinMask = calcPinMask(squareIndex,kingIndex);
		return MoveLogic.filterLegalMoves(0,pinMask,pseudoLegalMoves);

	}


	public static long calcPinMask(int squareIndex, int kingIndex) {

		long pinMask = -1;
		long from = 1L << squareIndex;
		boolean isPinned = (from & MoveLogic.pinned) != 0;
		if (isPinned) {
			pinMask = MoveLogic.squaresToLine[squareIndex][kingIndex];
		}
		return pinMask;
	}

	public static long generateAttackedSquares(int type, int color, int squareIndex) {

		long attackedSquares = 0;

		switch (type) {
			case 0:
				attackedSquares = MoveLogic.pawnAttacks[color][squareIndex];
				break;
			case 1:
				attackedSquares = MoveLogic.bishop_moves(squareIndex, GameState.occupied);
				break;
			case 2:
				attackedSquares = MoveLogic.knightAttacks[squareIndex];
				break;
			case 3:
				attackedSquares = MoveLogic.rook_moves(squareIndex, GameState.occupied);
				break;
			case 4:
				attackedSquares = MoveLogic.rook_moves(squareIndex, GameState.occupied);
				attackedSquares |= MoveLogic.bishop_moves(squareIndex, GameState.occupied);
				break;
			case 5:
				attackedSquares = MoveLogic.kingAttacks[squareIndex];
				break;
			default:
		}
		return attackedSquares;
	}

	public static long generateLegalMoves(int type, int squareIndex, long attackedSquares, int kingIndex) {

		long pseudoLegalMoves = MoveLogic.filterPseudoLegalMoves(attackedSquares);
		long pinMask = calcPinMask(squareIndex, kingIndex);
		return MoveLogic.filterLegalMoves(type, pinMask, pseudoLegalMoves);
	}

}
