package com.mafia.mafiabackend.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(hidden = true)
@Entity
@Data
@ToString(exclude = "gameInfos")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    @Id
    @SequenceGenerator(name = "playerSec", sequenceName = "PLAYER_SEQUENCE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "playerSec")
    private Long id;

    private String name;

    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH})
    @JsonManagedReference
    private List<GameInfo> gameInfos;

    @Embedded
    private MonitoringInfo monitoringInfo;
}
