package com.agent.rag;

import java.util.List;

public interface EmbeddingClient {

    double[] embed(String text);

    List<double[]> embedBatch(List<String> texts);
}
