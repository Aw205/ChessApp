package com.aw205.chessengine.engine;

public class Position{

    public byte castlingRights = 0; // wqs, wks, bqs, bks
    public int epSquare = 64; // 64 for no en passant
    public int halfMoveClock = 0 ;
    public int turn = 0;

    Position(){

    }

    Position(Position p ){

        this.castlingRights = p.castlingRights;
        this.epSquare = p.epSquare;
        this.halfMoveClock = p.halfMoveClock;
        this.turn = p.turn;

    }

}