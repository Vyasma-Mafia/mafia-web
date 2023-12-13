package com.mafia.mafiabackend.repository;

import java.util.List;
import java.util.Optional;

import com.mafia.mafiabackend.model.GameInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameInfoRepository extends JpaRepository<GameInfo, Long> {
    Optional<GameInfo> findByPlayerIdAndGameId(Long playerId, Long id);

    List<GameInfo> findAllByGameId(Long id);

    List<GameInfo> findAllByGameIdOrderBySitNumber(Long id);

    List<GameInfo> findAllByOrderByMonitoringInfoUpdatedAtDescSitNumberAsc();

}
