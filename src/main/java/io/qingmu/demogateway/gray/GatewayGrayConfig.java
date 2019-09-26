package io.qingmu.demogateway.gray;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayGrayConfig {

    @Bean
    public GrayRouteFilter grayFilter() {
        return new GrayRouteFilter();
    }

    @Bean
    public LoadBalancerClientFilterAspectJ loadBalancerClientFilterAspectJ() {
        return new LoadBalancerClientFilterAspectJ();
    }

}
