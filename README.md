# Poko

A chess engine.

## Performance

### Perft Tests

Starting Position: `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`

<details open>
<summary> Results </summary>

| Depth  | Nodes        | Time (ms) | Time w/ bulk-counting (ms) |
| ------ | ------------ | --------- | -------------------------- |
| 1      | 20 			| 0         | 0        
| 2      | 400      	| 1    	    | 0        
| 3      | 8902      	| 6    	    | 3
| 4      | 197,281      | 23    	| 14   
| 5      | 4,865,609	| 267    	| 161   
| 6      | 119,060,324  | 5485    	| 2591 
| 7      | 3,195,901,860| 136,134   | 61,802

</details>

\
Kiwipete (Position 2 on CPW): `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1`

<details>
<summary> Results </summary>

| Depth  | Nodes        | Time (ms) | Time w/ bulk-counting (ms) |
| ------ | ------------ | --------- | -------------------------- |
| 1      | 48 			| 0         | 0     
| 2      | 2039      	| 3         | 1     
| 3      | 97,862      	| 16        | 7
| 4      | 4,085,603    | 196       | 87
| 5      | 193,690,690	| 7170      | 2412
| 6      | 8,031,647,685| 300,512   | 106,694  
</details>

\
Position 3 on CPW: `8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1`

<details>
<summary> Results </summary>

| Depth  | Nodes        | Time (ms) | Time w/ bulk-counting (ms) |
| ------ | ------------ | --------- | -------------------------- |
| 1      | 14 			|  0        | 0    
| 2      | 191      	|  0   		| 0        
| 3      | 2812      	|  3    	| 1
| 4      | 43,238    	|  10    	| 6   
| 5      | 674,624		|  46    	| 30   
| 6      | 11,030,083 	|  503    	| 223
| 7      | 178,633,661 	|  7730    	| 3122
| 8      | 3,009,794,393|  124,691  | 48,912   

</details>


## Implementation

A brief overview of the **move generation**, **search**, and **evaluation**.

### Move generation

#### Calculating Attacked Squares 

<details>
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

<details>
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

        // rank and file masks account for moves out of bounds

		return attacks;

```
</details>


<details>
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

<details>
<summary> Rook </summary>

- Like the bishop, the rook operates with sliding moves but instead with a rank/files mask instead of diag/anti-diag masks.

```java
        return sliding_moves(square,occupied,rankMasks[square/8]) | sliding_moves(square,occupied,fileMasks[square%8]); 
```
</details>

<details>
<summary> Pawn </summary>

- Since pawns can't exist on the 1st or 8th rank, rank masks are uneccessary.

```java
       	long pawn_move = 1L << square;
		long push = (color == 0) ? pawn_move << 8 : pawn_move >> 8;
		return push & ~GameState.occupied; 
```

</details>

<details>
<summary> Queen </summary>

- This is the union of the bishop and rook moves.

```java

        long attackedSquares = MoveLogic.rook_moves(squareIndex,GameState.occupied);
	    attackedSquares |= MoveLogic.bishop_moves(squareIndex,GameState.occupied);

```
</details>

\
The sliding pieces employ a technique called [hyperbola quintessence](https://www.chessprogramming.org/Hyperbola_Quintessence) to generate the sliding moves. 


#### Checks & Pins

To determine if a move is legal, we have to know whether our king is in check and the position of the pinned pieces.


<details>
<summary> Finding checks </summary>

- We generate different attacks from the position of the king and intersect it with the position of the corresponding enemy pieces. The result is a bitboard of all pieces that are giving checks.

```java
 		// getting positions of all the pieces

		long pawnPos = GameState.piecePosition[color][Type.PAWN];
		long knightPos = GameState.piecePosition[color][Type.KNIGHT];
		long rookPos = bishopPos = GameState.piecePosition[color][Type.QUEEN];
		long bishopPos |= GameState.piecePosition[color][Type.BISHOP];
		rookPos |= GameState.piecePosition[color][Type.ROOK];

		return (pawnAttacks[color ^ 1][kingIndex] & pawnPos) | (knightAttacks[kingIndex] & knightPos)
				| (bishop_moves(kingIndex, GameState.occupied) & bishopPos)
				| (rook_moves(kingIndex, GameState.occupied) & rookPos);

```
</details>

<details>
<summary> Finding absolute pins



</details>

#### Make/Unmake 

When a move is made, the game state needs to be updated to reflect the new position. 

- `occupied` - a bitboard of the squares occupied by pieces.
- `colorPositions` - an array of bitboards representing the occupied squares of each color (black/white).
- `attackedSquares` - an array of bitboards representing the combined attacked squares of the same colored pieces.
- `piecePosition` - a 2D array of bitboards representing the position of each piece indexed by color and piece type.
- `board` - a square centric array of the board.
- `stack` - a stack from which Position objects are pushed and popped during make/unmake.


### Search

Uses the Alpha-Beta algorithm.

Features currently implemented:

- Transposition tables
- Quiescence search
- Move ordering
	- Captures first
	- MMV_LVA
- Iterative deepening

### Evaluation


It's pretty simple right now.

- Material 
- Mobility
	- Attacked squares