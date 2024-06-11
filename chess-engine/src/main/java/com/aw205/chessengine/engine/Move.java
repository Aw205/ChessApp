package com.aw205.chessengine.engine;

public class Move {
	
	public long from = 0;
	public long to = 0;
	public mType type;
	public Type promoType = null;
	public Piece captured = null;
	
	public Move(long from, long to) {

		this.from = from;
		this.to = to;
		
	}
	
	public Move(long from, long to, mType type,Type promoType) {
		
		this.from = from;
		this.to = to;
		this.type = type;
		this.promoType = promoType;
		
	}

	public Move(long from, long to, mType type,Type promoType,Piece captured) {
		
		this.from = from;
		this.to = to;
		this.type = type;
		this.promoType = promoType;
		this.captured = captured;
		
	}

}