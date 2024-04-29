package com.example.server.service;

import com.example.server.domain.*;
import com.example.server.dto.*;
import com.example.server.payload.response.AnswerListResponse;
import com.example.server.payload.response.ResultResponse;
import com.example.server.repository.AnswerListRepository;
import com.example.server.repository.GameOrderRepository;
import com.example.server.repository.RoomRepository;
import com.example.server.repository.RoomUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final GameOrderRepository gameOrderRepository;
    private final AnswerListRepository answerListRepository;
    @Autowired
    public ChatService(RoomRepository roomRepository,
                       RoomUserRepository roomUserRepository, GameOrderRepository gameOrderRepository,
                       AnswerListRepository answerListRepository){
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.gameOrderRepository = gameOrderRepository;
        this.answerListRepository = answerListRepository;
    }

    public ChatGameMessage setGame(ChatMessage chatMessage){
        if(chatMessage.getMessageType()==ChatMessage.MessageType.START){
            return startGame(chatMessage);
        }
//        else if (chatMessage.getMessageType()==ChatMessage.MessageType.END) {
//            return endGame(chatMessage);
//        }
        else if (chatMessage.getMessageType()==ChatMessage.MessageType.CORRECT) {
            return correctAnswer(chatMessage);
        }
        else{
            return playGame(chatMessage);
        }

    }

    public ChatGameMessage startGame(ChatMessage chatMessage) {
        makeGameOrder(chatMessage.getRoomId());
        Optional<Room> optionalRoom = roomRepository.findById(chatMessage.getRoomId());
        Room room = optionalRoom.get();
        room.setGameStatus(true);
        room.setCycle(1);
        room.setCurrentOrder(1);
        room.setCorrectMemberCnt(0);
        roomRepository.save(room);

        ChatGameMessage chatGameMessage = makeChatGameMessage(chatMessage, room);
        chatGameMessage.setMessageType(ChatMessage.MessageType.START);

        return chatGameMessage;
    }

    public ResultResponse endGame(Long roomId){
        Room room = roomRepository.findById(roomId).get();
        List<RoomUser> roomUsers = roomUserRepository.findByRoomId(roomId);
        room.setGameStatus(false);
        gameOrderRepository.deleteGameOrderByRoomId(roomId);

        return new ResultResponse(room, roomUsers);
    }


    public ChatGameMessage playGame(ChatMessage chatMessage) {
        ChatGameMessage chatGameMessage = new ChatGameMessage();
        RoomUser sendRoomUser = roomUserRepository.hasNickName(chatMessage.getSender()).get();
        GameOrder gameOrder = gameOrderRepository.findGameOrderByUserId(sendRoomUser.getId()).get();
        Room room = roomRepository.findById(chatMessage.getRoomId()).get();

        if(chatMessage.getMessageType()==ChatMessage.MessageType.ASK){  // 질문일 경우 다음 턴으로 넘어감.
            int currentOrder = sendRoomUser.getGameOrder().getUserOrder();
            int nextOrder = (currentOrder + 1) / room.getUserCount();
            gameOrder.setNowTurn(true);
            gameOrder.setNextTurn(false);
            GameOrder nextGameOrder = gameOrderRepository.findByUserOrder(nextOrder).get();
            nextGameOrder.setNowTurn(false);
            nextGameOrder.setNextTurn(true);
            room.setCurrentOrder(nextOrder);
            gameOrderRepository.save(gameOrder);
            gameOrderRepository.save(nextGameOrder);
            chatGameMessage = makeChatGameMessage(chatMessage, room);

            if(sendRoomUser.getGameOrder().getUserOrder() == room.getUserCount()){ // 질문자가 마지막 사람이면 사이클 추가
                room.setCycle(room.getCycle()+1);
            }
            roomRepository.save(room);  // 게임 메시지를 만든 후 저장한다.   TODO 사이클 추가 부분 생각해봐야할 듯!
        }
        else{
            chatGameMessage = makeChatGameMessage(chatMessage, room);
        }

        return chatGameMessage;
    }

    public ChatGameMessage correctAnswer(ChatMessage chatMessage) {   // 정답 맞추기
        ChatGameMessage chatGameMessage = new ChatGameMessage();
        RoomUser roomUser = roomUserRepository.hasNickName(chatMessage.getSender()).get();
        GameOrder gameOrder = gameOrderRepository.findGameOrderByUserId(roomUser.getId()).get();
        Room room = roomRepository.findById(chatMessage.getRoomId()).get();

        if(gameOrder.getAnswerName().equals(chatMessage.getContent())){ // 정답
            room.setCorrectMemberCnt(room.getCorrectMemberCnt()+1);
            gameOrder.setRanking(room.getCorrectMemberCnt());
            gameOrder.setHaveAnswerChance(false);
        }
        else{ // 오답
            gameOrder.setHaveAnswerChance(false); // 정답기회 없애기
            gameOrderRepository.save(gameOrder);
        }

        makeChatGameMessage(chatMessage, room);

        if(room.getCorrectMemberCnt() >= 3 || room.getUserCount()-1 <= room.getCorrectMemberCnt()){ // 게임 끝나는 경우
            chatGameMessage.setMessageType(ChatMessage.MessageType.END);
        }
        else{
            chatGameMessage.setMessageType(ChatMessage.MessageType.CORRECT);
        }
        return chatGameMessage;
    }




    public void makeGameOrder(Long roomId){  // 게임 순서 & 정답어 설정
        List<RoomUser> roomUsers = roomUserRepository.findRandomByRoomId(roomId);
        List<AnswerList> answerLists = answerListRepository.findAnswerListBy();
        Optional<Room> room = roomRepository.findById(roomId);
        int order = 1;
        for(RoomUser roomUser : roomUsers){
            GameOrder gameOrder = new GameOrder();
            gameOrder.setRoom(room.get());
            gameOrder.setRoomUser(roomUser);
            if(order == 1){
                gameOrder.setNowTurn(false);
                gameOrder.setNextTurn(true);
            }
            else{
                gameOrder.setNowTurn(false);
                gameOrder.setNextTurn(false);
            }
            gameOrder.setRanking(0);
            gameOrder.setPenalty(0);
            gameOrder.setHaveAnswerChance(true);
            gameOrder.setAnswerName(answerLists.get(order-1).getName());
            gameOrder.setUserOrder(order++);

            gameOrderRepository.save(gameOrder);
        }
    }

    public ChatGameMessage makeChatGameMessage(ChatMessage chatMessage, Room room){
        ChatGameMessage chatGameMessage = new ChatGameMessage();
        chatGameMessage.setContent(chatMessage.getContent());
        chatGameMessage.setSender(chatMessage.getSender());
        chatGameMessage.setRoomId(chatMessage.getRoomId());
        chatGameMessage.setGameEnd(room.isGameStatus());
        chatGameMessage.setCycle(room.getCycle());
        chatGameMessage.setGameUserDtos(makeGameUserDtos(chatMessage.getRoomId()));
        return chatGameMessage;
    }

    public List<GameUserDto> makeGameUserDtos(Long roomId){ // GameUserDtos 생성 메소드
        List<RoomUser> roomUsers = roomUserRepository.findByRoomId(roomId);
        List<GameUserDto> gameUserDtos = new ArrayList<>();
        for(RoomUser roomUser : roomUsers){
            Optional<GameOrder> gameOrderOptional = gameOrderRepository.findGameOrderByUserId(roomUser.getId());
            GameUserDto gameUserDto = GameUserDto.of(gameOrderOptional.get(), roomUser);
            gameUserDtos.add(gameUserDto);
        }
        return gameUserDtos;
    }

    public ChatRoomModeMessage changeRoomMode(ChatMessage chatMessage){
        Room room = roomRepository.findById(chatMessage.getRoomId()).get();

        if(room.isPrivateRoom()){
            room.setPrivateRoom(false);
        }
        else{
            room.setPrivateRoom(true);
        }

        roomRepository.save(room);
        return new ChatRoomModeMessage(chatMessage,room);
    }


    public AnswerListResponse getAnswerList(Long roomId, String nickname){
        return new AnswerListResponse(gameOrderRepository.findAnswerNotMe(nickname, roomId));
    }


}
