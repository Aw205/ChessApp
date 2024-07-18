
package com.aw205.chessengine.engine;

public class Type {

	public static final int PAWN = 0;
	public static final int BISHOP = 1;
	public static final int KNIGHT = 2;
	public static final int ROOK = 3;
	public static final int QUEEN = 4;
	public static final int KING = 5;

	public static char getFenChar(int t){

		switch (t) {
			case PAWN:
				return 'P';

			case BISHOP:
				return 'B';

			case KNIGHT:
				return 'N';

			case ROOK:
				return 'R';

			case QUEEN:
				return 'Q';

			case KING:
				return 'K';
			default:
				return 'x';
		}
	}
}
