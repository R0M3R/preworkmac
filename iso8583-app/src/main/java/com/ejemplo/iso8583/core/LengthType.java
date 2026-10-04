package com.ejemplo.iso8583.core;

/** Forma en que se codifica la longitud de un campo. */
public enum LengthType {
    /** Longitud fija. */
    FIXED(0),
    /** Longitud variable con prefijo de 2 digitos ASCII (hasta 99). */
    LLVAR(2),
    /** Longitud variable con prefijo de 3 digitos ASCII (hasta 999). */
    LLLVAR(3);

    private final int prefixDigits;

    LengthType(int prefixDigits) {
        this.prefixDigits = prefixDigits;
    }

    public int prefixDigits() {
        return prefixDigits;
    }
}
