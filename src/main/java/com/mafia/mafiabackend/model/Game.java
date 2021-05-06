package com.mafia.mafiabackend.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Hidden
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

    private Integer numberOfPlayers;

    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<GameInfo> gameInfos;

}
