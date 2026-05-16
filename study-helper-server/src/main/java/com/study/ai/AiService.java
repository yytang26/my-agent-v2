package com.study.ai;

public interface AiService {

    String generateSolution(String question, String subject);

    String generateChatReply(String message, String mode);

    String generateExplanation(Long poemId, String style, String targetAge);

    String generateSummary(String content, String style);

    String generateEncouragement();
}
