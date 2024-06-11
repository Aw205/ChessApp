# Chess

A custom chess engine.

## Implementation

### Move generation

Note that this doesn't encompass legal moves, which require additional tests for checks and pins.

<details open>
<summary> King </summary>

 - Precalculate the king moves given the position `square`.

```java
        long east = (square << 1) & ~fileMasks[0]; // ~fileMasks[0] ensures that overflow is adjusted for
		long west = (square >> 1) & ~fileMasks[7];
		long attack = (east | west);
		attack |= (attack << 8 | attack >> 8); // finding the top and bottom adjacent squares
		
		return (attack | (square >> 8)| (square << 8)); // finding the diagonals
```
</details>

<details open>
<summary> Knight </summary>

-  Precalcuate the knight moves given a position `pos`.

```java

        long east = 0;
        long west = 0;
        long attacks = 0;

		east = (pos << 1) & ~fileMasks[0];
		west = (pos >> 1) & ~fileMasks[7];
		attacks = (east | west) << 16 & (~rankMasks[0] & ~rankMasks[1]); //down 2 left/right 1
		attacks |= (east | west) >> 16 & (~rankMasks[7] & ~rankMasks[6]); //up 2 left/right 1
		east = (east << 1) & ~fileMasks[0];
		west = (west >> 1) & ~fileMasks[7];
		attacks |= (east | west) << 8 & (~rankMasks[0]); // left/right 2 up 1
		attacks |= (east | west) >> 8 & (~rankMasks[7]); // left/right 2 down 1

        // again, rank and file masks account for moves out of bounds

		return attacks;

```
</details>


<details open>
<summary> Bishop </summary>

- Operates with sliding moves, so we just need to know which diagonals the bishops sits on.

```java 

        int row = square / 8;
		int col = square % 8;
		int diagIndex = (row-col) & 15; // index of the diagonal the bishop is on -> '/'
		int antiDiagIndex = (row+col) ^ 7; // index of the anti-diagonal -> '\'
		
		return sliding_moves(square,occupied,diagMasks[diagIndex]) | sliding_moves(square,occupied,antiDiagMasks[antiDiagIndex]);

```

</details>

<details open>
<summary> Rook </summary>

- Like the bishop, the rook operates with sliding moves but instead with a rank/files mask instead of diag/anti-diag masks.

```java
        return sliding_moves(square,occupied,rankMasks[square/8]) | sliding_moves(square,occupied,fileMasks[square%8]); 
```
</details>

<details open>
<summary> Pawn </summary>

- Pre
</details>

<details open>
<summary> Queen </summary>

- This is simply the union of the bishop and rook moves.

```java

        long attackedSquares = MoveLogic.rook_moves(squareIndex,GameState.occupied);
	    attackedSquares |= MoveLogic.bishop_moves(squareIndex,GameState.occupied);

```
</details>

The rook and bishop moves employ a technique called [hyperbola quintessence](https://www.chessprogramming.org/Hyperbola_Quintessence) to generate the sliding moves. 


### Checks & Pins

In order to determine if a move is legal, we have to know whether our king is in check and if a piece is pinned to the king. For checks, we look at the attackers to our king

```java

        long rookPos, bishopPos, pawnPos, knightPos;

        // getting all the positions of the pieces
		
		pawnPos = GameState.piecePosition[pColor.ordinal()][Type.PAWN.ordinal()];
	    knightPos = GameState.piecePosition[pColor.ordinal()][Type.KNIGHT.ordinal()];
		rookPos = bishopPos = GameState.piecePosition[pColor.ordinal()][Type.QUEEN.ordinal()];
		bishopPos |= GameState.piecePosition[pColor.ordinal()][Type.BISHOP.ordinal()];
		rookPos |= GameState.piecePosition[pColor.ordinal()][Type.ROOK.ordinal()];
	
		return (pawnAttacks[pColor.opposite().ordinal()][kingIndex] & pawnPos) | (knightAttacks[kingIndex] & knightPos)
				| (bishop_moves(kingIndex,GameState.occupied) & bishopPos) | (rook_moves(kingIndex,GameState.occupied) & rookPos);


```

### Make/Unmake 

When a move is made, the game state needs to be updated to reflect the new position. 

- `occupied` - a bitboard of the squares occupied by pieces.
- `colorPositions` - an array of bitboards representing the occupied squares of each color (black/white).
- `attackedSquares` - an array of bitboards representing the combined attacked squares of the same colored pieces.
- `piecePosition` - a 2D array of bitboards representing the position of each piece like black knights, white queen, etc. The array is indexed by color and piece type.
- `castlingRights` - a byte with each bit representing the white and black castling rights. 
- `pieceArr` - an array of 2 Lists representing the black and white pieces.
- `board` - an array acting as the square centric representation of the pieces on the board.





## Performance

### Perft Tests

Starting Position: `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`

| Depth  | Nodes | Captures | En Passant | Castles | Promotions | Checks | Discovery Checks | Double Checks | Checkmate |
| ------ | ----- | -------- | ---------- | ------- | ---------- | ------ | ---------------- | ------------- | --------- | 
| 0      |       |          |            |         |            |        |                  |               |           |
| 1      |       |          |            |         |            |        |                  |               |           |
| 2      |       |          |            |         |            |        |                  |               |           |


