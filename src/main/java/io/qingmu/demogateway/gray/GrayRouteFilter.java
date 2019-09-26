package io.qingmu.demogateway.gray;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.cloud.gateway.filter.LoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Slf4j
public class GrayRouteFilter implements GlobalFilter, Ordered {

    @Autowired
    private GrayRouteRuleConfig grayConfig;
    /* 保证在lb之前 */
    public static final int GRAY_FILTER_ORDER = LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;
    public static final String X_GRAY_VERSION = "X-Gray-Version";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String serviceId = ((URI) exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR)).getHost();
        final Map<String, Integer> serverGrayConfig = grayConfig.getWeightMap().get(serviceId);
        /*没有发现灰度规则 跳过*/
        if (serverGrayConfig == null || serverGrayConfig.isEmpty()) {
            return chain.filter(exchange);
        }
        /* 如果设置过了直接跳过 */
        final String version = exchange.getRequest().getHeaders().getFirst(X_GRAY_VERSION);
        if (StringUtils.isNotBlank(version)) {
            return chain.filter(exchange);
        }

        // 根据权重选择版本 如有{v1.0.0=95,v1.1.0=5} = {v1.0.0}
        final String weighedInstance = this.randomWeight(serverGrayConfig);
        if (weighedInstance == null) {
            log.warn("weighedInstance is null,serverGrayConfig is {}", serverGrayConfig);
            return chain.filter(exchange);
        } else {
            log.debug("selected : {}", weighedInstance);
        }
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(X_GRAY_VERSION, weighedInstance).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return GRAY_FILTER_ORDER;
    }

    public String randomWeight(Map<String, Integer> serverMap) {
        int length = serverMap.size(); // 总个数
        final ArrayList<String> servers = new ArrayList<>(serverMap.keySet());
        int totalWeight = 0; // 总权重
        boolean sameWeight = true; // 权重是否都一样
        for (int i = 0; i < length; i++) {
            int weight = serverMap.get(servers.get(i));
            totalWeight += weight; // 累计总权重
            if (sameWeight && i > 0
                    && weight != serverMap.get(servers.get(i - 1))) {
                sameWeight = false; // 计算所有权重是否一样
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            // 如果权重不相同且权重大于0则按总权重数随机
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            // 并确定随机值落在哪个片断上
            for (int i = 0; i < length; i++) {
                offset -= serverMap.get(servers.get(i));
                if (offset < 0) {
                    return servers.get(i);
                }
            }
        }
        // 如果权重相同或权重为0则均等随机
        return servers.get(ThreadLocalRandom.current().nextInt(length));
    }
}
