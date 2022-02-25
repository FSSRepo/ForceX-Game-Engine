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
	
	public static char toKeyChar(byte key,boolean mayus){
		switch(key){
			case A_KEY:
				return mayus ? 'A' : 'a';
			case B_KEY:
				return mayus ? 'B' : 'b';
			case C_KEY:
				return mayus ? 'C' : 'c';
			case D_KEY:
				return mayus ? 'D' : 'd';
			case E_KEY:
				return mayus ? 'E' : 'e';
			case F_KEY:
				return mayus ? 'F' : 'f';
			case G_KEY:
				return mayus ? 'G' : 'g';
			case H_KEY:
				return mayus ? 'H' : 'h';
			case I_KEY:
				return mayus ? 'I' : 'i';
			case J_KEY:
				return mayus ? 'J' : 'j';
			case K_KEY:
				return mayus ? 'K' : 'k';
			case L_KEY:
				return mayus ? 'L' : 'l';
			case M_KEY:
				return mayus ? 'M' : 'm';
			case N_KEY:
				return mayus ? 'N' : 'n';
			case O_KEY:
				return mayus ? 'O' : 'o';
			case P_KEY:
				return mayus ? 'P' : 'p';
			case Q_KEY:
				return mayus ? 'Q' : 'q';
			case R_KEY:
				return mayus ? 'R' : 'r';
			case S_KEY:
				return mayus ? 'S' : 's';
			case T_KEY:
				return mayus ? 'T' : 't';
			case V_KEY:
				return mayus ? 'V' : 'v';
			case U_KEY:
				return mayus ? 'U' : 'u';
			case W_KEY:
				return mayus ? 'W' : 'w';
			case X_KEY:
				return mayus ? 'X' : 'x';
			case Y_KEY:
				return mayus ? 'Y' : 'y';
			case Z_KEY:
				return mayus ? 'Z' : 'z';
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
