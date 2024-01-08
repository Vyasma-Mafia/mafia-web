package com.mafia.mafiabackend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mafia.mafiabackend.dto.ActiveGamesDtoResponse;
import com.mafia.mafiabackend.dto.GameDtoRequest;
import com.mafia.mafiabackend.dto.GameFinishDtoRequest;
import com.mafia.mafiabackend.dto.GameInfoDto;
import com.mafia.mafiabackend.dto.GameInfoDtoResponse;
import com.mafia.mafiabackend.dto.NonActiveGameDtoResponse;
import com.mafia.mafiabackend.model.BestTurn;
import com.mafia.mafiabackend.model.Game;
import com.mafia.mafiabackend.model.GameInfo;
import com.mafia.mafiabackend.model.GameResult;
import com.mafia.mafiabackend.model.GameType;
import com.mafia.mafiabackend.model.MonitoringInfo;
import com.mafia.mafiabackend.model.Player;
import com.mafia.mafiabackend.model.Role;
import com.mafia.mafiabackend.repository.GameInfoRepository;
import com.mafia.mafiabackend.repository.GameRepository;
import com.mafia.mafiabackend.repository.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final GameInfoRepository gameInfoRepository;
    private final PlayerRepository playerRepository;
    private final ConversionService conversionService;
    private final StatisticsService statisticsService;
    private final GoogleSheetsService googleSheetsService;
    private final String defaultSeason;

    public GameService(
            GameRepository gameRepository,
            GameInfoRepository gameInfoRepository,
            PlayerRepository playerRepository,
            ConversionService conversionService,
            StatisticsService statisticsService,
            GoogleSheetsService googleSheetsService,
            @Value("${season.default}") String defaultSeason
    ) {
        this.gameRepository = gameRepository;
        this.gameInfoRepository = gameInfoRepository;
        this.playerRepository = playerRepository;
        this.conversionService = conversionService;
        this.statisticsService = statisticsService;
        this.googleSheetsService = googleSheetsService;
        this.defaultSeason = defaultSeason;
    }

    public GameInfoDtoResponse getGameInfosByGameId(Long id) {
        List<GameInfo> gameInfos = gameInfoRepository.findAllByGameId(id).stream()
                .sorted(Comparator.comparing(GameInfo::getAlive).reversed().thenComparing(GameInfo::getSitNumber))
                .toList();
        if (gameInfos.isEmpty()) {
            return null;
        }

        List<GameInfoDto> gameInfoDtos = new ArrayList<>();

        for (GameInfo gameInfo : gameInfos) {
            gameInfoDtos.add(conversionService.convert(gameInfo, GameInfoDto.class));
        }
        Optional<Game> optionalGame = gameRepository.findById(id);
        boolean gameStarted;
        if (optionalGame.isPresent()) {
            Game game = optionalGame.get();
            gameStarted = game.getGameStarted();
            if (game.getGameFinished()) {
                return GameInfoDtoResponse.builder()
                        .redWin(game.getRedWin())
                        .playerInfos(gameInfoDtos)
                        .gameFinished(true)
                        .gameId(id)
                        .gameStarted(gameStarted)
                        .build();
            } else {
                return GameInfoDtoResponse.builder()
                        .redWin(null)
                        .playerInfos(gameInfoDtos)
                        .gameFinished(false)
                        .gameId(id)
                        .canBlackWin(isGameCanFinishedByBlack(game))
                        .canRedWin(isGameCanFinishedByRed(game))
                        .gameStarted(gameStarted)
                        .build();
            }
        }
        return null;
    }

    private boolean isGameCanFinishedByRed(Game game) {
        if (game.getGameInfos().stream().anyMatch(x -> x.getRole() == null)) {
            return true;
        }
        return game.getGameInfos().stream().noneMatch(x -> Role.isBlack(x.getRole()) && x.getAlive());
    }

    private boolean isGameCanFinishedByBlack(Game game) {
        List<GameInfo> gameInfos = game.getGameInfos();
        if (gameInfos.stream().anyMatch(x -> x.getRole() == null)) {
            return true;
        }
        long numberOfAliveBlackPlayers = gameInfos.stream()
                .filter(x -> Role.isBlack(x.getRole()) && x.getAlive())
                .count();
        long numberOfAliveRedPlayers = gameInfos.stream()
                .filter(x -> !Role.isBlack(x.getRole()) && x.getAlive())
                .count();
        return numberOfAliveBlackPlayers >= numberOfAliveRedPlayers;
    }

    private static List<String> getGamePlayerNames(Game game) {
        return game.getGameInfos().stream()
                .sorted(Comparator.comparingInt(GameInfo::getSitNumber))
                .map(GameInfo::getPlayer)
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public List<ActiveGamesDtoResponse> getActiveGames() {
        return gameRepository.findAllByGameFinishedFalse().stream()
                .map(game -> ActiveGamesDtoResponse.builder()
                        .gameId(game.getId())
                        .playerNames(getGamePlayerNames(game))
                        .build())
                .collect(Collectors.toList());
    }

    public List<NonActiveGameDtoResponse> getLastTenNonActiveGames() {
        return gameRepository.findAllByGameFinishedTrue().stream()
                .sorted(Comparator.comparing(x -> ((Game) x).getMonitoringInfo().getUpdatedAt()).reversed())
                .limit(10)
                .map(game -> NonActiveGameDtoResponse.builder()
                        .gameId(game.getId())
                        .playerNames(getGamePlayerNames(game))
                        .winByRed(game.getRedWin())
                        .build())
                .collect(Collectors.toList());
    }

    public HttpStatus finishGame(GameFinishDtoRequest gameFinishDtoRequest) {
        Optional<Game> optionalGame = gameRepository.findById(gameFinishDtoRequest.getId());

        if (optionalGame.isEmpty()) {
            return HttpStatus.NOT_FOUND;
        }
        Game game = optionalGame.get();
        if (game.getGameFinished()) {
            return HttpStatus.ALREADY_REPORTED;
        }

        if (gameFinishDtoRequest.getResult() == GameResult.SKIP_AND_DELETE) {
            gameInfoRepository.deleteAll(game.getGameInfos());
            gameRepository.deleteById(gameFinishDtoRequest.getId());
            log.info("Game with id: " + gameFinishDtoRequest.getId() + " has been deleted from database");
            return HttpStatus.OK;
        }

        game.setRedWin(gameFinishDtoRequest.getResult() == GameResult.RED_WIN);
        game.setGameFinished(true);
        game.getMonitoringInfo().setUpdatedAt(Instant.now());
        var bestTurnDto = gameFinishDtoRequest.getBestTurn();
        if (bestTurnDto != null) {
            var bestTurnBuilder = BestTurn.builder()
                    .bestTurnFrom(bestTurnDto.getFrom());
            if (!bestTurnDto.getTo().isEmpty()) {
                bestTurnBuilder.bestTurn1(bestTurnDto.getTo().get(0));
            }
            if (bestTurnDto.getTo().size() >= 2) {
                bestTurnBuilder.bestTurn2(bestTurnDto.getTo().get(1));
            }
            if (bestTurnDto.getTo().size() >= 3) {
                bestTurnBuilder.bestTurn3(bestTurnDto.getTo().get(2));
            }
            game.setBestTurn(bestTurnBuilder.build());

        }
        gameRepository.save(game);
        statisticsService.updateCommonStatistics(game.getRedWin() ? Role.RED : Role.BLACK);
        googleSheetsService.sendResultStatToGoogleSheet(statisticsService.getSimpleStatisticForGame(game.getId()));
        log.info("Game with id: " + gameFinishDtoRequest.getId() + " has been finished");
        return HttpStatus.OK;
    }

    public Long createGame(GameDtoRequest gameDtoRequest) {
        Game game = Game.builder()
                .gameType(gameDtoRequest.getGameType())
                .numberOfPlayers(gameDtoRequest.getPlayersIds().size())
                .gameFinished(false)
                .gameStarted(false)
                .season(gameDtoRequest.getSeason() == null ? defaultSeason : gameDtoRequest.getSeason())
                .monitoringInfo(MonitoringInfo.builder()
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .build();

        gameRepository.save(game);

        Map<Long, Role> playerIdToRole = randomizeRoles(
                gameDtoRequest.getGameType(),
                new ArrayList<>(gameDtoRequest.getPlayersIds())
        );


        List<GameInfo> gameInfos = new ArrayList<>();
        List<Long> playersIds = gameDtoRequest.getPlayersIds();
        Map<Long, Player> idToPlayer = playerRepository.findAllById(playersIds).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        for (int i = 0; i < playersIds.size(); i++) {
            Long id = playersIds.get(i);
            Player player = idToPlayer.get(id);
            GameInfo gameInfo = GameInfo.builder()
                    .game(game)
                    .alive(true)
                    .foul(0)
                    .points(0)
                    .sitNumber(i + 1)
                    .role(playerIdToRole.get(id))
                    .player(player)
                    .monitoringInfo(MonitoringInfo.builder()
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build())
                    .build();
            gameInfos.add(gameInfo);
        }


        gameInfoRepository.saveAll(gameInfos);
        log.info("Game with id: " + game.getId() + "and game type: " + game.getGameType() + " has been created");
        return game.getId();
    }


    public GameInfoDtoResponse reshuffleRoles(Long id, GameType gameType) {
        List<Long> playerIds = new ArrayList<>();
        List<GameInfo> gameInfos = gameInfoRepository.findAllByGameId(id).stream()
                .peek(gameInfo -> playerIds.add(gameInfo.getPlayerId()))
                .collect(Collectors.toList());

        Map<Long, Role> playerIdToRole = randomizeRoles(gameType, playerIds);

        gameInfos.forEach(gameInfo -> {
            Long playerId = gameInfo.getPlayerId();
            Role role = playerIdToRole.get(playerId);
            gameInfo.setRole(role);
            gameInfo.getMonitoringInfo().setUpdatedAt(Instant.now());
        });

        return GameInfoDtoResponse.builder()
                .playerInfos(gameInfoSaveAndCreateDto(gameInfos))
                .gameFinished(false)
                .gameId(id)
                .gameStarted(false)
                .build();
    }

    private List<GameInfoDto> gameInfoSaveAndCreateDto(List<GameInfo> gameInfos) {
        return gameInfoRepository.saveAll(gameInfos).stream()
                .sorted(Comparator.comparing(GameInfo::getSitNumber))
                .map(gameInfo -> conversionService.convert(gameInfo, GameInfoDto.class))
                .collect(Collectors.toList());
    }

    private Map<Long, Role> randomizeRoles(GameType gameType, List<Long> playersIds) {
        Map<Long, Role> playerIdToRole = new HashMap<>();
        int numberOfPlayers = playersIds.size();
        int counter = 0;
        if (gameType == GameType.KIEV) {
            Long idToRemove = playersIds.get((int) (Math.random() * playersIds.size()));
            playerIdToRole.put(idToRemove, Role.WHORE);
            counter++;
            playersIds.remove(idToRemove);
            idToRemove = playersIds.get((int) (Math.random() * playersIds.size()));
            playerIdToRole.put(idToRemove, Role.DOCTOR);
            playersIds.remove(idToRemove);
            idToRemove = playersIds.get((int) (Math.random() * playersIds.size()));
            playerIdToRole.put(idToRemove, Role.MANIAC);
            playersIds.remove(idToRemove);
        }
        Long idToRemove = playersIds.get((int) (Math.random() * playersIds.size()));
        playerIdToRole.put(idToRemove, Role.SHERIFF);
        playersIds.remove(idToRemove);
        idToRemove = playersIds.get((int) (Math.random() * playersIds.size()));
        playerIdToRole.put(idToRemove, Role.DON);
        playersIds.remove(idToRemove);
        counter++;
        while (counter < numberOfPlayers / 3) {
            idToRemove = playersIds.get((int) (Math.random() * playersIds.size()));
            playerIdToRole.put(idToRemove, Role.BLACK);
            playersIds.remove(idToRemove);
            counter++;
        }
        for (Long id : playersIds) {
            playerIdToRole.put(id, Role.RED);
        }
        return playerIdToRole;
    }

    public HttpStatus startGame(Long gameId) {
        Optional<Game> optionalGame = gameRepository.findById(gameId);
        if (optionalGame.isPresent()) {
            Game game = optionalGame.get();
            game.setGameStarted(true);
            if (!validateRoles(game)) {
                return HttpStatus.BAD_REQUEST;
            }
            gameRepository.save(game);
            return HttpStatus.OK;
        }
        return HttpStatus.NOT_FOUND;
    }

    public boolean validateRoles(Game game) {
        if (game.getGameType() == GameType.CLASSIC && game.getGameInfos().size() == 10) {
            return checkRoleInGame(game, Role.BLACK, 2)
                   && checkRoleInGame(game, Role.DON, 1)
                   && checkRoleInGame(game, Role.SHERIFF, 1)
                   && checkRoleInGame(game, Role.RED, 6);
        }
        return true;
    }

    private static boolean checkRoleInGame(Game game, Role role, int expectedCount) {
        return game.getGameInfos().stream().filter(it -> it.getRole() == role).count() == expectedCount;
    }
}
