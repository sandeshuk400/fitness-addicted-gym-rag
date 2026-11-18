package com.fitnessaddicted.gym.ai.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessaddicted.gym.ai.service.AssistantService;
import com.fitnessaddicted.gym.ai.service.KnowledgeService;
import com.fitnessaddicted.gym.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    public record AskRequest(@NotBlank @Size(max = 500) String question) {
    }

    public record AskResponse(String answer, List<String> sources) {
    }

    public record ReindexResponse(int chunks) {
    }

    private final AssistantService assistant;
    private final KnowledgeService knowledge;

    public AssistantController(AssistantService assistant, KnowledgeService knowledge) {
        this.assistant = assistant;
        this.knowledge = knowledge;
    }

    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public AskResponse ask(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AskRequest request) {
        AssistantService.Answer a = assistant.ask(
                CurrentUser.id(jwt), CurrentUser.hasRole(jwt, "MEMBER"), request.question());
        return new AskResponse(a.answer(), a.sources());
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ReindexResponse reindex() {
        return new ReindexResponse(knowledge.reindex());
    }
}