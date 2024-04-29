package com.example.server.payload.response;

import com.example.server.domain.GameOrder;
import com.example.server.dto.AnswerUserDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class AnswerListResponse {
    List<AnswerUserDto> answerUserDtos;
    public AnswerListResponse (List<GameOrder> gameOrders){
        List<AnswerUserDto> answerUserDtos = new ArrayList<>();
        for(GameOrder gameOrder : gameOrders){
            answerUserDtos.add(new AnswerUserDto(gameOrder.getRoomUser().getRoomNickname(), gameOrder.getAnswerName()));
        }
        this.answerUserDtos = answerUserDtos;
    }
}
