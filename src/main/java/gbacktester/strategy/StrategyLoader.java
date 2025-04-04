package gbacktester.strategy;

import org.reflections.Reflections;

import gbacktester.strategy.Strategy;
import gbacktester.strategy.annotations.AutoLoadStrategy;

import java.util.Set;
import java.util.stream.Collectors;

public class StrategyLoader {

    public static Set<Class<? extends Strategy>> scanAnnotatedStrategies(String basePackage) {
        Reflections reflections = new Reflections(basePackage);

        // Scan all classes annotated with @AutoLoadStrategy that extend Strategy
        return reflections.getTypesAnnotatedWith(AutoLoadStrategy.class).stream()
                .filter(Strategy.class::isAssignableFrom)
                .map(cls -> (Class<? extends Strategy>) cls)
                .collect(Collectors.toSet());
    }
}

