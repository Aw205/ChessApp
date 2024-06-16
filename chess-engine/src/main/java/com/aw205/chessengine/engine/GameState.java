package com.aw205.chessengine.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class GameState {

	public static Position position = new Position();

	public static long occupied = 0;
	public static long[] colorPositions = new long[2];
	public static long[] attackedSquares = new long[2];
	public static long[][] piecePosition = new long[2][6];

	public static Stack<Position> stack = new Stack<Position>();
	public static int[] board = new int[64];

	public static long captures = 0;

	public GameState() {

	}

	public static void init() {

		MoveLogic.precompute();
		Arrays.fill(board,-1);

		parseFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		//parseFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"); // Kiwipete
		//parseFEN("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"); // Pos 3
		//parseFEN("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"); // Pos 4
		//parseFEN("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"); // Pos 5
		//parseFEN("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"); // Pos 6
		//parseFEN("n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1"); // promotion
		//parseFEN("rnbqkbnr/ppp3pp/4B3/p3R2p/1P6/8/PPPP1PPP/RNB1KBNR w - - 0
		// 1"); // double check
		//parseFEN("8/8/8/2k5/2p1p3/8/3P4/4K3 w - - 0 1"); // EP check evasion
		//parseFEN("8/6bb/8/8/R1pP2k1/4P3/P7/K7 b - d3 0 1"); // EP horizontal pin test

		for (int i = 0; i < board.length; i++) {
			if (board[i] != -1) {
				long pos = 1L << i;
				occupied |= pos;
				int color = Piece.getColor(board[i]);
				int type = Piece.getType(board[i]);
				piecePosition[color][type] |= pos;
				colorPositions[color] |= pos;
			}
		}

		long startTime = System.nanoTime();
		long num = perft(6);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;
		System.out.println("time: " + duration + " ms");
		System.out.println("DEPTH 6: " + num + " nodes");
		System.out.println("CAPTURES: " + captures);
	}

	public static long perft(int depth) {

		long nodes = 0;
		// if (depth == 0) {
		// 	return 1;
		// }
		List<Integer> moves = generateMoves();
		if (depth == 1) {
			return moves.size();
		}
		for (int i = 0; i < moves.size(); i++) {
			//int type = Move.getMoveType(moves.get(i));
			// if (depth == 1 && (type == mType.CAPTURE || type == mType.EP_CAPTURE || type
			// == mType.PROMO_CAPTURE)) {
			// captures++;
			// }
			// if (depth == 1 && (type == mType.PROMO || type == mType.PROMO_CAPTURE )) {
			// captures++;
			// }
			// if (depth == 1 && (type == mType.EP_CAPTURE )) {
			// captures++;
			// }
			// if (depth == 1 && (type==mType.DOUBLE_PUSH )) {
			// captures++;
			// }
			// if (depth == 1 && (type == mType.CASTLE.ordinal())) {
			// captures++;
			// }
			makeMove(moves.get(i));
			nodes += perft(depth - 1);
			unmakeMove(moves.get(i));
		}
		return nodes;
	}

	/**
	 * Player turn is same color as the move
	 * 
	 * @param move
	 */
	public static void makeMove(int move) {

		mType moveType = mType.valMTypes[Move.getMoveType(move)];

		int fromIndex = Move.getFromSquare(move);
		int toIndex = Move.getToSquare(move);
		long from = 1L << fromIndex;
		long to = 1L << toIndex;
		long fromTo = from ^ to;

		int type = Piece.getType(board[fromIndex]);

		stack.add(new Position(position));
		position.epSquare = 64;
		position.halfMoveClock++;

		switch (moveType) {

			case DOUBLE_PUSH:
				position.epSquare = fromIndex + (toIndex - fromIndex) / 2;
			case QUIET:
				piecePosition[position.turn][type] ^= fromTo;
				break;

			case PROMO:

				int promoType = Move.getPromoType(move);
				piecePosition[position.turn][0] ^= from;
				piecePosition[position.turn][promoType] ^= to;
				board[fromIndex] = Piece.encodePiece(position.turn, promoType);
				break;

			case EP_CAPTURE:

				long captureBB = MoveLogic.rankMasks[fromIndex / 8] & MoveLogic.fileMasks[toIndex % 8];
				int captureIndex = Long.numberOfTrailingZeros(captureBB);

				colorPositions[position.turn ^ 1] ^= captureBB;
				piecePosition[position.turn ^ 1][0] ^= captureBB;
				piecePosition[position.turn][0] ^= fromTo;
				board[captureIndex] = -1;

				break;

			case CAPTURE:

				int capturedType = Piece.getType(board[toIndex]);
				colorPositions[position.turn ^ 1] ^= to;
				piecePosition[position.turn ^ 1][capturedType] ^= to;
				piecePosition[position.turn][type] ^= fromTo;
				break;

			case PROMO_CAPTURE:

				int promotype = Move.getPromoType(move);
				int captureType = Piece.getType(board[toIndex]);

				colorPositions[position.turn ^ 1] ^= to;

				piecePosition[position.turn ^ 1][captureType] ^= to;
				piecePosition[position.turn][0] ^= from;
				piecePosition[position.turn][promotype] ^= to;

				board[fromIndex] = Piece.encodePiece(position.turn, promotype);
				break;

			case CASTLE:

				piecePosition[position.turn][Type.KING.ordinal()] ^= fromTo;

				long[] rookVals = MoveLogic.kingToRook.get(toIndex);

				piecePosition[position.turn][Type.ROOK.ordinal()] ^= rookVals[2];
				colorPositions[position.turn] ^= rookVals[2];
				board[(int) rookVals[1]] = board[(int) rookVals[0]];
				board[(int) rookVals[0]] = -1;
				break;

			default:
				System.out.println("Invalid move");
		}

		colorPositions[position.turn] ^= fromTo;
		board[toIndex] = board[fromIndex];
		board[fromIndex] = -1;

		occupied = colorPositions[0] | colorPositions[1];
		updateCastlingRights(type);
		position.turn ^= 1;
	}

	public static void unmakeMove(int move) {

		mType moveType = mType.valMTypes[Move.getMoveType(move)];

		int fromIndex = Move.getFromSquare(move);
		int toIndex = Move.getToSquare(move);
		long from = 1L << fromIndex;
		long to = 1L << toIndex;
		long fromTo = from ^ to;

		int type = Piece.getType(board[toIndex]);

		position = stack.pop();

		switch (moveType) {

			case DOUBLE_PUSH:
			case QUIET:

				piecePosition[position.turn][type] ^= fromTo;

				board[fromIndex] = board[toIndex];
				board[toIndex] = -1;

				break;

			case PROMO:

				piecePosition[position.turn][0] ^= from;
				piecePosition[position.turn][type] ^= to;

				board[fromIndex] = Piece.encodePiece(position.turn,0);
				board[toIndex] = -1;

				break;

			case EP_CAPTURE:

				long captureBB = MoveLogic.rankMasks[fromIndex / 8] & MoveLogic.fileMasks[toIndex % 8];
				int captureIndex = Long.numberOfTrailingZeros(captureBB);

				colorPositions[position.turn ^ 1] ^= captureBB;
				piecePosition[position.turn ^ 1][0] ^= captureBB;
				piecePosition[position.turn][0] ^= fromTo;

				board[fromIndex] = board[toIndex];
				board[toIndex] = -1;
				board[captureIndex] = Piece.encodePiece(position.turn ^ 1,0);

				break;

			case CAPTURE:

				int captured = Move.getCapturedPiece(move);
				int capturedType = Piece.getType(captured);

				colorPositions[position.turn ^ 1] ^= to;

				piecePosition[position.turn ^ 1][capturedType] ^= to;
				piecePosition[position.turn][type] ^= fromTo;

				board[fromIndex] = board[toIndex];
				board[toIndex] = captured;

				break;

			case PROMO_CAPTURE:

				int Captured = Move.getCapturedPiece(move);
				int CapturedType = Piece.getType(Captured);

				colorPositions[position.turn ^ 1] ^= to;

				piecePosition[position.turn ^ 1][CapturedType] ^= to;
				piecePosition[position.turn][type] ^= to;
				piecePosition[position.turn][0] ^= from;

				board[fromIndex] = Piece.encodePiece(position.turn,0);
				board[toIndex] = Captured;

				break;

			case CASTLE:

				piecePosition[position.turn][Type.KING.ordinal()] ^= fromTo;

				board[fromIndex] = board[toIndex];
				board[toIndex] = -1;

				long[] rookVals = MoveLogic.kingToRook.get(toIndex);

				piecePosition[position.turn][Type.ROOK.ordinal()] ^= rookVals[2];
				colorPositions[position.turn] ^= rookVals[2];

				board[(int) rookVals[0]] = board[(int) rookVals[1]];
				board[(int) rookVals[1]] = -1;

				break;
			default:
				System.out.println("Invalid unmake move");
		}

		colorPositions[position.turn] ^= fromTo;
		occupied = colorPositions[0] | colorPositions[1];
	}

	public static List<Integer> generateMoves() {

		attackedSquares[0] = 0;
		attackedSquares[1] = 0;

		long bb = colorPositions[position.turn ^ 1];
		while (bb != 0) {
			int squareIndex = Long.numberOfTrailingZeros(bb);
			int type = Piece.getType(board[squareIndex]);
			attackedSquares[position.turn ^ 1] |= Piece.generateAttackedSquares(type, position.turn ^ 1, squareIndex);
			bb &= (bb - 1);
		}

		// determine check masks so can generate right legal moves
		long kingBB = GameState.piecePosition[position.turn][Type.KING.ordinal()];
		int kingIndex = Long.numberOfTrailingZeros(kingBB);

		long checkers = MoveLogic.getAttackersToKing(position.turn ^ 1, kingIndex);
		if (checkers != 0) {
			MoveLogic.updateCheckMasks(kingIndex, checkers);
		}
		// find pins
		MoveLogic.findAbsolutePins();

		List<Integer> moveList = new ArrayList<Integer>();

		long nonPawns = colorPositions[position.turn] & ~piecePosition[position.turn][0];
		while (nonPawns != 0) {

			int fromIndex = Long.numberOfTrailingZeros(nonPawns);
			int type = Piece.getType(board[fromIndex]);

			long attacked = Piece.generateAttackedSquares(type, position.turn, fromIndex);
			long legalMoves = Piece.generateLegalMoves(type, fromIndex, attacked, kingIndex);

			addMoves(fromIndex, legalMoves, moveList);
			attackedSquares[position.turn] |= attacked;

			nonPawns &= (nonPawns - 1);
		}

		long pawns = piecePosition[position.turn][0];
		while (pawns != 0) {

			int fromIndex = Long.numberOfTrailingZeros(pawns);

			long attacked = Piece.generateAttackedSquares(0, position.turn, fromIndex);
			long legalMoves = Piece.generateLegalPawnMoves(fromIndex, attacked, kingIndex);

			addPawnMoves(fromIndex, legalMoves, moveList);
			attackedSquares[position.turn] |= attacked;

			pawns &= (pawns - 1);
		}

		generateCastleMoves(kingBB, kingIndex, moveList);
		generateEpMoves(kingIndex, moveList);

		// reset check mask
		MoveLogic.capture_mask = -1;
		MoveLogic.push_mask = -1;
		MoveLogic.king_mask = 0;

		return moveList;
	}

	private static void addMoves(int fromIndex, long move, List<Integer> moves) {

		long nonCaptures = move & ~occupied;
		while (nonCaptures != 0) {
			int toIndex = Long.numberOfTrailingZeros(nonCaptures);
			moves.add(Move.encodeMove(fromIndex, toIndex, mType.QUIET.ordinal(), 0, 0));
			nonCaptures &= (nonCaptures - 1);
		}

		long captures = move & occupied;
		while (captures != 0) {
			int toIndex = Long.numberOfTrailingZeros(captures);
			moves.add(Move.encodeMove(fromIndex, toIndex, mType.CAPTURE.ordinal(), 0, board[toIndex]));
			captures &= (captures - 1);
		}
	}

	private static void addPawnMoves(int fromIndex, long move, List<Integer> moves) {

		while (move != 0) {

			int toIndex = Long.numberOfTrailingZeros(move);
			int moveType = MoveLogic.pawnMoves[fromIndex][toIndex].ordinal();
			int m = Move.encodeMove(fromIndex, toIndex, moveType, Type.QUEEN.ordinal(), board[toIndex]);

			if (moveType == mType.PROMO.ordinal() || moveType == mType.PROMO_CAPTURE.ordinal()) {
				moves.add(Move.encodeMove(fromIndex, toIndex, moveType, Type.KNIGHT.ordinal(), board[toIndex]));
				moves.add(Move.encodeMove(fromIndex, toIndex, moveType, Type.BISHOP.ordinal(), board[toIndex]));
				moves.add(Move.encodeMove(fromIndex, toIndex, moveType, Type.ROOK.ordinal(), board[toIndex]));
			}
			moves.add(m);
			move &= (move - 1);
		}
	}

	/**
	 * Updates the permanent castling rights
	 * 
	 * @param type - The type of the piece that is moving
	 */
	private static void updateCastlingRights(int type) {

		if (type == Type.KING.ordinal()) { // what if king eats other rook?
			position.castlingRights[position.turn * 2] = false;
			position.castlingRights[position.turn * 2 + 1] = false;
			return;
		}

		for (int color = 0; color < 2; color++) {
			for (int side = 0; side < 2; side++) {
				int sqr = MoveLogic.initialRookSquares[color][side];
				if (board[sqr] == -1 || (Piece.getColor(board[sqr]) != color)) {
					position.castlingRights[color * 2 + side] = false;
				}
			}
		}
	}

	private static void generateCastleMoves(long kingPos, int kingIndex, List<Integer> moves) {

		if ((kingPos & attackedSquares[position.turn ^ 1]) != 0) {
			return;
		}
		for (int i = 0; i < 2; i++) {
			if (position.castlingRights[position.turn * 2 + i]) {
				if ((MoveLogic.castleAttackedSquares[position.turn][i] & attackedSquares[position.turn ^ 1]) == 0) {
					if ((MoveLogic.castleOccupiedSquares[position.turn][i] & occupied) == 0) {

						int to = MoveLogic.castleTargetSquares[position.turn][i];
						int castle = Move.encodeMove(kingIndex, to, mType.CASTLE.ordinal(), 0, 0);
						moves.add(castle);
					}
				}
			}
		}
	}

	private static void generateEpMoves(int kingIndex, List<Integer> moves) {

		long epPos = MoveLogic.pawnAttacks[position.turn ^ 1][position.epSquare]
				& piecePosition[position.turn][Type.PAWN.ordinal()];

		while (epPos != 0) {

			int from = Long.numberOfTrailingZeros(epPos);
			long fromBB = 1L << from;
			long epCapture = MoveLogic.rankMasks[from / 8] & MoveLogic.fileMasks[position.epSquare % 8];
			
			long pinMask = -1;
			if((fromBB & MoveLogic.pinned) != 0){
				pinMask = MoveLogic.squaresToLine[from][kingIndex];
			}

			if (((MoveLogic.capture_mask & epCapture) & pinMask) != 0) {

				long occ = occupied ^ fromBB ^ epCapture;
				long rookQueenPos = piecePosition[position.turn ^ 1][Type.QUEEN.ordinal()]
						| piecePosition[position.turn ^ 1][Type.ROOK.ordinal()];

				if ((MoveLogic.sliding_moves(kingIndex, occ, MoveLogic.rankMasks[kingIndex / 8]) & rookQueenPos) == 0) {

					int m = Move.encodeMove(from, position.epSquare, mType.EP_CAPTURE.ordinal(), 0, 0);
					moves.add(m);
				}
			}
			epPos &= (epPos - 1);
		}
	}

	/**
	 * Starting fen: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1.
	 * Uses regex for validation.
	 * 
	 * @param fen
	 */
	private static void parseFEN(String fen) {

		String[] parts = fen.split(" ");
		if (parts.length != 6) {
			throw new IllegalArgumentException("Invalid FEN string");
		}

		int idx = 56;
		for (int i = 0; i < parts[0].length(); i++) {
			char c = parts[0].charAt(i);
			if (c == '/') {
				idx -= 16;
				continue;
			}
			if (Character.isDigit(c)) {
				idx += Character.getNumericValue(c);
				continue;
			}
			Colour color = (Character.isLowerCase(c)) ? Colour.BLACK : Colour.WHITE;
			int p = -1;

			switch (Character.toLowerCase(c)) {
				case 'p':
					p = Piece.encodePiece(color.ordinal(),Type.PAWN.ordinal());
					break;
				case 'b':
					p = Piece.encodePiece(color.ordinal(),Type.BISHOP.ordinal());
					break;
				case 'n':
					p = Piece.encodePiece(color.ordinal(),Type.KNIGHT.ordinal());
					break;
				case 'r':
					p = Piece.encodePiece(color.ordinal(),Type.ROOK.ordinal());
					break;
				case 'q':
					p = Piece.encodePiece(color.ordinal(),Type.QUEEN.ordinal());
					break;
				case 'k':
					p = Piece.encodePiece(color.ordinal(),Type.KING.ordinal());
					break;
				default:
					System.err.println("Invalid FEN");
			}
			board[idx] = p;
			idx++;
		}

		char sideToMove = parts[1].charAt(0);
		String castlingRights = parts[2];
		String enPassantTarget = parts[3];
		int halfMoveClock = Integer.parseInt(parts[4]);
		// int fullMoveNumber = Integer.parseInt(parts[5]);

		position.turn = (sideToMove == 'w') ? 0 : 1;

		Map<Character, Integer> castleMap = Map.of(
				'Q', 0,
				'K', 1,
				'q', 2,
				'k', 3);

		if (!castlingRights.equals("-")) {
			for (int i = 0; i < castlingRights.length(); i++) {
				int idex = castleMap.get(castlingRights.charAt(i));
				position.castlingRights[idex] = true;
			}
		}

		if (!enPassantTarget.equals("-")) {
			char epFile = enPassantTarget.charAt(0);
			int rank = enPassantTarget.charAt(1) - '0';
			position.epSquare = (epFile - 'a') + (rank - 1) * 8;
		}

		position.halfMoveClock = halfMoveClock;
	}

	public static void printDebug() {

		String RESET = "\u001B[0m";
		String RED = "\u001B[31m";
		String GREEN = "\u001B[32m";
		//String YELLOW = "\u001B[33m";
		String MAGENTA = "\u001B[35m";
		String CYAN = "\u001B[36m";
		String GRAY = "\u001b[38;5;240m";

		System.out.println(MAGENTA + "Game State: -----------------" + RESET);
		System.out.println(RED + "Move Counter: " + position.halfMoveClock + RESET);

		String turn = (position.turn == 0) ? "White" : "Black";

		System.out.println(RED + "TURN: " + turn + RESET);

		System.out.println(GREEN + "Occupied:" + RESET);

		String occString = String.format("%64s", Long.toBinaryString(occupied)).replace(' ', '0');
		StringBuilder occupiedBB = new StringBuilder(occString);
		occupiedBB.reverse();
		for (int i = 64; i > 0; i -= 8) {
			String row = occupiedBB.substring(i - 8, i)
					.replace("", " ")
					.replace("1", RED + "1" + RESET)
					.replace(" 0", GRAY + " 0" + RESET);
			System.out.println(" " + row);
		}

		// System.out.println(YELLOW + "En Passant Square: " + RESET +
		// position.epSquare);

		// System.out.println( GREEN + "Attacked Squares: " + RESET);
		// System.out.println(Long.toBinaryString(attackedSquares[0]));
		// System.out.println(Long.toBinaryString(attackedSquares[1]));

		System.out.println(GREEN + "Castling Rights: " + RESET);
		System.out.println("White queenside: " + position.castlingRights[0]);
		System.out.println("White kingside: " + position.castlingRights[1]);
		System.out.println("Black queenside: " + position.castlingRights[2]);
		System.out.println("Black kingside: " + position.castlingRights[3]);

		System.out.println(GREEN + "Board:" + RESET);

		int idx = 56;
		for (int i = 0; i < 64; i++) {
			if (board[idx] != -1) {
				char c = Type.getFenChar(Type.values()[Piece.getType(board[idx])]);
				String p = (Piece.getColor(board[idx]) == Colour.WHITE.ordinal()) ? MAGENTA + c + RESET
						: CYAN + Character.toLowerCase(c) + RESET;
				System.out.print(p);
				idx++;
			} else {
				System.out.print(".");
				idx++;
			}
			if ((i + 1) % 8 == 0) {
				idx -= 16;
				System.out.println("");
			}
		}
	}

	public static String getFen() {

		StringBuilder fen = new StringBuilder();
		StringBuilder row = new StringBuilder();
		int emptyCount = 0;

		for (int i = 63; i > -1; i--) {

			if (board[i] == -1) {
				emptyCount++;
			} else {
				if (emptyCount > 0) {
					row.append(emptyCount);
					emptyCount = 0;
				}
				char c = Type.getFenChar(Type.values()[Piece.getType(board[i])]);
				if (Piece.getColor(board[i]) == Colour.BLACK.ordinal()) {
					c = Character.toLowerCase(c);
				}
				row.append(c);
			}
			if (i % 8 == 0) {
				if (emptyCount > 0) {
					row.append(emptyCount);
					emptyCount = 0;
				}
				if (i != board.length - 1) {
					row.reverse();
					fen.append(row).append('/');
					row.setLength(0);
				}
			}
		}
		fen.deleteCharAt(fen.length() - 1);
		// fen.append(" w KQkq - 0 1");
		return fen.toString();
	}

}
