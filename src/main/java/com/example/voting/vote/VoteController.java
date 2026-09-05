package com.example.voting.vote;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Votos", description = "Registro dos votos dos associados e apuração do resultado")
class VoteController {

    private final VoteService votes;

    VoteController(VoteService votes) {
        this.votes = votes;
    }

    @PostMapping("/api/v1/topics/{topicId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registra o voto de um associado",
            description = "Cada associado vota uma única vez por pauta. A elegibilidade é confirmada com o "
                    + "serviço externo antes do registro.")
    @ApiResponse(responseCode = "201", description = "Voto registrado")
    @ApiResponse(responseCode = "400", description = "A escolha não é YES nem NO", content = @Content)
    @ApiResponse(responseCode = "404", description = "Não existe pauta com esse identificador", content = @Content)
    @ApiResponse(responseCode = "409", description = "O associado já votou nessa pauta", content = @Content)
    @ApiResponse(
            responseCode = "422",
            description = "A pauta não tem sessão, a sessão já fechou, ou o associado não pode votar",
            content = @Content)
    @ApiResponse(
            responseCode = "503",
            description = "O serviço de elegibilidade não respondeu; o voto não foi decidido",
            content = @Content)
    VoteResponse cast(@PathVariable long topicId, @Valid @RequestBody CastVoteRequest request) {
        return votes.cast(topicId, request.memberId(), request.choice());
    }

    @GetMapping("/api/v1/topics/{topicId}/result")
    @Operation(
            summary = "Apura o resultado de uma pauta",
            description = "Fica disponível durante a votação, como parcial, e continua depois que a sessão "
                    + "fecha. O campo `votingOpen` diz em qual dos dois casos a resposta está.")
    @ApiResponse(responseCode = "200", description = "Resultado apurado")
    @ApiResponse(responseCode = "404", description = "Não existe pauta com esse identificador", content = @Content)
    @ApiResponse(responseCode = "422", description = "A pauta nunca foi posta em votação", content = @Content)
    VoteResultResponse result(@PathVariable long topicId) {
        return votes.resultFor(topicId);
    }
}
