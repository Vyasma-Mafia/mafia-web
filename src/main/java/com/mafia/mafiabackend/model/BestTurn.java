package com.mafia.mafiabackend.model;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class BestTurn {
    private Integer bestTurnFrom;
    private Integer bestTurn1;
    private Integer bestTurn2;
    private Integer bestTurn3;
}
