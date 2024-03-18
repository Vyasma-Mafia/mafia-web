package com.mafia.mafiabackend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import com.mafia.mafiabackend.dto.CommonWinsDtoResponse;
import com.mafia.mafiabackend.dto.GameRatingDtoResponse;
import com.mafia.mafiabackend.dto.GameStatisticDtoResponse;
import com.mafia.mafiabackend.dto.SimpleStatisticDto;
import com.mafia.mafiabackend.dto.StatisticsDtoResponse;
import com.mafia.mafiabackend.model.CommonStatistic;
import com.mafia.mafiabackend.model.Game;
import com.mafia.mafiabackend.model.GameInfo;
import com.mafia.mafiabackend.model.Player;
import com.mafia.mafiabackend.model.Role;
import com.mafia.mafiabackend.repository.CommonStatisticsRepository;
import com.mafia.mafiabackend.repository.GameInfoRepository;
import com.mafia.mafiabackend.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {
    private final PlayerRepository playerRepository;
    private final GameInfoRepository gameInfoRepository;
    private final CommonStatisticsRepository commonStatisticsRepository;
    private final String defaultSeason;

    public StatisticsService(
            PlayerRepository playerRepository,
            GameInfoRepository gameInfoRepository,
            CommonStatisticsRepository commonStatisticsRepository,
            @Value("${season.default}") String defaultSeason
    ) {
        this.playerRepository = playerRepository;
        this.gameInfoRepository = gameInfoRepository;
        this.commonStatisticsRepository = commonStatisticsRepository;
        this.defaultSeason = defaultSeason;
    }

    public StatisticsDtoResponse getStatisticsByPlayerId(Long id) {
        Player player = playerRepository.findById(id).orElse(null);

        if (player == null) {
            return null;
        }

        List<GameInfo> gameInfos = player.getGameInfos().stream()
                .filter(gameInfo -> gameInfo.getGame().getGameFinished())
                .collect(Collectors.toList());

        if (gameInfos.isEmpty()) {
            return null;
        }
        List<Game> games = gameInfos.stream()
                .map(GameInfo::getGame)
                .toList();

        List<GameStatisticDtoResponse> gameStatisticDtoRespons = new ArrayList<>();
        games.forEach(game -> gameStatisticDtoRespons.add(GameStatisticDtoResponse.builder()
                .gameFinished(game.getGameFinished())
                .gameType(game.getGameType())
                .id(game.getId())
                .playerWins(isPlayerWon(game, player.getId()))
                .build()));

        return StatisticsDtoResponse.builder()
                .name(player.getName())
                .games(gameStatisticDtoRespons)
                .points(getPointsCount(gameInfos))
                .winRate(getWinRate(gameInfos))
                .averageFouls(getAverageFouls(gameInfos))
                .deathCount(getDeathCount(gameInfos))
                .winsByRed(getWinsByRoleType(true, gameInfos))
                .winsByBlack(getWinsByRoleType(false, gameInfos))
                .gamesByVillager(getTotalGamesPlayedByRole(Role.RED, gameInfos))
                .gamesByBlackRole(getTotalGamesPlayedByRole(Role.BLACK, gameInfos))
                .gamesByDon(getTotalGamesPlayedByRole(Role.DON, gameInfos))
                .gamesBySheriff(getTotalGamesPlayedByRole(Role.SHERIFF, gameInfos))
                .winsByVillager(getWinsByRole(Role.RED, gameInfos))
                .winsByBlackRole(getWinsByRole(Role.BLACK, gameInfos))
                .winsByDon(getWinsByRole(Role.DON, gameInfos))
                .winsBySheriff(getWinsByRole(Role.SHERIFF, gameInfos))
                .totalGames(gameInfos.size())
                .build();
    }

    public void updateCommonStatisticWithPastGames() {
        List<GameInfo> gameInfos = gameInfoRepository.findAll();
        int totalRedWins = (int) gameInfos.stream()
                .map(GameInfo::getGame)
                .distinct()
                .filter(Game::getRedWin)
                .count();

        int totalGames = (int) gameInfos.stream()
                .map(GameInfo::getGame)
                .distinct()
                .count();
        commonStatisticsRepository.save(CommonStatistic.builder()
                .totalGames(totalGames)
                .totalRedWins(totalRedWins)
                .build());
    }

    public void updateCommonStatistics(Role role) {

        // FIXME
        List<CommonStatistic> commonStatisticList = commonStatisticsRepository.findAll();
        CommonStatistic commonStatistic;
        if (!commonStatisticList.isEmpty()) {
            commonStatistic = commonStatisticList.get(0);
        } else {
            commonStatistic = commonStatisticsRepository.save(new CommonStatistic(0L, 0, 0));
        }
        if (!Role.isBlack(role)) {
            commonStatisticsRepository.save(CommonStatistic.builder()
                    .id(commonStatistic.getId())
                    .totalRedWins(commonStatistic.getTotalRedWins() + 1)
                    .totalGames(commonStatistic.getTotalGames() + 1)
                    .build());
        } else {
            commonStatisticsRepository.save(CommonStatistic.builder()
                    .id(commonStatistic.getId())
                    .totalRedWins(commonStatistic.getTotalRedWins())
                    .totalGames(commonStatistic.getTotalGames() + 1)
                    .build());
        }
    }

    public ResponseEntity<CommonWinsDtoResponse> getCommonStatist() {
        List<CommonStatistic> commonStatisticList = commonStatisticsRepository.findAll();
        CommonStatistic commonStatistic;
        if (!commonStatisticList.isEmpty()) {
            commonStatistic = commonStatisticList.get(0);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(CommonWinsDtoResponse.builder()
                .winsByRed(commonStatistic.getTotalRedWins())
                .winsByBlack(commonStatistic.getTotalGames() - commonStatistic.getTotalRedWins())
                .totalGames(commonStatistic.getTotalGames())
                .build(), HttpStatus.OK);
    }


    private Boolean isPlayerWon(Game game, Long playerId) {
        return game.getGameInfos().stream()
                .filter(gameInfo -> gameInfo.getPlayerId().equals(playerId))
                .map(GameInfo::getRole)
                .anyMatch(role -> !Role.isBlack(role).equals(game.getRedWin()));
    }

    private Long getWinsByRoleType(Boolean isRed, List<GameInfo> gameInfos) {
        return isRed ? gameInfos.stream()
                .filter(gameInfo -> !Role.isBlack(gameInfo.getRole()) && gameInfo.getGame().getRedWin())
                .count() : gameInfos.stream()
                .filter(gameInfo -> Role.isBlack(gameInfo.getRole()) && !gameInfo.getGame().getRedWin())
                .count();

    }

    private Long getTotalGamesPlayedByRole(Role role, List<GameInfo> gameInfos) {
        return gameInfos.stream()
                .filter(gameInfo -> gameInfo.getGame().getGameFinished())
                .filter(gameInfo -> gameInfo.getRole().equals(role))
                .count();
    }

    private Long getWinsByRole(Role role, List<GameInfo> gameInfos) {
        return gameInfos.stream()
                .filter(gameInfo -> gameInfo.getGame().getGameFinished())
                .filter(gameInfo -> gameInfo.getRole().equals(role))
                .filter(gameInfo -> (gameInfo.getGame().getRedWin() && !Role.isBlack(role))
                                    || (!gameInfo.getGame().getRedWin() && Role.isBlack(role)))
                .count();
    }

    private Long getWinRate(List<GameInfo> gameInfos) {
        double value =
                (double) (getWinsByRoleType(true, gameInfos) + getWinsByRoleType(false, gameInfos)) / gameInfos.size();

        return Math.round(value * 100);
    }

    private Double getPointsCount(List<GameInfo> gameInfos) {
        return gameInfos.stream()
                .mapToDouble(GameInfo::getPoints)
                .sum();
    }

    private Double getAverageFouls(List<GameInfo> gameInfos) {
        OptionalDouble average = gameInfos.stream()
                .mapToInt(GameInfo::getFoul)
                .average();
        return average.isPresent() ? average.getAsDouble() : 0;
    }

    private Long getDeathCount(List<GameInfo> gameInfos) {
        return gameInfos.stream()
                .filter(gameInfo -> !gameInfo.getAlive())
                .count();
    }

    private static double bestTurnScoreCalculate(Integer it) {
        return switch (it) {
            case 3 -> 1;
            case 2 -> 0.5;
            default -> 0;
        };
    }

    public List<GameRatingDtoResponse> getPlayersRating(Optional<String> season) {
        return playerRepository.findAll().stream()
                .map(player -> {
                    List<GameInfo> gameInfos = player.getGameInfos().stream()
                            .filter(gameInfo -> gameInfo.getGame().getGameFinished())
                            .filter(gameInfo -> Objects.equals(
                                    gameInfo.getGame().getSeason(), season.orElse(defaultSeason)))
                            .collect(Collectors.toList());
                    if (gameInfos.size() < 2) {
                        return null;
                    }
                    long totalWins = getWinsByRoleType(true, gameInfos) + getWinsByRoleType(false, gameInfos);
                    double bestTurnScores = gameInfos.stream()
                            .filter(it -> !it.getRole().isBlack())
                            .map(it -> countBestTurn(it.getGame(), it.getSitNumber()))
                            .mapToDouble(StatisticsService::bestTurnScoreCalculate)
                            .sum();
                    long totalGames = gameInfos.size();
                    return GameRatingDtoResponse.builder()
                            .playerName(player.getName())
                            .totalWins(totalWins)
                            .totalGames(totalGames)
                            .rating((totalWins + bestTurnScores) / totalGames)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(x ->
                                ((GameRatingDtoResponse) x).getRating())
                        .reversed())
                .limit(100)
                .collect(Collectors.toList());
    }

    public List<SimpleStatisticDto> getSimpleStatistic() {
        List<GameInfo> gameInfos = gameInfoRepository.findAllByOrderByMonitoringInfoUpdatedAtDescSitNumberAsc();
        return gameInfos.stream()
                .map(this::buildSimpleStatisticDto)
                .limit(1000)
                .toList();
    }

    public List<SimpleStatisticDto> getSimpleStatisticForGame(long gameId) {
        List<GameInfo> gameInfos = gameInfoRepository.findAllByGameIdOrderBySitNumber(gameId);
        return gameInfos.stream()
                .map(this::buildSimpleStatisticDto)
                .toList();
    }

    private SimpleStatisticDto buildSimpleStatisticDto(GameInfo it) {
        int bestTurn = countBestTurn(it.getGame(), it.getSitNumber());
        return SimpleStatisticDto.builder()
                .gameDate(it.getGame().getMonitoringInfo().getUpdatedAt())
                .gameId(it.getGameId())
                .playerName(it.getPlayer().getName())
                .sitNumber(it.getSitNumber())
                .isRed(!it.getRole().isBlack())
                .isRedWin(it.getGame().getRedWin())
                .bestTurn(bestTurn)
                .role(it.getRole())
                .season(it.getGame().getSeason())
                .firstKilled(checkFirstKilled(it.getGame(), it.getSitNumber()))
                .points(it.getPoints())
                .rating(byGamePlayerRating(it, bestTurn))
                .build();
    }

    private double byGamePlayerRating(GameInfo it, int bestTurn) {
        boolean isWin = it.getRole().isBlack() != it.getGame().getRedWin();
        return it.getPoints() + (isWin ? 2.5d : 0) + bestTurnScoreCalculate(bestTurn) * 2.5d;
    }

    private boolean checkFirstKilled(Game game, Integer sitNumber) {
        return game.getBestTurn() != null && Objects.equals(game.getBestTurn().getBestTurnFrom(), sitNumber);
    }


    private int countBestTurn(Game game, int sitNumber) {
        if (game.getBestTurn() == null || game.getBestTurn().getBestTurnFrom() != sitNumber) {
            return 0;
        }
        var res = 0;
        res += checkOnBlack(game, game.getBestTurn().getBestTurn1());
        res += checkOnBlack(game, game.getBestTurn().getBestTurn2());
        res += checkOnBlack(game, game.getBestTurn().getBestTurn3());
        return res;


    }

    private static Integer checkOnBlack(Game game, Integer sitNumber) {
        if (sitNumber == null) {
            return 0;
        }
        return game.getGameInfos().stream().filter(it -> Objects.equals(it.getSitNumber(), sitNumber))
                .findFirst().map(it -> it.getRole().isBlack() ? 1 : 0).orElse(0);
    }
}
