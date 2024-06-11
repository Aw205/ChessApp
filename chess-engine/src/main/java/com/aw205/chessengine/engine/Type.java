
package com.aw205.chessengine.engine;

public enum Type {

	PAWN, BISHOP, KNIGHT, ROOK, QUEEN, KING;

	public static char getFenChar(Type t) {

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
		};
		return '0';

	}
}
