package com.mafia.mafiabackend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.mafia.mafiabackend.model.GameInfo;
import com.mafia.mafiabackend.model.Player;
import com.mafia.mafiabackend.repository.GameInfoRepository;
import com.mafia.mafiabackend.repository.PlayerRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AdminService {
    private final PlayerRepository playerRepository;
    private final GameInfoRepository gameInfoRepository;

    public HttpStatus mergePlayers(long fromId, long toId) {
        Optional<Player> fromPlayerO = playerRepository.findById(fromId);
        Optional<Player> toPlayerO = playerRepository.findById(toId);
        if (fromPlayerO.isEmpty() || toPlayerO.isEmpty()) {
            return HttpStatus.NOT_FOUND;
        }
        Player fromPlayer = fromPlayerO.get();
        Player toPlayer = toPlayerO.get();

        List<GameInfo> fromGameInfos = fromPlayer.getGameInfos();
        List<GameInfo> toGameInfos = toPlayer.getGameInfos();

        if (!Sets.intersection(
                fromGameInfos.stream().map(GameInfo::getGameId).collect(Collectors.toSet()),
                toGameInfos.stream().map(GameInfo::getGameId).collect(Collectors.toSet())
        ).isEmpty()) {
            return HttpStatus.CONFLICT;
        }
        fromGameInfos.forEach(it -> it.setPlayer(toPlayer));
        gameInfoRepository.saveAll(fromGameInfos);
        playerRepository.delete(fromPlayer);
        return HttpStatus.OK;
    }
}
