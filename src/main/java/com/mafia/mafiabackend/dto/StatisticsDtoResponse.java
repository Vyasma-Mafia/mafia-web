package com.mafia.mafiabackend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Сущность, передаваемая в качестве ответа при получении статистики по игроку")
public class StatisticsDtoResponse {
    @Schema(description = "Имя игрока")
    private String name;

    @Schema(description = "Список игр, в которых участвовал игрок")
    private List<GameStatisticDtoResponse> games;

    @Schema(description = "Суммарное кол-во очков игрока")
    private Double points;

    @Schema(description = "Процент побед игрока")
    private Long winRate;

    @Schema(description = "Среднее кол-во фолов игрока")
    private Double averageFouls;

    @Schema(description = "Общее колво смертей игрока")
    private Long deathCount;

    @Schema(description = "Кол-во побед игрока за чёрных")
    private Long winsByBlack;

    @Schema(description = "Кол-во побед игрока за красных")
    private Long winsByRed;

    private Long winsByVillager;

    private Long winsByBlackRole;

    private Long winsByDon;

    private Long winsBySheriff;

    private Long gamesByVillager;

    private Long gamesByBlackRole;

    private Long gamesByDon;

    private Long gamesBySheriff;

    @Schema(description = "Общее кол-во игр, сыгранных игроком")
    private Integer totalGames;
}
