package com.agent.rag.advanced;

import com.agent.rag.Chunk;

public class ScoredChunk {

    private final Chunk chunk;
    private final double score;

    public ScoredChunk(Chunk chunk, double score) {
        this.chunk = chunk;
        this.score = score;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public double getScore() {
        return score;
    }

    public String getChunkId() {
        return chunk != null ? chunk.getId() : null;
    }
}
