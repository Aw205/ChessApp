package com.aw205.chessengine.engine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class GameState {

	public static Position position = new Position();

	public static long occupied = 0;
	public static long[] colorPositions = new long[2];
	public static long[] attackedSquares = new long[2];
	public static long[][] piecePosition = new long[2][6];

	public static Map<String, Move> movesMap = new HashMap<String, Move>();

	@SuppressWarnings("unchecked")
	public static List<Piece> [][] pieceList = new LinkedList [2][6];

	public static Stack<Position> stack = new Stack<Position>();
	public static Piece[] board = new Piece[64];

	public static int captures = 0;

	public GameState() {

	}

	public static void init() {

		MoveLogic.precompute();

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 6; j++) {
				pieceList[i][j] = new LinkedList<Piece>();
			}
		}
		GameState.parseFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		//GameState.parseFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"); // Kiwipete
		// GameState.parseFEN("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"); // position 4
		// GameState.parseFEN("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"); // position 5
		// GameState.parseFEN("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1
		// w - - 0 10"); // position 6
		//GameState.parseFEN("n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1"); // promotion
		// GameState.parseFEN("rnbqkbnr/ppp3pp/4B3/p3R2p/1P6/8/PPPP1PPP/RNB1KBNR w - - 0
		// 1"); // double check
		//GameState.parseFEN("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"); //position 3
		// GameState.parseFEN("8/8/8/2k5/2p1p3/8/3P4/4K3 w - - 0 1"); // en passant
		// check evasion
		// GameState.parseFEN("8/6bb/8/8/R1pP2k1/4P3/P7/K7 b - d3 0 1"); // en passant
		// horizontal pin test

		for (Piece p : board) {
			if (p != null) {
				long pos = MoveLogic.squareToBB.get(p.squareIndex);
				occupied |= pos;
				piecePosition[p.color.ordinal()][p.type.ordinal()] |= pos;
				colorPositions[p.color.ordinal()] |= pos;
			}
		}

		long startTime = System.nanoTime();
		int num = perft(5);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;
		System.out.println("time: " + duration + " ms");
		System.out.println("DEPTH 6: " + num + " nodes");
		System.out.println("CAPTURES: " + captures);
	}

	public static int perft(int depth) {

		int nodes = 0;
		if (depth == 0) {
			return 1;
		}
		List<Move> moves = generateMoves();

		for (int i = 0; i < moves.size(); i++) {

			mType type = moves.get(i).type;
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
			// if (depth == 1 && (type == mType.CASTLE)) {
			// captures++;
			// }

			makeMove(moves.get(i));
			nodes += perft(depth - 1);
			unmakeMove(moves.get(i));
		}
		return nodes;
	}

	public static void unmakeMove(Move move) {

		int fromIndex = MoveLogic.BBtoSquare.get(move.from);
		int toIndex = MoveLogic.BBtoSquare.get(move.to);
		long fromTo = move.from ^ move.to;
		Type type = board[toIndex].type;

		position = stack.pop();

		switch (move.type) {

			case DOUBLE_PUSH:
			case QUIET:

				piecePosition[position.turn][type.ordinal()] ^= fromTo;

				board[fromIndex] = board[toIndex];
				board[toIndex] = null;
				board[fromIndex].squareIndex = fromIndex;

				break;

			case PROMO:

				piecePosition[position.turn][Type.PAWN.ordinal()] ^= move.from;
				piecePosition[position.turn][move.promoType.ordinal()] ^= move.to;

				pieceList[position.turn][type.ordinal()].remove(board[toIndex]); //new addition
				pieceList[position.turn][Type.PAWN.ordinal()].add(board[toIndex]); //new addition


				board[fromIndex] = board[toIndex];
				board[toIndex] = null;
				board[fromIndex].squareIndex = fromIndex;
				board[fromIndex].type = Type.PAWN; //need to chagen position of piecelist wow

				break;

			case EP_CAPTURE:

				pieceList[position.turn^1][move.captured.type.ordinal()].add(move.captured);

				long captureBB = MoveLogic.rankMasks[fromIndex / 8] & MoveLogic.fileMasks[toIndex % 8];
				int captureIndex = MoveLogic.BBtoSquare.get(captureBB);

				colorPositions[position.turn ^ 1] ^= captureBB;

				piecePosition[position.turn ^ 1][Type.PAWN.ordinal()] ^= captureBB;
				piecePosition[position.turn][Type.PAWN.ordinal()] ^= fromTo;

				board[fromIndex] = board[toIndex];
				board[toIndex] = null;
				board[fromIndex].squareIndex = fromIndex;

				board[captureIndex] = move.captured;

				break;

			case CAPTURE:

				Piece captured = move.captured;
				pieceList[position.turn^1][move.captured.type.ordinal()].add(move.captured);

				colorPositions[position.turn ^ 1] ^= move.to;

				piecePosition[position.turn ^ 1][captured.type.ordinal()] ^= move.to;
				piecePosition[position.turn][type.ordinal()] ^= fromTo;

				board[fromIndex] = board[toIndex];
				board[fromIndex].squareIndex = fromIndex;
				board[toIndex] = move.captured;

				break;

			case PROMO_CAPTURE:

				pieceList[position.turn^1][move.captured.type.ordinal()].add(move.captured);
				pieceList[position.turn][type.ordinal()].remove(board[toIndex]); //new addition
				pieceList[position.turn][Type.PAWN.ordinal()].add(board[toIndex]); //new addition

				colorPositions[position.turn ^ 1] ^= move.to;

				piecePosition[position.turn ^ 1][move.captured.type.ordinal()] ^= move.to;
				piecePosition[position.turn][move.promoType.ordinal()] ^= move.to;
				piecePosition[position.turn][Type.PAWN.ordinal()] ^= move.from;

				board[fromIndex] = board[toIndex];
				board[fromIndex].squareIndex = fromIndex;
				board[fromIndex].type = Type.PAWN;
				board[toIndex] = move.captured;

				break;

			case CASTLE:

				piecePosition[position.turn][Type.KING.ordinal()] ^= fromTo;

				board[fromIndex] = board[toIndex];
				board[toIndex] = null;
				board[fromIndex].squareIndex = fromIndex;

				long rookFrom = 0;
				long rookTo = 0;

				if (move.to > move.from) {
					rookFrom = move.from << 3;
					rookTo = move.from << 1;
				} else {
					rookFrom = move.from >> 4;
					rookTo = move.from >> 1;
				}

				long rookFromTo = rookFrom ^ rookTo;

				int rookFromIdx = MoveLogic.BBtoSquare.get(rookFrom);
				int rookToIdx = MoveLogic.BBtoSquare.get(rookTo);

				piecePosition[position.turn][Type.ROOK.ordinal()] ^= rookFromTo;

				colorPositions[position.turn] ^= rookFromTo;
				board[rookFromIdx] = board[rookToIdx];
				board[rookToIdx] = null;
				board[rookFromIdx].squareIndex = rookFromIdx;

				break;
			default:
				System.out.println("Invalid unmake move");
				break;
		}

		colorPositions[position.turn] ^= fromTo;
		occupied = colorPositions[0] | colorPositions[1];

	}

	/**
	 * Player turn is same color as the move
	 * 
	 * @param move
	 */
	public static void makeMove(Move move) {

		int fromIndex = MoveLogic.BBtoSquare.get(move.from);
		int toIndex = MoveLogic.BBtoSquare.get(move.to);

		long fromTo = move.from ^ move.to;

		Type type = board[fromIndex].type;
		stack.add(new Position(position));

		position.epSquare = 64;
		position.halfMoveClock++;

		switch (move.type) {

			case DOUBLE_PUSH:
				position.epSquare = fromIndex + (toIndex - fromIndex) / 2;
			case QUIET:
				piecePosition[position.turn][type.ordinal()] ^= fromTo;
				break;

			case PROMO:

				piecePosition[position.turn][Type.PAWN.ordinal()] ^= move.from;
				piecePosition[position.turn][move.promoType.ordinal()] ^= move.to;
				pieceList[position.turn][move.promoType.ordinal()].add(board[fromIndex]); //new addition
				pieceList[position.turn][Type.PAWN.ordinal()].remove(board[fromIndex]); //new addition
				board[fromIndex].type = move.promoType;
				break;

			case EP_CAPTURE:

				long captureBB = MoveLogic.rankMasks[fromIndex / 8] & MoveLogic.fileMasks[toIndex % 8];
				int captureIndex = MoveLogic.BBtoSquare.get(captureBB);
				move.captured = new Piece(board[captureIndex]);

				pieceList[position.turn^1][board[captureIndex].type.ordinal()].remove(board[captureIndex]);

				colorPositions[position.turn ^ 1] ^= captureBB;
				piecePosition[position.turn ^ 1][Type.PAWN.ordinal()] ^= captureBB;
				piecePosition[position.turn][Type.PAWN.ordinal()] ^= fromTo;
				board[captureIndex] = null;
				break;

			case CAPTURE:

				Type capturedType = move.captured.type;
				pieceList[position.turn^1][board[toIndex].type.ordinal()].remove(board[toIndex]);


				colorPositions[position.turn ^ 1] ^= move.to;
				piecePosition[position.turn ^ 1][capturedType.ordinal()] ^= move.to;
				piecePosition[position.turn][type.ordinal()] ^= fromTo;
				break;

			case PROMO_CAPTURE:

				pieceList[position.turn^1][board[toIndex].type.ordinal()].remove(board[toIndex]);
				pieceList[position.turn][move.promoType.ordinal()].add(board[fromIndex]); //new addition
				pieceList[position.turn][Type.PAWN.ordinal()].remove(board[fromIndex]); //new addition


				colorPositions[position.turn ^ 1] ^= move.to;
				board[fromIndex].type = move.promoType;
				piecePosition[position.turn ^ 1][move.captured.type.ordinal()] ^= move.to;
				piecePosition[position.turn][Type.PAWN.ordinal()] ^= move.from;
				piecePosition[position.turn][move.promoType.ordinal()] ^= move.to;
				break;

			case CASTLE:

				piecePosition[position.turn][Type.KING.ordinal()] ^= fromTo;

				long rookFrom = 0;
				long rookTo = 0;
				if (move.to > move.from) {
					rookFrom = move.from << 3;
					rookTo = move.from << 1;
				} else {
					rookFrom = move.from >> 4;
					rookTo = move.from >> 1;
				}
				long rookFromTo = rookFrom ^ rookTo;
				int rookFromIdx = MoveLogic.BBtoSquare.get(rookFrom);
				int rookToIdx = MoveLogic.BBtoSquare.get(rookTo);

				piecePosition[position.turn][Type.ROOK.ordinal()] ^= rookFromTo;
				colorPositions[position.turn] ^= rookFromTo;
				board[rookFromIdx].squareIndex = rookToIdx;
				board[rookToIdx] = board[rookFromIdx];
				board[rookFromIdx] = null;
				break;

			default:
				System.out.println("Invalid move");
				break;
		}

		colorPositions[position.turn] ^= fromTo;
		board[fromIndex].squareIndex = toIndex;
		board[toIndex] = board[fromIndex];
		board[fromIndex] = null;
		occupied = colorPositions[0] | colorPositions[1];
		updateCastlingRights(type);
		position.turn ^= 1;
	}

	public static List<Move> generateMoves() {

		attackedSquares[0] = 0;
		attackedSquares[1] = 0;

		for (List<Piece> list : pieceList[position.turn ^ 1]) {
			for (Piece p : list) {
				attackedSquares[position.turn ^ 1] |= p.generateAttackedSquares();
			}
		}

		// determine check masks so can generate right legal moves
		long kingPos = GameState.piecePosition[position.turn][Type.KING.ordinal()];
		int kingIndex = MoveLogic.BBtoSquare.get(kingPos);

		long checkers = MoveLogic.getAttackersToKing(position.turn ^ 1, kingIndex);
		if (checkers != 0) {
			MoveLogic.updateCheckMasks(kingIndex, checkers);
		}
		// find pins
		MoveLogic.findAbsolutePins();

		List<Move> moveList = new ArrayList<Move>();
		// generate legal moves to add to move list
		// also update the attacked squares in the process

		for (int i = 1; i < 6; i++) {
			for (Piece p : pieceList[position.turn][i]) {
				p.generateLegalMoves();
				addMoves(p.squareIndex, p.legalMoves, p.type, moveList);
				attackedSquares[p.color.ordinal()] |= p.attackedSquares;
			}
		}
		
		for (Piece p : pieceList[position.turn][0]) {
			p.generateLegalPawnMoves();
			addPawnMoves(p.squareIndex, p.legalMoves, moveList);
			attackedSquares[p.color.ordinal()] |= p.attackedSquares;
		}

		generateCastleMoves(kingPos, moveList);
		generateEpMoves(kingIndex, moveList);

		// reset check mask
		MoveLogic.capture_mask = -1;
		MoveLogic.push_mask = -1;
		MoveLogic.king_mask = 0;

		return moveList;
	}


	private static void addPawnMoves(int fromIndex, long move, List<Move> moves) {

		long from = MoveLogic.squareToBB.get(fromIndex);
		while (move != 0) {
			long to = move & -move;
			int toIndex = Long.numberOfTrailingZeros(move);
			Move m = new Move(from, to, mType.QUIET, Type.QUEEN, new Piece(board[toIndex]));
			m.type = MoveLogic.pawnMoves[fromIndex][toIndex];

			if (m.type == mType.PROMO || m.type == mType.PROMO_CAPTURE) {
				moves.add(new Move(from, to, m.type, Type.ROOK, new Piece(board[toIndex])));
				moves.add(new Move(from, to, m.type, Type.KNIGHT, new Piece(board[toIndex])));
				moves.add(new Move(from, to, m.type, Type.BISHOP, new Piece(board[toIndex])));
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
	private static void updateCastlingRights(Type type) {

		if (type == Type.KING) { // what if king eats other rook?
			position.castlingRights[position.turn * 2] = false;
			position.castlingRights[position.turn * 2 + 1] = false;
			return;
		}

		for (int color = 0; color < 2; color++) {
			for (int side = 0; side < 2; side++) {
				int sqr = MoveLogic.initialRookSquares[color][side];
				if (board[sqr] == null || (board[sqr].color.ordinal() != color)) {
					position.castlingRights[color * 2 + side] = false;
				}
			}
		}
	}

	private static void generateCastleMoves(long kingPos, List<Move> moves) {

		if ((kingPos & attackedSquares[position.turn ^ 1]) != 0) {
			return;
		}

		for (int i = 0; i < 2; i++) {
			if (position.castlingRights[position.turn * 2 + i]) {
				if ((MoveLogic.castleAttackedSquares[position.turn][i] & attackedSquares[position.turn ^ 1]) == 0) {
					if ((MoveLogic.castleOccupiedSquares[position.turn][i] & occupied) == 0) {
						int to = MoveLogic.castleTargetSquares[position.turn][i];
						Move castle = new Move(kingPos, MoveLogic.squareToBB.get(to), mType.CASTLE, null);
						moves.add(castle);
					}
				}
			}
		}
	}

	private static void generateEpMoves(int kingIndex, List<Move> moves) {

		long epPos = MoveLogic.pawnAttacks[position.turn ^ 1][position.epSquare]
				& piecePosition[position.turn][Type.PAWN.ordinal()];

		while (epPos != 0) {

			int from = Long.numberOfTrailingZeros(epPos);
			long fr = MoveLogic.squareToBB.get(from);
			long epPush = MoveLogic.squareToBB.get(position.epSquare);
			long epCapture = MoveLogic.rankMasks[from / 8] & MoveLogic.fileMasks[position.epSquare % 8];

			if (((MoveLogic.capture_mask & epCapture) & board[from].pin_mask) != 0) {

				long occ = GameState.occupied ^ fr ^ epCapture;
				long rookQueenPos = piecePosition[position.turn ^ 1][Type.QUEEN.ordinal()]
						| piecePosition[position.turn ^ 1][Type.ROOK.ordinal()];

				if ((MoveLogic.sliding_moves(kingIndex, occ, MoveLogic.rankMasks[kingIndex / 8]) & rookQueenPos) == 0) {
					moves.add(new Move(fr, epPush, mType.EP_CAPTURE, Type.QUEEN));
				}
			}
			epPos &= (epPos - 1);
		}

	}

	private static void addMoves(int fromIndex, long move, Type type, List<Move> moves) {

		long from = MoveLogic.squareToBB.get(fromIndex);

		long nonCaptures = move & ~occupied;
		while (nonCaptures != 0) {
			long to = nonCaptures & -nonCaptures;
			moves.add(new Move(from, to, mType.QUIET, null));
			nonCaptures &= (nonCaptures - 1);
		}

		long captures = move & occupied;
		while (captures != 0) {
			int toIndex = Long.numberOfTrailingZeros(captures);
			long to = captures & -captures;
			moves.add(new Move(from, to, mType.CAPTURE, null, new Piece(board[toIndex])));
			captures &= (captures - 1);
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
			//List<Piece> list = (color == Colour.BLACK) ? GameState.pieceArr[1] : GameState.pieceArr[0];
			Piece p = null;

			switch (Character.toLowerCase(c)) {
				case 'p':
					p = new Piece(idx, Type.PAWN, color);
					break;
				case 'b':
					p = new Piece(idx, Type.BISHOP, color);
					break;
				case 'n':
					p = new Piece(idx, Type.KNIGHT, color);
					break;
				case 'r':
					p = new Piece(idx, Type.ROOK, color);
					break;
				case 'q':
					p = new Piece(idx, Type.QUEEN, color);
					break;
				case 'k':
					p = new Piece(idx, Type.KING, color);
					break;
				default:
					System.err.println("Invalid FEN");
			}
			GameState.board[idx] = p;
			pieceList[p.color.ordinal()][p.type.ordinal()].add(p);
			//list.add(p);
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
		String YELLOW = "\u001B[33m";
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
			if (board[idx] != null) {
				char c = Type.getFenChar(board[idx].type);
				String p = (board[idx].color == Colour.WHITE) ? MAGENTA + c + RESET
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

			if (board[i] == null) {
				emptyCount++;
			} else {
				if (emptyCount > 0) {
					row.append(emptyCount);
					emptyCount = 0;
				}
				char c = Type.getFenChar(board[i].type);
				if (board[i].color == Colour.BLACK) {
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
