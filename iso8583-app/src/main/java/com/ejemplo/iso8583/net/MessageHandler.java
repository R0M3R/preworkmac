package com.ejemplo.iso8583.net;

import com.ejemplo.iso8583.core.IsoMessage;

/** Logica de negocio del servidor: recibe una solicitud y devuelve la respuesta (o null para no responder). */
@FunctionalInterface
public interface MessageHandler {
    IsoMessage handle(IsoMessage request);
}
