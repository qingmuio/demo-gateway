package io.qingmu.demogateway.gray;

import com.alibaba.cloud.nacos.ribbon.NacosServer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class DeveloperRouteRule extends ZoneAvoidanceRule {

    @Override
    public Server choose(Object key) {
        final List<Server> allServers = getLoadBalancer().getAllServers();
        final ServerWebExchange serverWebExchange = LoadBalancerClientFilterAspectJ.getExchange();
        // gateway 内部调用 可能会为null
        if (serverWebExchange == null) {
            return getServer(key, allServers);
        }
        final ServerHttpRequest request = serverWebExchange.getRequest();

        // 存放已打标签但不满足标签的server
        final List<Server> metaServers = new ArrayList<>();

        // 存放未标签的server
        final List<Server> noMetaServers = new ArrayList<>();

        // 匹配成功的server
        final List<Server> matchedMetaServers = new ArrayList<>();

        final MultiValueMap<String, String> attributes = request.getQueryParams();
        // 取得接口端传入的参数
        String inputDeveloper = attributes.getFirst("developer");
        if (StringUtils.isBlank(inputDeveloper)) {
            final List<String> developerHeaders = request.getHeaders().get("developer");
            if (developerHeaders != null && !developerHeaders.isEmpty()) {
                inputDeveloper = developerHeaders.get(0);
            }
        }


        for (Server server : allServers) {
            if (server instanceof NacosServer) {
                final NacosServer nacosServer = (NacosServer) server;
                final Map<String, String> metadata = nacosServer.getMetadata();
                final String developer = metadata.get("developer");
                // 如果没有meta数据 表示是测试服务上的地址
                if (developer == null || developer.equals("")) {
                    // 存放并没有打标签的server
                    noMetaServers.add(server);
                } else {
                    // 如果匹配成功开发者直接调用
                    if (inputDeveloper != null && (!"".equals(inputDeveloper)) && developer.equals(inputDeveloper)) {
                        matchedMetaServers.add(server);
                    } else {
                        // 存入server有标签但是不匹配的server
                        metaServers.add(server);
                    }
                }


            }
        }

        //优先走自定义路由。即满足灰度要求的server
        if (!matchedMetaServers.isEmpty()) {
            log.info("匹配成功 matchedMetaServers ：{} ", matchedMetaServers);
            return getServer(key, matchedMetaServers);
        }
        // 如果没有匹配成功的则走
        else {
            if (!noMetaServers.isEmpty()) {
                return getServer(key, noMetaServers);
            } else {
                // 似情况打开
                return null;
//                com.google.common.base.Optional<Server> server = getPredicate().chooseRoundRobinAfterFiltering(metaServers, key);
//                if (server.isPresent()) {
//                    return server.get();
//                } else {
//                    return null;
//                }
            }

        }

    }

    private Server getServer(Object key, List<Server> allServers) {
        com.google.common.base.Optional<Server> server = getPredicate().chooseRoundRobinAfterFiltering(allServers, key);
        if (server.isPresent()) {
            return server.get();
        } else {
            return null;
        }
    }
}