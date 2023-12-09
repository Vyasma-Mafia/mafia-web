package com.mafia.mafiabackend.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SimpleStatisticDto {
    private Instant gameDate;
    private long gameId;
    private String playerName;
    private Boolean isRed;
    private Boolean isRedWin;
    private int bestTurn;
}
