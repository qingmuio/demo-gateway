package io.qingmu.demogateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

//./wrk -c 200 -t 200 -d 1m
@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthorizationFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;
    final static String checkUrl = "http://USER-SERVICE/usr/v2/check?projectId=%s&sessionId=%s";


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final ServerHttpRequest request = exchange.getRequest();
        final HttpHeaders headers = request.getHeaders();

        String sessionIdVar = request.getQueryParams().getFirst("sessionId");
        if (StringUtils.isEmpty(sessionIdVar)) {
            sessionIdVar = headers.getFirst("X-SESSION-ID");
        }

        String projectIdVar = request.getQueryParams().getFirst("projectId");
        if (StringUtils.isEmpty(projectIdVar)) {
            projectIdVar = headers.getFirst("X-PROJECT-ID");
        }

        return chain.filter(exchange);

    }


    private Mono<Void> getFilter(ServerWebExchange exchange, GatewayFilterChain chain, String projectId, String userId, String sessionId) {
        return chain.filter(exchange
                .mutate()
                .request(exchange
                        .getRequest()
                        .mutate()
                        .uri(getUri(exchange, projectId, userId, sessionId))
                        .build())
                .build());
    }


    private URI getUri(ServerWebExchange exchange, String projectId, String userId, String sessionId) {
        final ServerHttpRequest request = exchange.getRequest();
        final URI uri = request.getURI();
        final StringBuilder query = new StringBuilder();
        String originalQuery = uri.getRawQuery();

        if (StringUtils.hasText(originalQuery)) {
            query.append(originalQuery);
            if (originalQuery.charAt(originalQuery.length() - 1) != '&') {
                query.append('&');
            }
        }
        query.append("userId=").append(userId);

        if (StringUtils.isEmpty(request.getQueryParams().getFirst("projectId"))) {
            query.append("&projectId=").append(projectId);
        }
        if (StringUtils.isEmpty(request.getQueryParams().getFirst("sessionId"))) {
            query.append("&sessionId=").append(sessionId);
        }

        return UriComponentsBuilder.fromUri(uri)
                .replaceQuery(query.toString())
                .build(true)
                .toUri();
    }


    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
