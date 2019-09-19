
package io.qingmu.demogateway.advice;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Response<T> implements Serializable {

    protected int code;

    protected String message;

    protected T data;
}