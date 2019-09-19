package io.qingmu.demogateway.advice;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class CustomException extends RuntimeException {
    protected int code;
    protected String message;

    public CustomException() {
        super();
    }

    public CustomException(String message) {
        super(message);
        this.message = message;
    }

    public CustomException(int code, String message) {
        super(message);
        this.message = message;
        this.code = code;
    }

}
