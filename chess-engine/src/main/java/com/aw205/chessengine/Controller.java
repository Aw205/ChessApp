package com.aw205.chessengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aw205.chessengine.engine.GameState;
import com.aw205.chessengine.engine.Search;
import com.aw205.chessengine.engine.Move;
import com.aw205.chessengine.engine.MoveLogic;
import com.aw205.chessengine.engine.Piece;
import com.aw205.chessengine.engine.Type;
import com.aw205.chessengine.engine.mType;

@CrossOrigin(origins = { "*" })
@RestController
public class Controller {

    public Map<String, Integer> moveMap = new HashMap<String, Integer>();

    @RequestMapping("/legalMoves")
    public List<_Move> getLegalMoves() {

        long kingPos = GameState.piecePosition[GameState.position.turn ^ 1][Type.KING];
        int kingIndex = Long.numberOfTrailingZeros(kingPos);

        List<_Move> moves = new ArrayList<_Move>();

        for (int m : GameState.generateMoves()) {

            GameState.makeMove(m);
            String fen = GameState.getFen();
            long checkers = MoveLogic.getAttackersToKing(GameState.position.turn ^ 1, kingIndex);
            GameState.unmakeMove(m);

            _Move m1 = new _Move(m, fen);
            if (checkers != 0) {
                m1.moveType = "CHECK";
                m1.notation+= "+";
            }
          
            moves.add(m1);
            moveMap.put(m1.getID(), m);
        }
        return moves;
    }

    @PutMapping("/updateGameState")
    public void updateGameState(@RequestBody String id) {
        GameState.makeMove(moveMap.get(id));
    }

    @RequestMapping("/getBestMove")
    public _Move getBestMove() {

        int move = Search.rootSearch(Integer.MIN_VALUE, Integer.MAX_VALUE, 4);

        GameState.makeMove(move);
        String fen = GameState.getFen();
        GameState.unmakeMove(move);

        return new _Move(move, fen);
    }
}

class _Move {

    public String from;
    public String to;
    public String moveType;
    public char fromType;
    public char promoType;
    public String notation;
    public String fen;
    public String id;

    public _Move(int m, String fen) {

        this.from = moveToCoord(Move.getFromSquare(m));
        this.to = moveToCoord(Move.getToSquare(m));

        this.fromType = Type.getFenChar(Piece.getType(GameState.board[Move.getFromSquare(m)])); 

        this.moveType = mType.getMoveString(Move.getMoveType(m));
        this.promoType = Type.getFenChar(Move.getPromoType(m));

        this.notation = this.getNotation();

        this.fen = fen;
        this.id = this.getID();

    }

    public String getID() {
        return this.from + this.to + this.promoType;
    }

    private String moveToCoord(int square) {

        int row = 1 + (square / 8);
        char column = (char) ('a' + (square % 8));
        return String.valueOf(column) + row;
    }

    private String getNotation() {

        String notation = "";
        if (this.moveType.equals("CAPTURE")) {
            char prefix = (Character.toLowerCase(this.fromType) == 'p') ? this.from.charAt(0) : this.fromType;
            notation = prefix + "x" + this.to;
        } else if (this.moveType.equals("CASTLE")) {
            notation = (this.to.equals("g1") || this.to.equals("g8")) ? "O-O" : "O-O-O";
        } else if (this.moveType.equals("PROMO")) {
            notation = this.to + "=" + this.promoType;
        } else if (this.moveType.equals("PROMO_CAPTURE")) {
            notation = this.from.charAt(0) + "x" + this.to + "=" + this.promoType;
        } else {
            String prefix = (Character.toLowerCase(this.fromType) == 'p') ? "" : String.valueOf(this.fromType);
            notation = prefix + this.to;
        }

        return notation;
    }
}
