package com.mafia.mafiabackend.dto;

import java.util.List;

import com.mafia.mafiabackend.model.GameType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Schema
@Data
@Builder
@AllArgsConstructor
public class GameDtoResponse {
    private Long id;

    private GameType gameType;

    private Boolean redWin;

    private Boolean gameFinished;

    private Boolean gameStarted;

    private Integer numberOfPlayers;

    private List<Long> gameInfos;

    private String season;
}
