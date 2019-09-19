package io.qingmu.demogateway;

import io.netty.channel.ChannelOption;
import io.qingmu.demogateway.advice.CustomExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;

@SpringBootApplication
@EnableDiscoveryClient
public class DemoGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoGatewayApplication.class, args);
    }

    @Primary
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ErrorWebExceptionHandler errorWebExceptionHandler() {
        return new CustomExceptionHandler();
    }

    @Bean
    public WebClient webClient(LoadBalancerClient loadBalancerClient) {
        return WebClient.builder()
                .filter(new LoadBalancerExchangeFilterFunction(loadBalancerClient))
                .clientConnector(clientHttpConnector())
                .build();
    }

    @Bean
    public ClientHttpConnector clientHttpConnector() {
        TcpClient tcpClient = TcpClient.create(
                ConnectionProvider.fixed("fixed", 100, 5000)
        )
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20000);
        return new ReactorClientHttpConnector(HttpClient.from(tcpClient));
    }

}
