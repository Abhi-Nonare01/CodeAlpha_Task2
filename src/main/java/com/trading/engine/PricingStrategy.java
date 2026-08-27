package com.trading.engine;

import com.trading.domain.Stock;
import java.util.Random;

/**
 * Strategy interface for simulating asset price movements.
 */
public interface PricingStrategy {
    /**
     * Computes the next simulated price for a stock given a time step and external shocks.
     *
     * @param stock The stock being priced
     * @param dt Time delta (e.g. 1/252 for daily step or smaller for intraday)
     * @param newsShockPct Temporary shock from news sentiment in percent
     * @return New simulated price
     */
    double calculateNextPrice(Stock stock, double dt, double newsShockPct);
    
    /**
     * Default Geometric Brownian Motion (GBM) implementation with Jump Diffusion.
     */
    class GeometricBrownianMotion implements PricingStrategy {
        private final Random random = new Random();

        @Override
        public double calculateNextPrice(Stock stock, double dt, double newsShockPct) {
            double s0 = stock.getCurrentPrice();
            double mu = stock.getDrift();
            double sigma = stock.getVolatility();

            // Standard normal random variable Z ~ N(0, 1) using Box-Muller
            double z = random.nextGaussian();

            // GBM formula: S(t+dt) = S(t) * exp((mu - 0.5 * sigma^2)*dt + sigma * sqrt(dt) * Z)
            double driftTerm = (mu - 0.5 * Math.pow(sigma, 2)) * dt;
            double diffusionTerm = sigma * Math.sqrt(dt) * z;
            double gbmMultiplier = Math.exp(driftTerm + diffusionTerm);

            // Incorporate news shock (if any)
            double shockMultiplier = 1.0 + (newsShockPct / 100.0);

            // Small mean-reverting micro-noise to prevent runaway price spikes
            double microNoise = 1.0 + (random.nextDouble() - 0.5) * 0.002;

            double nextPrice = s0 * gbmMultiplier * shockMultiplier * microNoise;

            // Ensure price is at least $0.01
            return Math.max(0.01, Math.round(nextPrice * 100.0) / 100.0);
        }
    }
}
