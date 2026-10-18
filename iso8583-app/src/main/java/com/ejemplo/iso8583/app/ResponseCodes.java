package com.ejemplo.iso8583.app;

import java.util.Map;

/** Descripcion de los codigos de respuesta (campo 39) mas usados. */
public final class ResponseCodes {

    private static final Map<String, String> CODES = Map.ofEntries(
            Map.entry("00", "Aprobada"),
            Map.entry("01", "Referir al emisor"),
            Map.entry("05", "No aprobada"),
            Map.entry("12", "Transaccion invalida"),
            Map.entry("13", "Monto invalido"),
            Map.entry("14", "Numero de tarjeta invalido"),
            Map.entry("17", "Cancelada por el cliente"),
            Map.entry("30", "Error de formato"),
            Map.entry("41", "Tarjeta perdida"),
            Map.entry("43", "Tarjeta robada"),
            Map.entry("51", "Fondos insuficientes"),
            Map.entry("54", "Tarjeta vencida"),
            Map.entry("55", "PIN incorrecto"),
            Map.entry("57", "Transaccion no permitida al tarjetahabiente"),
            Map.entry("61", "Excede limite de monto"),
            Map.entry("91", "Emisor no disponible"),
            Map.entry("96", "Error del sistema"));

    private ResponseCodes() {
    }

    public static String describe(String code) {
        return CODES.getOrDefault(code, "Codigo desconocido");
    }
}
