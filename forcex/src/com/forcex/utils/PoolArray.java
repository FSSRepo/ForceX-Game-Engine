package com.forcex.utils;
import java.util.*;

public class PoolArray<T> {
	private T[] buffer;
	private int count;
	private int index_input;
	private int index_output;
	
	public PoolArray(int maxCount){
		buffer = (T[])new Object[maxCount];
		index_input = 0;
		index_output = 0;
		count = 0;
	}
	
	public int count(){
		return count;
	}
	
	public boolean push(T item){
		if(count == buffer.length){
			return false;
		}
		buffer[index_input] = item;
		index_input = (index_input + 1) % buffer.length;
		count++;
		return true;
	}
	
	public T get(){
		if(count == 0){
			return null;
		}
		return buffer[index_output];
	}
	
	public void remove(){
		if(count == 0){
			return;
		}
		buffer[index_output] = null;
		index_output = (index_output + 1) % buffer.length;
		count--;
	}
	
	public void processNumPointers(){
		for(T t : buffer){
			if(t != null){
				count++;
			}
		}
	}
	
	public T pop() {
		if(count == 0){
			processNumPointers();
			if(count == 0) return null;
		}
		T item = buffer[index_output];
		buffer[index_output] = null;
		index_output = (index_output + 1) % buffer.length;
		count--;
		return item;
	}
}
