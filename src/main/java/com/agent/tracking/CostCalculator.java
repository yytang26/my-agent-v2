package com.agent.tracking;

import org.springframework.stereotype.Component;

@Component
public class CostCalculator {

    private final PricingRegistry pricingRegistry;

    public CostCalculator(PricingRegistry pricingRegistry) {
        this.pricingRegistry = pricingRegistry;
    }

    public double calculateCost(String model, int inputTokens, int outputTokens) {
        PricingRegistry.ModelPricing pricing = pricingRegistry.getPricing(model);
        double inputCost = (inputTokens * pricing.getInputPricePerMillion()) / 1_000_000.0;
        double outputCost = (outputTokens * pricing.getOutputPricePerMillion()) / 1_000_000.0;
        return inputCost + outputCost;
    }
}
