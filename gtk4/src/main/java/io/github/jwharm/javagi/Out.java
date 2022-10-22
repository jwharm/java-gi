package io.github.jwharm.javagi;

public class Out<T> {
	
	private T value;
	
	public Out() {
	}
	
	public Out(T value) {
		this.value = value;
	}
	
	public T get() {
		return value;
	}
	
	public void set(T value) {
		this.value = value;
	}
}
