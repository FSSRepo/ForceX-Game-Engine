package com.forcex.io;
import com.forcex.math.*;

public interface BinaryReader
{
	int readInt();
	int readUShort();
	short readUbyte();
	byte readByte();
	short readShort();
	float readFloat();
	String readString(int len);
	String scanString();
	float[] readFloatArray(int len);
	short[] readShortArray(int len);
	byte[] readByteArray(int len);
	Quaternion readQuaternion();
	Vector3f readVector();
	boolean readBoolean();
	void skip(int len);
	void clear();
}
