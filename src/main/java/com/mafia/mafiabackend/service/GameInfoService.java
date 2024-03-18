package com.mafia.mafiabackend.service;

import java.time.Instant;
import java.util.List;

import com.mafia.mafiabackend.dto.GameInfoDtoRequest;
import com.mafia.mafiabackend.dto.GamePointsDtoRequest;
import com.mafia.mafiabackend.model.GameInfo;
import com.mafia.mafiabackend.model.GameResult;
import com.mafia.mafiabackend.repository.GameInfoRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GameInfoService {
    private final GameInfoRepository gameInfoRepository;
    private final GameService gameService;
    private final StatisticsService statisticsService;

    public GameResult updateGameInfo(GameInfoDtoRequest gameInfoDtoRequest) {
        GameInfo gameInfo = gameInfoRepository.findByPlayerIdAndGameId(
                gameInfoDtoRequest.getPlayerId(),
                gameInfoDtoRequest.getId())
                .orElseThrow(RuntimeException::new);
        if (gameInfo.getGame().getGameFinished()) {
            return GameResult.GAME_ALREADY_FINISHED;
        }

        gameInfo.setFoul(gameInfoDtoRequest.getFouls());
        gameInfo.setAlive(gameInfoDtoRequest.getAlive());
        if (gameInfoDtoRequest.getRole() != null) {
            gameInfo.setRole(gameInfoDtoRequest.getRole());
        }
        if (gameInfoDtoRequest.getPoints() != null) {
            gameInfo.setPoints(gameInfoDtoRequest.getPoints());
        }
        gameInfo.getMonitoringInfo().setUpdatedAt(Instant.now());
        gameInfoRepository.save(gameInfo);

//        Game game = gameInfo.getGame();

//        // FIXME
//        boolean gameFinishedByBlack = isGameFinishedByBlack(game);
//        boolean gameFinishedByRed = isGameFinishedByRed(game);
//        if (gameFinishedByBlack) {
//            gameService.finishGame(GameFinishDtoRequest.builder()
//                    .id(game.getId())
//                    .result(GameResult.BLACK_WIN)
//                    .build());
//            statisticsService.updateCommonStatistics(Role.BLACK);
//            return GameResult.BLACK_WIN;
//        }
//        if (gameFinishedByRed) {
//            gameService.finishGame(GameFinishDtoRequest.builder()
//                    .id(game.getId())
//                    .result(GameResult.RED_WIN)
//                    .build());
//            statisticsService.updateCommonStatistics(Role.RED);
//            return GameResult.RED_WIN;
//        }
        return GameResult.GAME_IN_PROGRESS;
    }

    public HttpStatus updateGameInfoPoints(GamePointsDtoRequest request) {
        List<GameInfo> gameInfos = gameInfoRepository.findAllByGameIdOrderBySitNumber(request.getId());
        if (gameInfos.size() != request.getPoints().size()) {
            return HttpStatus.BAD_REQUEST;
        }
        for (int i = 0; i < gameInfos.size(); i++) {
            gameInfos.get(i).setPoints(request.getPoints().get(i));
        }
        gameInfoRepository.saveAll(gameInfos);
        return HttpStatus.OK;
    }
}
