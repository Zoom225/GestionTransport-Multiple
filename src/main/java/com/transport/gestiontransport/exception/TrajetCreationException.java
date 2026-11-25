package com.transport.gestiontransport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TrajetCreationException extends RuntimeException {

    public TrajetCreationException(String message) {
        super(message);
    }
}
