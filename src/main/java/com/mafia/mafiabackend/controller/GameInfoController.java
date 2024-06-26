package com.mafia.mafiabackend.controller;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.mafia.mafiabackend.dto.GameInfoDtoRequest;
import com.mafia.mafiabackend.dto.GameInfoDtoResponse;
import com.mafia.mafiabackend.dto.GamePointsDtoRequest;
import com.mafia.mafiabackend.model.GameResult;
import com.mafia.mafiabackend.service.GameInfoService;
import com.mafia.mafiabackend.service.GameService;
import com.mafia.mafiabackend.validation.GameExists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "GameInfo Controller", description = "Управление записями о состоянии игроков в игре," +
        " получение всех состояний игроков для данной игры," +
        " обновление информации о состоянии игроков")
public class GameInfoController {

    private final GameInfoService gameInfoService;
    private final GameService gameService;

    @Operation(
            summary = "Обновление информации о фолах, очках и смерти игрока в базе"
    )
    @PatchMapping("/gameInfo")
    public GameResult changeGameInfo(@RequestBody @Valid GameInfoDtoRequest gameInfoDtoRequest) {
        return gameInfoService.updateGameInfo(gameInfoDtoRequest);
    }

    @Operation(
            summary = "Обновление информации о фолах, очках и смерти игрока в базе"
    )
    @PatchMapping("/gameInfo/points")
    public HttpStatus changeGameInfo(@RequestBody @Valid GamePointsDtoRequest gameInfoDtoRequest) {
        return gameInfoService.updateGameInfoPoints(gameInfoDtoRequest);
    }



    @Operation(
            summary = "По id игры получает список всех gameInfos (состояний игроков сейчас)"
    )
    @GetMapping("/gameInfo/{id}")
    public GameInfoDtoResponse getGameInfos(@PathVariable("id") @NotNull @GameExists Long id) {
        return gameService.getGameInfosByGameId(id);
    }
}
