package com.aw205.chessengine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.aw205.chessengine.engine.GameState;

@SpringBootApplication
public class ChessEngineApplication {

	public static void main(String[] args) {

		GameState.init();
		//SpringApplication.run(ChessEngineApplication.class, args);
		
	}
	
}
