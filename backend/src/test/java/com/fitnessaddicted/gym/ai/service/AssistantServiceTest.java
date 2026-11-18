package com.fitnessaddicted.gym.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.server.ResponseStatusException;

import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.member.domain.ConsentPurpose;
import com.fitnessaddicted.gym.member.service.ConsentService;

/** Unit tests: the vector store and the chat model are mocked, so no Ollama or Docker is needed. */
class AssistantServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final ChatClient.Builder builder = mock(ChatClient.Builder.class);
    private final ConsentService consents = mock(ConsentService.class);
    private AssistantService service;

    @BeforeEach
    void setUp() {
        when(builder.build()).thenReturn(chat);
        service = new AssistantService(vectorStore, builder, consents, 0.5);
    }

    private static Document doc(String title, double score) {
        return Document.builder().text("About " + title).metadata("title", title).score(score).build();
    }

    private void searchReturns(Document... docs) {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(docs));
    }

    private void modelSays(String text) {
        when(chat.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(text);
    }

    @Test
    void memberWithoutAiConsentIsRefusedAndNothingIsSearched() {
        when(consents.isGranted(7L, ConsentPurpose.AI_PROCESSING)).thenReturn(false);

        assertThrows(ApiException.class, () -> service.ask(7L, true, "How long can I freeze?"));

        verifyNoInteractions(vectorStore);
        verifyNoInteractions(chat);
    }

    @Test
    void staffAreNotAskedForMemberConsent() {
        searchReturns(doc("Freezing a membership", 0.9));
        modelSays("Up to 30 days.");

        AssistantService.Answer answer = service.ask(1L, false, "How long can I freeze?");

        assertEquals("Up to 30 days.", answer.answer());
        verifyNoInteractions(consents);
    }

    @Test
    void noMatchingKnowledgeMeansNoModelCallAndAnHonestAnswer() {
        searchReturns();

        AssistantService.Answer answer = service.ask(1L, false, "What is the capital of France?");

        assertEquals(AssistantService.NO_ANSWER, answer.answer());
        assertEquals(List.of(), answer.sources());
        verifyNoInteractions(chat);
    }

    @Test
    void onlyChunksCloseToTheBestMatchAreUsed() {
        searchReturns(doc("A", 0.90), doc("B", 0.85), doc("C", 0.50));
        modelSays("answer");

        AssistantService.Answer answer = service.ask(1L, false, "question");

        assertEquals(List.of("A", "B"), answer.sources());
    }

    @Test
    void atMostThreeChunksAreUsed() {
        searchReturns(doc("A", 0.90), doc("B", 0.89), doc("C", 0.88), doc("D", 0.87));
        modelSays("answer");

        AssistantService.Answer answer = service.ask(1L, false, "question");

        assertEquals(List.of("A", "B", "C"), answer.sources());
    }

    @Test
    void aFailingModelBecomesServiceUnavailable() {
        searchReturns(doc("A", 0.90));
        when(chat.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("model is down"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.ask(1L, false, "question"));

        assertEquals(503, e.getStatusCode().value());
    }
}