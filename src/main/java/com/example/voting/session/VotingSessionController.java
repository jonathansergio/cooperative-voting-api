package com.example.voting.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Sessões de votação", description = "Abertura e consulta da sessão de votação de uma pauta")
class VotingSessionController {

    private final VotingSessionService sessions;

    VotingSessionController(VotingSessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping("/api/v1/topics/{topicId}/sessions")
    @Operation(
            summary = "Abre a sessão de votação de uma pauta",
            description = "A duração vem em `durationMinutes`. Sem esse campo, a sessão fica aberta por um minuto.")
    @ApiResponse(responseCode = "201", description = "Sessão aberta")
    @ApiResponse(responseCode = "404", description = "Não existe pauta com esse identificador", content = @Content)
    @ApiResponse(responseCode = "409", description = "A pauta já tem uma sessão de votação", content = @Content)
    ResponseEntity<VotingSessionResponse> open(
            @PathVariable Long topicId, @Valid @RequestBody OpenSessionRequest request) {
        VotingSessionResponse session = sessions.open(topicId, request.durationMinutes());
        return ResponseEntity.created(URI.create("/api/v1/sessions/" + session.id()))
                .body(session);
    }

    @GetMapping("/api/v1/sessions/{id}")
    @Operation(
            summary = "Busca uma sessão de votação",
            description =
                    "O campo `open` é calculado no momento da consulta, comparando a hora atual com o fechamento.")
    @ApiResponse(responseCode = "200", description = "Sessão encontrada")
    @ApiResponse(responseCode = "404", description = "Não existe sessão com esse identificador", content = @Content)
    VotingSessionResponse find(@PathVariable Long id) {
        return sessions.find(id);
    }
}
