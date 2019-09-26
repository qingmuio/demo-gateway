package io.qingmu.demogateway.advice;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.ValidationException;
import java.nio.charset.Charset;

@Setter
@Slf4j
public class CustomExceptionHandler implements ErrorWebExceptionHandler {


    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 按照异常类型进行处理
        final ServerHttpRequest request = exchange.getRequest();
        HttpStatus httpStatus;
        String body;
        int code = 500;
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            httpStatus = responseStatusException.getStatus();
            if (httpStatus == HttpStatus.NOT_FOUND) {
                body = "服务接口未找到-404,path:" + request.getPath().value();
            } else
                body = responseStatusException.getMessage();
        } else if (ex instanceof CustomException) {
            body = ((CustomException) ex).getMessage();
            code = ((CustomException) ex).getCode();
        } else if (ex instanceof WebClientResponseException) {
            final Response result = JsonUtils.fromJson(((WebClientResponseException) ex).getResponseBodyAsString(), Response.class);
            body = result.getMessage();
            code = result.getCode();
        } else if (ex instanceof ValidationException) {
            body = ex.getMessage();
            code = 400;
        } else {
            log.error(ex.getMessage(), ex);
            body = "服务器繁忙-请稍后重试。";
        }
        log.error("{},{},{}", request.getPath(), ex.getMessage(),request.getQueryParams());
        final ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        response.getHeaders()
                .setContentType(MediaType
                        .APPLICATION_JSON_UTF8);
        response.setStatusCode(HttpStatus
                .INTERNAL_SERVER_ERROR);
        return response
                .writeWith(Mono
                        .just(response
                                .bufferFactory()
                                .wrap(JsonUtils
                                        .toJson(Response
                                                .builder()
                                                .code(code)
                                                .message(body)
                                                .build())
                                        .getBytes(Charset
                                                .forName("UTF-8")))));

    }
}
