package com.aw205.chessengine.engine;

public enum Colour {
	
	WHITE,BLACK;
	
	private static final Colour[] VALUES = values();
	
	public Colour opposite() {
		return VALUES[this.ordinal()^1];
	}

}
