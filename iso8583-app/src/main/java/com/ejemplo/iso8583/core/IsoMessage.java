package com.ejemplo.iso8583.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Mensaje ISO 8583: un MTI (Message Type Indicator) y un conjunto de campos 2..128.
 *
 * Los valores se guardan como texto. Para los campos binarios (52, 55, 64, 96, 128)
 * el valor es la representacion hexadecimal de los bytes.
 * El bitmap no se guarda: el {@link IsoPackager} lo calcula a partir de los campos presentes.
 */
public class IsoMessage {

    private String mti;
    private final SortedMap<Integer, String> fields = new TreeMap<>();

    public IsoMessage(String mti) {
        setMti(mti);
    }

    public String getMti() {
        return mti;
    }

    public void setMti(String mti) {
        if (mti == null || !mti.matches("\\d{4}")) {
            throw new IsoException("MTI invalido: '" + mti + "' (deben ser 4 digitos)");
        }
        this.mti = mti;
    }

    public IsoMessage set(int field, String value) {
        if (field < 2 || field > 128) {
            throw new IsoException("Numero de campo fuera de rango: " + field);
        }
        fields.put(field, Objects.requireNonNull(value, "valor del campo " + field));
        return this;
    }

    public String get(int field) {
        return fields.get(field);
    }

    public boolean has(int field) {
        return fields.containsKey(field);
    }

    public IsoMessage remove(int field) {
        fields.remove(field);
        return this;
    }

    public SortedMap<Integer, String> getFields() {
        return Collections.unmodifiableSortedMap(fields);
    }

    /** true si el MTI corresponde a una solicitud o aviso (tercer digito par: 0, 2, 4...). */
    public boolean isRequest() {
        return (mti.charAt(2) - '0') % 2 == 0;
    }

    /**
     * Crea la respuesta a este mensaje: suma 10 al MTI (0200 -> 0210, 0800 -> 0810)
     * y copia los campos que normalmente se devuelven sin cambios.
     */
    public IsoMessage createResponse() {
        if (!isRequest()) {
            throw new IsoException("El mensaje " + mti + " ya es una respuesta");
        }
        String responseMti = String.format("%04d", Integer.parseInt(mti) + 10);
        IsoMessage response = new IsoMessage(responseMti);
        int[] echoed = {2, 3, 4, 7, 11, 12, 13, 22, 24, 25, 32, 37, 41, 42, 49, 70, 90};
        for (int f : echoed) {
            if (has(f)) {
                response.set(f, get(f));
            }
        }
        return response;
    }

    @Override
    public String toString() {
        return toString(Iso87Fields.definitions());
    }

    /** Representacion legible del mensaje, con el PAN y datos sensibles enmascarados. */
    public String toString(Map<Integer, FieldDefinition> definitions) {
        StringBuilder sb = new StringBuilder();
        sb.append("  MTI  : ").append(mti).append(System.lineSeparator());
        for (Map.Entry<Integer, String> e : fields.entrySet()) {
            FieldDefinition def = definitions.get(e.getKey());
            String name = def != null ? def.name() : "?";
            sb.append(String.format("  [%3d] %-45s : %s%n", e.getKey(), name, mask(e.getKey(), e.getValue())));
        }
        return sb.toString();
    }

    private static String mask(int field, String value) {
        switch (field) {
            case 2:
                if (value.length() <= 10) {
                    return "*".repeat(value.length());
                }
                return value.substring(0, 6) + "*".repeat(value.length() - 10) + value.substring(value.length() - 4);
            case 35:
            case 45:
            case 52:
                return "*** (" + value.length() + " caracteres)";
            default:
                return value;
        }
    }
}
