package io.qingmu.demogateway.gray;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.server.ServerWebExchange;

@Aspect
@Slf4j
public class LoadBalancerClientFilterAspectJ {

    public static final ThreadLocal<ServerWebExchange> EXCHANGE_THREAD_LOCAL = new InheritableThreadLocal<>();

    @Around("execution( * org.springframework.cloud.gateway.filter.LoadBalancerClientFilter.filter(..))")
    public Object onBefore(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            final ServerWebExchange exchange = (ServerWebExchange) joinPoint.getArgs()[0];
            EXCHANGE_THREAD_LOCAL.set(exchange);
            System.out.println(Thread.currentThread());
            return joinPoint.proceed();
        } finally {
            EXCHANGE_THREAD_LOCAL.remove();
        }
    }

    public static ServerWebExchange getExchange() {
        return EXCHANGE_THREAD_LOCAL.get();
    }
}