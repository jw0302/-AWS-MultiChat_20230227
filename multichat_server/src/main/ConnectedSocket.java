package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import dto.request.RequestDto;
import dto.response.ResponseDto;
import entity.Room;
import lombok.Getter;

@Getter
public class ConnectedSocket extends Thread {
	
	private static List<ConnectedSocket> connectedSocketList = new ArrayList<>();
	private static List<Room> roomList = new ArrayList<>();											// 방이 생성되면 roomList에 모인다
	private static int index = 0;
	private Socket socket;
	private String username;
	
	private Gson gson;
	
	public ConnectedSocket(Socket socket) {
		this.socket = socket;
		gson = new Gson();
		Room room = new Room("TestRoom" + index, "TestUser" + index);
		index++;
		roomList.add(room);
	}
	
	
	@Override
	public void run() {
		while(true) {
			BufferedReader bufferedReader;
			try {
				bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String requestJson = bufferedReader.readLine();
				
				System.out.println("요청: " + requestJson);
				requestMapping(requestJson);
				
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	
	
	private void requestMapping(String requestJson) {
		RequestDto<?> requestDto = gson.fromJson(requestJson, RequestDto.class);
		
		switch(requestDto.getResource()) {
			case "usernameCheck" :
				checkUsername((String) requestDto.getBody());
				break;
				
			case "createRoom" :
				Room room = new Room((String) requestDto.getBody(), username);
				room.getUsers().add(this);
				roomList.add(room);
				ResponseDto<String> responseDto = new ResponseDto<String>("createRoomSuccessfully", null);
				sendToMe(responseDto);
				refreshUsernameList(username);
				sendToAll(refreshRoomList(), connectedSocketList);
				break;
				
		}
	}
	
	
	private void checkUsername(String username) {
		if(username.isBlank()) {
			sendToMe(new ResponseDto<String>("usernameCheckIsBlank", "사용자 이름은 공백일 수 없습니다."));
			return;
		}
		
		for(ConnectedSocket connectedSocket : connectedSocketList) {
			if(connectedSocket.getUsername().equals(username)) {
				sendToMe(new ResponseDto<String>("usernameCheckIsDuplicate", "이미 사용중인 이름입니다."));
				return;
			}
		}
		
		this.username = username;
		connectedSocketList.add(this);
		sendToMe(new ResponseDto<String>("usernameCheckSuccessfully", null));
		sendToMe(refreshRoomList());
		
	}
	
	
	private ResponseDto<List<Map<String, String>>> refreshRoomList() {
		List<Map<String, String>> roomNameList = new ArrayList<>();
		
		for(Room room : roomList) {
			Map<String, String> roomInfo = new HashMap<>();			
			roomInfo.put("roomName", room.getRoomName());
			roomInfo.put("owner", room.getOwner());
			roomNameList.add(roomInfo);
		}
		
		ResponseDto<List<Map<String, String>>> responseDto = new ResponseDto<List<Map<String, String>>>("refreshRoomList", roomNameList);
		
		return responseDto;
	}
	
	
	private Room findConnectedRoom(String roomName) {
		Room room = null;
		for(Room r : roomList) {			
			for(ConnectedSocket cs : r.getUsers()) {
				if(cs.getUsername().equals(username)) {
					return r;
				}
			}
		}
		return null;
		
	}
	
	
	private Room findRoom(Map<String, String> roomInfo) {
		for(Room room : roomList) {
			if(room.getRoomName().equals(roomInfo.get("roomName")) 
					&& room.getOwner().equals(roomInfo.get("owner"))) {
				return room;
			
			}
		}
		return null;
	
	}
	
	
	private void refreshUsernameList(String username) {
		Room room = findConnectedRoom(username);
		List<String> usernameList = new ArrayList<>();
		usernameList.add("방제목: " + room.getRoomName());							// 첫번째 줄은 방제목이 표시된다.
		for(ConnectedSocket connectedSocket : room.getUsers()) {					// 두번째 줄부터는 방의 user들이 표시되는데 user가 owner이면 뒤에 (방장) 붙여줌
			if(connectedSocket.getUsername().equals(room.getOwner())) {
				usernameList.add(connectedSocket.getUsername() + "(방장)");
				continue;
			}
			usernameList.add(connectedSocket.getUsername());                        // users들 계속 반복 돌다가 (방장)이 아닌 users들은 밑으로 내려와 그냥 이름 표시함
		}
		ResponseDto<List<String>> responseDto = new ResponseDto<List<String>>("refreshUsernameList", usernameList);
		sendToAll(responseDto, room.getUsers());
		
	}
	
	
	private void sendToMe(ResponseDto<?> responseDto) {
		try {
			OutputStream outputStream = socket.getOutputStream();
			PrintWriter printWriter = new PrintWriter(outputStream, true);
			
			String responseJson = gson.toJson(responseDto);
			printWriter.println(responseJson);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void sendToAll(ResponseDto<?> responseDto, List<ConnectedSocket> connectedSockets) {
		
		for(ConnectedSocket connectedSocket : connectedSockets) {
			try {
				OutputStream outputStream = connectedSocket.getSocket().getOutputStream();
				PrintWriter printWriter = new PrintWriter(outputStream, true);
			
				String responseJson = gson.toJson(responseDto);
				printWriter.println(responseJson);
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
