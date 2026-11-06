package kg.megalab.ministorebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String timezone, Cashier cashier, Jwt jwt) {
    public record Cashier(String login, String passwordHash) {}
    public record Jwt(String secret, long ttlHours) {}
}
