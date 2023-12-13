package com.mafia.mafiabackend.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.mafia.mafiabackend.dto.SimpleStatisticDto;
import com.mafia.mafiabackend.service.StatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Csv Result Controller",
        description = "Возвращать результаты прошедших игр в csv для учета")
public class CsvResultController {
    private final StatisticsService statisticsService;

    //    gameDate,gameId,playerName,sitNumber,redWin,isRed,bestTurn
    @GetMapping(value = "/results/csv", produces = "text/csv")
    public void leastGamesSimpleResultsAsCsv(HttpServletResponse response) {
        List<SimpleStatisticDto> statistics = statisticsService.getSimpleStatistic();
        try (CSVPrinter printer = new CSVPrinter(response.getWriter(), CSVFormat.EXCEL)) {
            for (var statistic : statistics) {
                printer.printRecord(
                        statistic.getGameDate(),
                        statistic.getGameId(),
                        statistic.getPlayerName(),
                        statistic.getSitNumber(),
                        statistic.getIsRedWin(),
                        statistic.getIsRed(),
                        statistic.getBestTurn()
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
