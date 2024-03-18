package com.mafia.mafiabackend.dto;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.mafia.mafiabackend.validation.GameExists;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Сущность, передаваемая в качестве запроса при обновлении информации о очках")
@Data
public class GamePointsDtoRequest {
    @Schema(description = "Id игры", example = "305")
    @GameExists
    @NotNull
    private Long id;

    @Schema(description = "Количество очков у каждого игрока", example = "[2.5, 3.3, ...]")
    @NotNull
    private List<Double> points;
}
