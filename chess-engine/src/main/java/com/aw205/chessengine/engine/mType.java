package com.aw205.chessengine.engine;

public class mType {

	public static final int QUIET = 0;
	public static final int DOUBLE_PUSH = 1;
	public static final int CASTLE = 2;
	public static final int CAPTURE = 3;
	public static final int EP_CAPTURE = 4;
	public static final int PROMO = 5;
	public static final int PROMO_CAPTURE = 6;

	public static String getMoveString(int t) {

		switch (t) {
			case QUIET:
				return "QUIET";

			case DOUBLE_PUSH:
				return "DOUBLE_PUSH";

			case CASTLE:
				return "CASTLE";

			case CAPTURE:
				return "CAPTURE";

			case EP_CAPTURE:
				return "EP_CAPTURE";

			case PROMO:
				return "PROMO";

			case PROMO_CAPTURE:
				return "PROMO_CAPTURE";
		};
		return "";
	}

}
