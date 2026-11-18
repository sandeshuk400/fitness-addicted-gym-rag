package com.fitnessaddicted.gym.ai.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.common.ApiException;

/**
 * Builds the assistant's knowledge: the live membership plans from the database plus the policy
 * sections in classpath:knowledge/*.md. Each "## " section becomes one searchable chunk.
 */
@Service
public class KnowledgeService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;

    public KnowledgeService(VectorStore vectorStore, JdbcTemplate jdbc) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
    }

    /** Replaces everything in the vector store. One transaction: if embedding fails, the old content stays. */
    @Transactional
    public int reindex() {
        List<Document> chunks = new ArrayList<>();
        Document plans = plansDocument();
        if (plans != null) {
            chunks.add(plans);
        }
        try {
            for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath:knowledge/*.md")) {
                String text = resource.getContentAsString(StandardCharsets.UTF_8);
                chunks.addAll(sections(text, String.valueOf(resource.getFilename())));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (chunks.isEmpty()) {
            throw ApiException.badRequest("No knowledge found to index");
        }
        jdbc.update("delete from vector_store");
        vectorStore.add(chunks);
        return chunks.size();
    }

    /**
     * The wording is deliberate: questions like "what plans do you have and how much are they" should
     * match this chunk strongly, so the header repeats the words people actually use.
     */
    private Document plansDocument() {
        List<String> lines = jdbc.query(
                "select name, description, duration_days, price from membership_plan where active = true order by price",
                (rs, i) -> {
                    String description = rs.getString(2);
                    return "- " + rs.getString(1) + " plan: costs INR "
                            + rs.getBigDecimal(4).stripTrailingZeros().toPlainString()
                            + " for " + rs.getInt(3) + " days"
                            + (description == null || description.isBlank() ? "." : ". " + description.trim());
                });
        if (lines.isEmpty()) {
            return null;
        }
        String text = "Membership plans and prices\n"
                + "These are the membership plans we offer and how much each plan costs (price list):\n"
                + String.join("\n", lines);
        return new Document(text, Map.of("title", "Membership plans and prices", "file", "database"));
    }

    private static List<Document> sections(String markdown, String file) {
        List<Document> out = new ArrayList<>();
        String title = file;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.split("\\R")) {
            if (line.startsWith("## ")) {
                addSection(out, title, body, file);
                title = line.substring(3).trim();
                body.setLength(0);
            } else if (!line.startsWith("# ")) {
                body.append(line).append('\n');
            }
        }
        addSection(out, title, body, file);
        return out;
    }

    private static void addSection(List<Document> out, String title, StringBuilder body, String file) {
        String text = body.toString().strip();
        if (!text.isEmpty()) {
            out.add(new Document(title + "\n" + text, Map.of("title", title, "file", file)));
        }
    }
}