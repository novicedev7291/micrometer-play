package com.example.micrometer;

import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * @author <a href="kuldeepyadav7291@gmail.com">Kuldeep</a>
 */
class SimpleMicrometerRun {
    private static final Logger log = LoggerFactory.getLogger(SimpleMicrometerRun.class);

    public static void main(String[] args) {
        MeterRegistry registry = compositeRegistry();

        Counter counter = registry.counter("counter", "method", "actual");

        Counter customCounter = Counter.builder("customCounter")
                                               .description("My custom counter")
                                                       .tags("dev", "performance")
                                               .register(registry);
        customCounter.increment(10.0);

        RandomEngine r = new MersenneTwister64(0);
        Normal dist = new Normal(0, 1, r);

        // This will run a background kind of job and will increment both counter periodically after initial delay of 100ms(play with it)
        // When counter incremented, micrometer will log the throughput i.e. no. of times counter incremented / 10(step) <=> increment/s
        // here, tag is dev and its value is performance and will appear as counter{method=actual} in logs.
        Flux.interval(Duration.ofMillis(100))
            .doOnEach(d -> {
                if (dist.nextDouble() + 0.1 > 0) {
                    counter.increment();
                    customCounter.increment();
                }
            })
            .blockLast();

    }

    private static SimpleMeterRegistry simpleRegistry() {
        return new SimpleMeterRegistry();
    }

    private static LoggingMeterRegistry compositeRegistry() {
        LoggingMeterRegistry.Builder builder = LoggingMeterRegistry.builder(new LoggingRegistryConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public Duration step() {
                // Means every 10 seconds, statistics will be published
                return Duration.ofSeconds(10);
            }
        });

        builder.clock(Clock.SYSTEM)
               .loggingSink(log::info);

        return builder.build();
    }
}
