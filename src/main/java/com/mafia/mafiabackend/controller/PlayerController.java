package com.mafia.mafiabackend.controller;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.mafia.mafiabackend.dto.CommonWinsDtoResponse;
import com.mafia.mafiabackend.dto.GameRatingDtoResponse;
import com.mafia.mafiabackend.dto.PlayerDtoRequest;
import com.mafia.mafiabackend.dto.PlayerDtoResponse;
import com.mafia.mafiabackend.dto.StatisticsDtoResponse;
import com.mafia.mafiabackend.model.Season;
import com.mafia.mafiabackend.service.PlayerService;
import com.mafia.mafiabackend.service.StatisticsService;
import com.mafia.mafiabackend.validation.PlayerExists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@Tag(name = "Player Controller",
        description = "Управление игроками, добавление," +
                " получение списка всех игроков, получение игрока по его id, получение статистики игрока")
public class PlayerController {

    private final PlayerService playerService;
    private final StatisticsService statisticsService;

    @Operation(
            summary = "Добавление нового игрока"
    )
    @PostMapping("/player")
    public Long addPlayer(@RequestBody @Valid PlayerDtoRequest playerDtoRequest) {
        return playerService.addPlayer(playerDtoRequest.getName());
    }

    @Operation(
            summary = "Рейтинг по последним 100 игрокам"
    )
    @GetMapping("/player/rating")
    public List<GameRatingDtoResponse> getPlayersRating(
            @RequestParam(value = "season", required = false) Season season
    ) {
        return statisticsService.getPlayersRating(Optional.ofNullable(season));
    }

    @Operation(
            summary = "Соотношение процента побед за красных и чёрных"
    )
    @GetMapping("/player/common-statistics")
    public ResponseEntity<CommonWinsDtoResponse> getWinsCount(){
        return statisticsService.getCommonStatist();
    }

    @Operation(
            summary = "пдейт базы для добавления общей статистики по уже прошедшим играм"
    )
    @PostMapping("/player/statistics/update-with-past-games")
    public void updateWithPastGames(){
        statisticsService.updateCommonStatisticWithPastGames();
    }

    @Operation(
            summary = "Получение списка всех игроков"
    )
    @GetMapping("/player")
    public List<PlayerDtoResponse> getAllPlayers() {
        return playerService.getAllPlayersOrderedByTotalGamesPlayed();
    }


    @Operation(
            summary = "Получение игрока по его id"
    )
    @GetMapping("/player/{id}")
    public PlayerDtoResponse getPlayerById(@PathVariable("id") @NotNull @PlayerExists Long id) {
        return playerService.getPlayerById(id);
    }

    @Operation(
            summary = "Удаляет игрока по его id"
    )
    @DeleteMapping("/player/{id}")
    public void deletePlayerById(@PathVariable("id") @NotNull @PlayerExists Long id) {
        playerService.deletePlayerById(id);
    }

    @Operation(
            summary = "Получает статистику по всем завершённым играм игрока с данным id"
    )
    @GetMapping("/player/{id}/stats")
    public StatisticsDtoResponse getStatisticsByPlayerId(@PathVariable("id") @NotNull @PlayerExists Long id) {
        return statisticsService.getStatisticsByPlayerId(id);
    }
}
