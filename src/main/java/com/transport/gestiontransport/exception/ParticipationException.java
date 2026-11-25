package com.transport.gestiontransport.exception;

public class ParticipationException extends BusinessException {
    private final String code;
    public ParticipationException(String code, String message) {
        super(message);
        this.code = code;
    }
    public String getCode() { return code; }
}
