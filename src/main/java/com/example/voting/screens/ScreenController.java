package com.example.voting.screens;

import com.example.voting.vote.Choice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The screen-driven face of the API, in the message format the brief's annex defines. Each response
 * is a whole screen, and every button carries the address of the next step, so the client walks the
 * flow without knowing any of the rules.
 */
@RestController
@RequestMapping("/api/v1/screens")
@EnableConfigurationProperties(ScreenProperties.class)
@Tag(name = "Telas do aplicativo", description = "Contrato de telas FORMULARIO e SELECAO do anexo 1")
class ScreenController {

    private final ScreenService screens;

    ScreenController(ScreenService screens) {
        this.screens = screens;
    }

    @GetMapping("/topics")
    @Operation(summary = "Tela de seleção com as pautas em votação")
    Screen topics() {
        return screens.topicsOpenForVoting();
    }

    @PostMapping("/topics/{topicId}/identify")
    @Operation(summary = "Tela de formulário pedindo o CPF de quem vai votar")
    Screen identify(@PathVariable long topicId) {
        return screens.askWhoIsVoting(topicId);
    }

    @PostMapping("/topics/{topicId}/choose")
    @Operation(summary = "Tela de seleção com as duas respostas possíveis")
    Screen choose(@PathVariable long topicId, @Valid @RequestBody IdentifyRequest request) {
        return screens.offerTheTwoAnswers(topicId, request.memberId());
    }

    @PostMapping("/topics/{topicId}/votes")
    @Operation(
            summary = "Registra o voto e devolve a tela de confirmação",
            description = "Um voto recusado também volta como tela, com o motivo em texto, porque o "
                    + "aplicativo não sabe renderizar um documento de erro.")
    Screen vote(@PathVariable long topicId, @Valid @RequestBody ScreenVoteRequest request) {
        return screens.castAndConfirm(topicId, request.memberId(), request.choice());
    }

    record IdentifyRequest(@NotBlank String memberId) {}

    record ScreenVoteRequest(@NotBlank String memberId, @NotNull Choice choice) {}
}
