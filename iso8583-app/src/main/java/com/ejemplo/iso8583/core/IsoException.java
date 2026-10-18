package com.ejemplo.iso8583.core;

/** Error al empaquetar, desempaquetar o validar un mensaje ISO 8583. */
public class IsoException extends RuntimeException {

    public IsoException(String message) {
        super(message);
    }

    public IsoException(String message, Throwable cause) {
        super(message, cause);
    }
}
