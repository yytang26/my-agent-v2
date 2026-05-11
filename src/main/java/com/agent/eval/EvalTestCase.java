package com.agent.eval;

import java.util.List;

/**
 * 评估测试用例
 */
public class EvalTestCase {

    private String id;
    private String input;
    private String expectedBehavior;
    private List<String> requiredTools;
    private String category;

    public EvalTestCase() {
    }

    public EvalTestCase(String id, String input, String expectedBehavior, List<String> requiredTools, String category) {
        this.id = id;
        this.input = input;
        this.expectedBehavior = expectedBehavior;
        this.requiredTools = requiredTools;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getExpectedBehavior() {
        return expectedBehavior;
    }

    public void setExpectedBehavior(String expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }

    public List<String> getRequiredTools() {
        return requiredTools;
    }

    public void setRequiredTools(List<String> requiredTools) {
        this.requiredTools = requiredTools;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
