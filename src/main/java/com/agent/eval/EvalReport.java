package com.agent.eval;

import java.util.List;

/**
 * 评估报告
 */
public class EvalReport {

    private int total;
    private int passed;
    private int failed;
    private double successRate;
    private List<EvalResult> details;

    public EvalReport() {
    }

    public EvalReport(int total, int passed, int failed, double successRate, List<EvalResult> details) {
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.successRate = successRate;
        this.details = details;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public List<EvalResult> getDetails() {
        return details;
    }

    public void setDetails(List<EvalResult> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 评估报告 ===\n\n");
        sb.append(String.format("总计: %d | 通过: %d | 失败: %d | 通过率: %.1f%%%n%n",
                total, passed, failed, successRate * 100));

        for (EvalResult result : details) {
            String status = result.isPassed() ? "PASS" : "FAIL";
            sb.append(String.format("[%s] Test #%s - %s%n", status, result.getTestCaseId(),
                    result.isPassed() ? "" : result.getFailureReason()));
            if (!result.isPassed() && result.getActualOutput() != null) {
                sb.append(String.format("  输出: %s%n", result.getActualOutput().length() > 100
                        ? result.getActualOutput().substring(0, 100) + "..." : result.getActualOutput()));
            }
        }

        return sb.toString().trim();
    }
}
