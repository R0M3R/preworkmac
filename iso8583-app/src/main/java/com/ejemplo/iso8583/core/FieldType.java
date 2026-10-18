package com.ejemplo.iso8583.core;

/** Tipo de contenido de un campo ISO 8583. */
public enum FieldType {
    /** Solo digitos (n). Los campos fijos se rellenan con ceros a la izquierda. */
    NUMERIC,
    /** Alfanumerico/especial (a, an, ans, z). Los campos fijos se rellenan con espacios a la derecha. */
    ALPHA,
    /** Binario (b). En el mensaje se guarda como hexadecimal; en el cable viaja como bytes crudos. */
    BINARY
}
