package com.mafia.mafiabackend.controller;

import com.mafia.mafiabackend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Controller",
        description = "Управление игроками, добавление," +
                      " получение списка всех игроков, получение игрока по его id, получение статистики игрока")
public class AdminController {
    private final AdminService adminService;

    @Operation(
            summary = "Объединяет статистику двух аккаунтов, удаляет исходящий"
    )
    @PostMapping("/player/{fromId}/{toId}/_merge")
    public HttpStatus mergePlayers(
            @PathVariable long fromId,
            @PathVariable long toId
    ) {
        return adminService.mergePlayers(fromId, toId);
    }
}
