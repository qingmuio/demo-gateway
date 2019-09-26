package io.qingmu.demogateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class IcoController {

    @GetMapping("/favicon.ico")
    public Mono<Byte> favicon() {
        return Mono.just(new Byte((byte) 0));
    }
}
