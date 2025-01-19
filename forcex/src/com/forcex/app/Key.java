package com.forcex.app;

public class Key
{
	// alphabet key [English]
	public static final byte A_KEY = 1;
	public static final byte B_KEY = 2;
	public static final byte C_KEY = 3;
	public static final byte D_KEY = 4;
	public static final byte E_KEY = 5;
	public static final byte F_KEY = 6;
	public static final byte G_KEY = 7;
	public static final byte H_KEY = 8;
	public static final byte I_KEY = 9;
	public static final byte J_KEY = 10;
	public static final byte K_KEY = 11;
	public static final byte L_KEY = 12;
	public static final byte M_KEY = 13;
	public static final byte N_KEY = 14;
	public static final byte O_KEY = 15;
	public static final byte P_KEY = 16;
	public static final byte Q_KEY = 17;
	public static final byte R_KEY = 18;
	public static final byte S_KEY = 19;
	public static final byte T_KEY = 20;
	public static final byte U_KEY = 21;
	public static final byte V_KEY = 22;
	public static final byte W_KEY = 23;
	public static final byte X_KEY = 24;
	public static final byte Y_KEY = 25;
	public static final byte Z_KEY = 26;
	
	// directions keys
	public static final byte KEY_UP = 27;
	public static final byte KEY_DOWN = 28;
	public static final byte KEY_RIGHT = 29;
	public static final byte KEY_LEFT = 30;
	
	// numeric keys
	public static final byte KEY_0 = 31;
	public static final byte KEY_1 = 32;
	public static final byte KEY_2 = 33;
	public static final byte KEY_3 = 34;
	public static final byte KEY_4 = 35;
	public static final byte KEY_5 = 36;
	public static final byte KEY_6 = 37;
	public static final byte KEY_7 = 38;
	public static final byte KEY_8 = 39;
	public static final byte KEY_9 = 40;
	
	// especial keys
	public static final byte KEY_ESC = 41;
	public static final byte KEY_TAB = 42;
	public static final byte KEY_DEL = 44;
	public static final byte KEY_ENTER = 45;
	public static final byte KEY_SPACE = 46;
	public static final byte KEY_CAPITAL = 47;
	public static final byte KEY_DOT = 48;
	public static final byte KEY_MINUS = 49;

	public static final byte KEY_RIGHT_SHIFT = 50;
	public static final byte KEY_LEFT_SHIFT = 51;
	
	public static char toKeyChar(byte key,boolean capital_letter){
		switch(key){
			case A_KEY:
				return capital_letter ? 'A' : 'a';
			case B_KEY:
				return capital_letter ? 'B' : 'b';
			case C_KEY:
				return capital_letter ? 'C' : 'c';
			case D_KEY:
				return capital_letter ? 'D' : 'd';
			case E_KEY:
				return capital_letter ? 'E' : 'e';
			case F_KEY:
				return capital_letter ? 'F' : 'f';
			case G_KEY:
				return capital_letter ? 'G' : 'g';
			case H_KEY:
				return capital_letter ? 'H' : 'h';
			case I_KEY:
				return capital_letter ? 'I' : 'i';
			case J_KEY:
				return capital_letter ? 'J' : 'j';
			case K_KEY:
				return capital_letter ? 'K' : 'k';
			case L_KEY:
				return capital_letter ? 'L' : 'l';
			case M_KEY:
				return capital_letter ? 'M' : 'm';
			case N_KEY:
				return capital_letter ? 'N' : 'n';
			case O_KEY:
				return capital_letter ? 'O' : 'o';
			case P_KEY:
				return capital_letter ? 'P' : 'p';
			case Q_KEY:
				return capital_letter ? 'Q' : 'q';
			case R_KEY:
				return capital_letter ? 'R' : 'r';
			case S_KEY:
				return capital_letter ? 'S' : 's';
			case T_KEY:
				return capital_letter ? 'T' : 't';
			case V_KEY:
				return capital_letter ? 'V' : 'v';
			case U_KEY:
				return capital_letter ? 'U' : 'u';
			case W_KEY:
				return capital_letter ? 'W' : 'w';
			case X_KEY:
				return capital_letter ? 'X' : 'x';
			case Y_KEY:
				return capital_letter ? 'Y' : 'y';
			case Z_KEY:
				return capital_letter ? 'Z' : 'z';
			case KEY_0:
				return '0';
			case KEY_1:
				return '1';
			case KEY_2:
				return '2';
			case KEY_3:
				return '3';
			case KEY_4:
				return '4';
			case KEY_5:
				return '5';
			case KEY_6:
				return '6';
			case KEY_7:
				return '7';
			case KEY_8:
				return '8';
			case KEY_9:
				return '9';
		}
		return ' ';
	}
}
