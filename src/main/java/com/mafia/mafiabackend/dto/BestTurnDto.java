package com.mafia.mafiabackend.dto;

import java.util.List;

import lombok.Data;

@Data
public class BestTurnDto {
    private int from;
    private List<Integer> to;
}
