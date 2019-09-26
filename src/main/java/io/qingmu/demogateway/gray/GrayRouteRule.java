package io.qingmu.demogateway.gray;

import com.alibaba.cloud.nacos.ribbon.NacosServer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Slf4j
public class GrayRouteRule extends ZoneAvoidanceRule {

    private final String VERSION = "version";

    @Autowired
    private GrayRouteRuleConfig grayConfig;

    @Override
    public Server choose(Object key) {
        final List<Server> allServers = getLoadBalancer().getAllServers();
        final ServerWebExchange serverWebExchange = LoadBalancerClientFilterAspectJ.getExchange();
        if(serverWebExchange == null){
            return super.choose(key);
        }

        final String serviceId = ((URI) serverWebExchange.getAttribute(GATEWAY_REQUEST_URL_ATTR)).getHost();
        final Map<String, Integer> serverGrayConfig = grayConfig.getWeightMap().get(serviceId);
        /*没有发现灰度规则 跳过*/
        if (serverGrayConfig == null || serverGrayConfig.isEmpty()) {
            return super.choose(key);
        }
        // 根据权重选择版本 如有{v1.0.0=95,v1.1.0=5} = {v1.0.0}
        final String weighedInstance = this.randomWeight(serverGrayConfig);
        if (weighedInstance == null) {
            log.warn("weighedInstance is null,serverGrayConfig is {}", serverGrayConfig);
            return super.choose(key);
        } else {
            log.debug("selected : {}", weighedInstance);
        }
//         根据权重选择版本 如有{v1.0.0=95,v1.1.0=5} = {v1.0.0}
//        final String weighedInstance = serverWebExchange.getRequest().getHeaders().getFirst(GrayFilter.X_GRAY_VERSION);
//        if (StringUtils.isBlank(weighedInstance)) {
//            return super.choose(key);
//        }
        // {v1.0.0:[192.168.0.1,192.168.0.2],v1.1.0:[192.168.1.1,192.168.1.2]}
        Map<String, List<Server>> versionServers = new HashMap<>();
        final String other = "other";
        for (Server server : allServers) {
            if (server instanceof NacosServer) {
                final NacosServer nacosServer = (NacosServer) server;
                final Map<String, String> metadata = nacosServer.getMetadata();
                final String version = metadata.get(VERSION);
                // 未打版本号的服务归到other
                if (StringUtils.isBlank(version)) {
                    if (!versionServers.containsKey(other)) {
                        versionServers.put(other, new ArrayList<>(2));
                    }
                    versionServers.get(other).add(server);
                } else {
                    // 多版本支持
                    if (StringUtils.contains(version, ",")) {
                        final String[] versions = StringUtils.split(version, ",");
                        for (String supportVersion : versions) {
                            if (!versionServers.containsKey(supportVersion)) {
                                versionServers.put(supportVersion, new ArrayList<>(2));
                            }
                            versionServers.get(supportVersion).add(server);
                        }
                    } else {
                        if (!versionServers.containsKey(version)) {
                            versionServers.put(version, new ArrayList<>(2));
                        }
                        versionServers.get(version).add(server);
                    }
                }
            }
        }
        final List<Server> servers = versionServers.get(weighedInstance);
        if (servers == null || servers.isEmpty()) {
            log.warn("selected version ：{} ，but server list is empty，using all servers", weighedInstance);
            return super.choose(key);

        }
        return getServer(key, servers);
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

    private Server getServer(Object key, List<Server> allServers) {
        com.google.common.base.Optional<Server> server = getPredicate().chooseRoundRobinAfterFiltering(allServers, key);
        if (server.isPresent()) {
            return server.get();
        } else {
            return null;
        }
    }
}