package com.agent.tracking;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PricingRegistry {

    private final Map<String, ModelPricing> registry = new ConcurrentHashMap<>();

    public PricingRegistry() {
        registry.put("claude-sonnet-4-20250514", new ModelPricing(3.0, 15.0));
        registry.put("claude-3-5-haiku", new ModelPricing(1.0, 5.0));
        registry.put("gpt-4o", new ModelPricing(5.0, 15.0));
        registry.put("mock-model", new ModelPricing(1.0, 1.0));
    }

    public ModelPricing getPricing(String model) {
        return registry.getOrDefault(model, new ModelPricing(0.0, 0.0));
    }

    public static class ModelPricing {
        private final double inputPricePerMillion;
        private final double outputPricePerMillion;

        public ModelPricing(double inputPricePerMillion, double outputPricePerMillion) {
            this.inputPricePerMillion = inputPricePerMillion;
            this.outputPricePerMillion = outputPricePerMillion;
        }

        public double getInputPricePerMillion() {
            return inputPricePerMillion;
        }

        public double getOutputPricePerMillion() {
            return outputPricePerMillion;
        }
    }
}
