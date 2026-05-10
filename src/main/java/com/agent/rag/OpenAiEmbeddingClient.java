package com.agent.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!mock")
public class OpenAiEmbeddingClient implements EmbeddingClient {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${openai.embedding-model:text-embedding-3-small}")
    private String model;

    @Override
    public double[] embed(String text) {
        throw new UnsupportedOperationException(
                "OpenAI embedding API not yet implemented. " +
                "Model: " + model + ", Endpoint: " + baseUrl);
    }

    @Override
    public List<double[]> embedBatch(List<String> texts) {
        throw new UnsupportedOperationException(
                "OpenAI embedding API not yet implemented. " +
                "Model: " + model + ", Endpoint: " + baseUrl);
    }
}
