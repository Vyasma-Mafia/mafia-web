package com.mafia.mafiabackend.model;

import java.util.List;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(hidden = true)
@Entity
@Data
@ToString(exclude = {"gameInfos"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    @Id
    @SequenceGenerator(name = "gameSec", sequenceName = "GAME_SEQUENCE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gameSec")
    private Long id;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    private Boolean redWin;

    private Boolean gameFinished;

    private Boolean gameStarted;

    private Integer numberOfPlayers;

    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<GameInfo> gameInfos;

    @Embedded
    private BestTurn bestTurn;

    @Embedded
    private MonitoringInfo monitoringInfo;

    private Season season;
}
