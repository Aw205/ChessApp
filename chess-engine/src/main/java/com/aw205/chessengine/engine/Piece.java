package com.aw205.chessengine.engine;

import org.springframework.web.bind.annotation.GetMapping;

public class Piece {
	
	Type type = Type.BISHOP;

	public int squareIndex = 0;
	public Colour color;
	
	public long pin_mask = -1; // squares the piece can travel along the pin
	public long legalMoves = 0;
	public long pseudoLegalMoves =0;
	public long attackedSquares =0;
	
	
	public Piece(int squareIndex,Type type,Colour color) {
		
		this.type = type;
		this.color = color;
		this.squareIndex = squareIndex;

	}

	public Piece(Piece p ){

		if(p == null) return;

		this.type = p.type;
		this.squareIndex = p.squareIndex;
		this.color = p.color;
		this.pin_mask = p.pin_mask;
		this.legalMoves = p.legalMoves;
		this.pseudoLegalMoves = p.pseudoLegalMoves;
		this.attackedSquares = p.attackedSquares;


	}
	
	public void generateLegalMoves() {
		
		generateAttackedSquares();
		pseudoLegalMoves = MoveLogic.filterPseudoLegalMoves(attackedSquares, color);
		calcPinMask();
		legalMoves = MoveLogic.filterLegalMoves(type, color, pin_mask, pseudoLegalMoves);
	}

	public void generateLegalPawnMoves(){

		generateAttackedSquares();
		pseudoLegalMoves = MoveLogic.filterPseudoLegalMoves(attackedSquares, color);

		pseudoLegalMoves &= GameState.occupied; 
		pseudoLegalMoves |= MoveLogic.single_pawn_push(squareIndex, color); 
		pseudoLegalMoves |= MoveLogic.double_pawn_push(squareIndex, color);

		calcPinMask();
		legalMoves = MoveLogic.filterLegalMoves(type, color, pin_mask, pseudoLegalMoves);
	}
	
	public void calcPinMask() {
		
		//can pass in king index
		pin_mask = -1;
		long from = MoveLogic.squareToBB.get(squareIndex);
		boolean isPinned = (from & MoveLogic.pinned) !=0;
		if(isPinned) {
			long kingPos = GameState.piecePosition[color.ordinal()][Type.KING.ordinal()];
			int kingIndex = MoveLogic.BBtoSquare.get(kingPos);
			pin_mask = MoveLogic.squaresToLine[squareIndex][kingIndex];	
		}
	}
	
	public long generateAttackedSquares(){

		switch (type) {
			case PAWN:
				attackedSquares = MoveLogic.pawnAttacks[color.ordinal()][squareIndex];
				break;
			case BISHOP:
				attackedSquares = MoveLogic.bishop_moves(squareIndex,GameState.occupied);
				break;
			case KNIGHT:
				attackedSquares = MoveLogic.knightAttacks[squareIndex];
				break;
			case ROOK:
				attackedSquares = MoveLogic.rook_moves(squareIndex,GameState.occupied);
				break;
			case QUEEN:
				attackedSquares = MoveLogic.rook_moves(squareIndex,GameState.occupied);
				attackedSquares |= MoveLogic.bishop_moves(squareIndex,GameState.occupied);
				break;
			case KING:
				attackedSquares = MoveLogic.kingAttacks[squareIndex];
				break;
			default:
		}
		return attackedSquares;
	}

}
