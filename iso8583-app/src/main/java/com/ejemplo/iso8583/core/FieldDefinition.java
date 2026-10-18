package com.ejemplo.iso8583.core;

/**
 * Definicion de un campo (data element) ISO 8583.
 *
 * @param number     numero de campo (2..128)
 * @param name       descripcion legible
 * @param type       tipo de contenido
 * @param lengthType fijo, LLVAR o LLLVAR
 * @param maxLength  longitud (fija) o maxima (variable). Para BINARY se expresa en bytes.
 */
public record FieldDefinition(int number, String name, FieldType type, LengthType lengthType, int maxLength) {

    public static FieldDefinition n(int number, String name, int length) {
        return new FieldDefinition(number, name, FieldType.NUMERIC, LengthType.FIXED, length);
    }

    public static FieldDefinition an(int number, String name, int length) {
        return new FieldDefinition(number, name, FieldType.ALPHA, LengthType.FIXED, length);
    }

    public static FieldDefinition b(int number, String name, int bytes) {
        return new FieldDefinition(number, name, FieldType.BINARY, LengthType.FIXED, bytes);
    }

    public static FieldDefinition llvarN(int number, String name, int max) {
        return new FieldDefinition(number, name, FieldType.NUMERIC, LengthType.LLVAR, max);
    }

    public static FieldDefinition llvar(int number, String name, int max) {
        return new FieldDefinition(number, name, FieldType.ALPHA, LengthType.LLVAR, max);
    }

    public static FieldDefinition lllvarN(int number, String name, int max) {
        return new FieldDefinition(number, name, FieldType.NUMERIC, LengthType.LLLVAR, max);
    }

    public static FieldDefinition lllvar(int number, String name, int max) {
        return new FieldDefinition(number, name, FieldType.ALPHA, LengthType.LLLVAR, max);
    }

    public static FieldDefinition lllvarB(int number, String name, int maxBytes) {
        return new FieldDefinition(number, name, FieldType.BINARY, LengthType.LLLVAR, maxBytes);
    }
}
