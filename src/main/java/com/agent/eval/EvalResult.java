package com.agent.eval;

import java.util.List;

/**
 * 评估结果
 */
public class EvalResult {

    private String testCaseId;
    private boolean passed;
    private String actualOutput;
    private List<String> toolsCalled;
    private String failureReason;

    public EvalResult() {
    }

    public EvalResult(String testCaseId, boolean passed, String actualOutput,
                      List<String> toolsCalled, String failureReason) {
        this.testCaseId = testCaseId;
        this.passed = passed;
        this.actualOutput = actualOutput;
        this.toolsCalled = toolsCalled;
        this.failureReason = failureReason;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public List<String> getToolsCalled() {
        return toolsCalled;
    }

    public void setToolsCalled(List<String> toolsCalled) {
        this.toolsCalled = toolsCalled;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
