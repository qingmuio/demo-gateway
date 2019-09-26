package io.qingmu.demogateway.gray;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RefreshScope
@Setter
@Getter
@ConfigurationProperties(prefix = "spring.gray")
public class GrayRouteRuleConfig {
    private Map<String, Map<String, Integer>> weightMap = new HashMap<>();
}
