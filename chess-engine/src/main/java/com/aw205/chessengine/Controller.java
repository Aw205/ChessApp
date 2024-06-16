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
import com.aw205.chessengine.engine.Move;
import com.aw205.chessengine.engine.MoveLogic;
import com.aw205.chessengine.engine.Type;
import com.aw205.chessengine.engine.mType;

@CrossOrigin(origins = { "*" })
@RestController
public class Controller {

    public Map<String, Integer> moveMap = new HashMap<String, Integer>();

    @RequestMapping("/legalMoves")
    public List<_Move> getLegalMoves() {

        long kingPos = GameState.piecePosition[GameState.position.turn ^ 1][Type.KING.ordinal()];
        int kingIndex = Long.numberOfTrailingZeros(kingPos);

        List<_Move> moves = new ArrayList<_Move>();

        for (int m : GameState.generateMoves()) {

            GameState.makeMove(m);
            String fen = GameState.getFen();
            long checkers = MoveLogic.getAttackersToKing(GameState.position.turn ^ 1, kingIndex);
            GameState.unmakeMove(m);

            _Move m1 = new _Move(m, fen);
            if (checkers != 0) {
                m1.type = "CHECK";
            }
            moves.add(m1);
            moveMap.put(m1.getID(), m);
        }
        return moves;
    }

    @PutMapping("/updateGameState")
    public void updateGameState(@RequestBody String fromTo) {
        GameState.makeMove(moveMap.get(fromTo));
        //GameState.printDebug();
    }
}

class _Move {

    public String from;
    public String to;
    public String type;
    public String promoType = "";
    public String fen;
    public String id;

    public _Move(int m, String fen) {

        this.from = moveToCoord(Move.getFromSquare(m));
        this.to = moveToCoord(Move.getToSquare(m));

        this.type = mType.valMTypes[Move.getMoveType(m)].toString();
        
        if(this.type.equals("PROMO") || this.type.equals("PROMO_CAPTURE")){ //fix this
            this.promoType = Type.values()[Move.getPromoType(m)].toString();
        }
       
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
}
