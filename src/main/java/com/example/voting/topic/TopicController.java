package com.example.voting.topic;

import com.example.voting.shared.errors.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/topics")
@Tag(name = "Pautas", description = "Cadastro das pautas que serão levadas à assembleia")
class TopicController {

    private final TopicRepository topics;
    private final Clock clock;

    TopicController(TopicRepository topics, Clock clock) {
        this.topics = topics;
        this.clock = clock;
    }

    @PostMapping
    @Operation(summary = "Cadastra uma pauta")
    @ApiResponse(responseCode = "201", description = "Pauta cadastrada; o cabeçalho Location aponta para ela")
    @ApiResponse(responseCode = "400", description = "Título ausente ou em branco", content = @Content)
    ResponseEntity<TopicResponse> register(@Valid @RequestBody CreateTopicRequest request) {
        Topic topic = topics.save(new Topic(request.title(), request.description(), clock.instant()));
        return ResponseEntity.created(URI.create("/api/v1/topics/" + topic.id()))
                .body(TopicResponse.from(topic));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma pauta pelo identificador")
    @ApiResponse(responseCode = "200", description = "Pauta encontrada")
    @ApiResponse(responseCode = "404", description = "Não existe pauta com esse identificador", content = @Content)
    TopicResponse find(@PathVariable Long id) {
        return topics.findById(id)
                .map(TopicResponse::from)
                .orElseThrow(() -> new NotFoundException("Topic %d not found".formatted(id)));
    }
}
