package io.qingmu.demogateway;

import io.netty.channel.ChannelOption;
import io.qingmu.demogateway.advice.CustomExceptionHandler;
import io.qingmu.demogateway.gray.GrayRouteRuleConfig;
import io.qingmu.demogateway.gray.GrayRouteRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;


@SpringBootApplication(scanBasePackages = "io.qingmu")
@EnableDiscoveryClient
@EnableAspectJAutoProxy
public class DemoGatewayApplication {

    @Autowired
    private GrayRouteRuleConfig matchConfig;


    public static void main(String[] args) {
        SpringApplication.run(DemoGatewayApplication.class, args);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GrayRouteRule grayAwareRule() {
        return new GrayRouteRule();
    }

//    @PostConstruct
//    public void init() {
//        final Thread thread = new Thread(() -> {
//            while (true) {
//                System.out.println(matchConfig.getWeightMap());
//                try {
//                    TimeUnit.SECONDS.sleep(10);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        });
//        thread.setDaemon(true);
//        thread.start();
//    }

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
