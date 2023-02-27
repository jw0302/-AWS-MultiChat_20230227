package main;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectedSocket extends Thread {
	
	private static List<ConnectedSocket> connectedSocketList = new ArrayList<>();
	private Socket socket;
	
	public ConnectedSocket(Socket socket) {
		this.socket = socket;
		connectedSocketList.add(this);
	}
	
	
	@Override
	public void run() {
		System.out.println("클라이언트가 연결!!");
	}

}
