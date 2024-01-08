package com.mafia.mafiabackend.dto;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.mafia.mafiabackend.model.GameType;
import com.mafia.mafiabackend.validation.PlayerExists;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Сущность, передаваемая в качестве запроса при создании игры")
@Data
public class GameDtoRequest {
    @Schema(description = "Тип игры CLASSIC или KIEV", example = "CLASSIC")
    @NotNull
    private GameType gameType;

    @Schema(description = "Список Id всех игроков размером от 8 до 18", example = "[100, 101, 102, 103, 104 ...]")
    @NotNull
    @Size(min = 8, max = 18)
    private List<@PlayerExists Long> playersIds;

    @Schema(description = "В каком сезоне прошла игра", example = "AUTUMN_23")
    private String season;
}
