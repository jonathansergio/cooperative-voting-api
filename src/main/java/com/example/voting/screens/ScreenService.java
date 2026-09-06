package com.example.voting.screens;

import com.example.voting.screens.Screen.Button;
import com.example.voting.screens.Screen.FormItem;
import com.example.voting.screens.Screen.SelectionItem;
import com.example.voting.session.VotingSessionService;
import com.example.voting.shared.errors.ConflictException;
import com.example.voting.shared.errors.NotFoundException;
import com.example.voting.shared.errors.UnprocessableException;
import com.example.voting.shared.errors.UpstreamUnavailableException;
import com.example.voting.topic.TopicRepository;
import com.example.voting.topic.TopicSummary;
import com.example.voting.vote.Choice;
import com.example.voting.vote.VoteService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Builds the screens the mobile client renders. This is presentation only: every rule it depends on
 * lives in the voting domain, and this class just chooses what to show. Having a second face on the
 * same services, next to the REST API, is what the layering was for.
 */
@Service
class ScreenService {

    private final TopicRepository topics;
    private final VotingSessionService sessions;
    private final VoteService votes;
    private final ScreenProperties properties;

    ScreenService(
            TopicRepository topics, VotingSessionService sessions, VoteService votes, ScreenProperties properties) {
        this.topics = topics;
        this.sessions = sessions;
        this.votes = votes;
        this.properties = properties;
    }

    Screen topicsOpenForVoting() {
        List<Long> openTopicIds = sessions.topicsAcceptingVotes();
        List<SelectionItem> items = topics.summariesOf(openTopicIds).stream()
                .map(topic -> SelectionItem.to(topic.title(), url("/topics/%d/identify".formatted(topic.id()))))
                .toList();
        return Screen.selection("Pautas em votação", items);
    }

    Screen askWhoIsVoting(long topicId) {
        TopicSummary topic = summaryOf(topicId);
        return Screen.form(
                topic.title(),
                List.of(
                        FormItem.text(
                                topic.description() == null ? "Informe seu CPF para votar." : topic.description()),
                        FormItem.textInput("memberId", "CPF do associado", "")),
                Button.to("Continuar", url("/topics/%d/choose".formatted(topicId))),
                Button.to("Cancelar", url("/topics")));
    }

    Screen offerTheTwoAnswers(long topicId, String memberId) {
        TopicSummary topic = summaryOf(topicId);
        String votesUrl = url("/topics/%d/votes".formatted(topicId));
        return Screen.selection(
                topic.title(),
                List.of(
                        new SelectionItem("Sim", votesUrl, Map.of("memberId", memberId, "choice", Choice.YES.name())),
                        new SelectionItem("Não", votesUrl, Map.of("memberId", memberId, "choice", Choice.NO.name()))));
    }

    /**
     * A refused vote is not an error for this client: it has no way to render a problem document, so
     * the reason comes back as the text of a screen. The REST API keeps answering 409 and 422.
     */
    Screen castAndConfirm(long topicId, String memberId, Choice choice) {
        TopicSummary topic = summaryOf(topicId);
        return Screen.form(
                topic.title(),
                List.of(FormItem.text(outcomeOf(topicId, memberId, choice))),
                Button.to("Voltar às pautas", url("/topics")),
                null);
    }

    private String outcomeOf(long topicId, String memberId, Choice choice) {
        // Only the refusals the domain declares are turned into text. Anything else is a defect and
        // must keep travelling as a 500 instead of being shown to the member as if it were expected.
        try {
            votes.cast(topicId, memberId, choice);
            return "Seu voto foi registrado.";
        } catch (ConflictException alreadyVoted) {
            return "Você já votou nesta pauta.";
        } catch (UnprocessableException notAllowed) {
            return "Esta pauta não está aceitando votos, ou você não está habilitado a votar.";
        } catch (UpstreamUnavailableException unreachable) {
            return "Não foi possível confirmar sua habilitação agora. Tente novamente em instantes.";
        }
    }

    private TopicSummary summaryOf(long topicId) {
        return topics.summaryOf(topicId)
                .orElseThrow(() -> new NotFoundException("Topic %d not found".formatted(topicId)));
    }

    private String url(String path) {
        return properties.url("/api/v1/screens" + path);
    }
}
