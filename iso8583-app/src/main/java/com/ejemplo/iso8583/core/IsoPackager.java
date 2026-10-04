package com.ejemplo.iso8583.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Convierte un {@link IsoMessage} a bytes y viceversa.
 *
 * Formato en el cable (el mas comun en implementaciones "ASCII"):
 * <pre>
 *   MTI         4 caracteres ASCII
 *   Bitmap      8 bytes binarios (16 si hay campos 65..128: el bit 1 indica bitmap secundario)
 *   Campos      en orden ascendente
 *                 - numericos y alfanumericos en ASCII
 *                 - prefijos LL/LLL en digitos ASCII
 *                 - binarios como bytes crudos (el prefijo de longitud cuenta bytes)
 * </pre>
 */
public class IsoPackager {

    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private final Map<Integer, FieldDefinition> definitions;
    private final Charset charset;

    public IsoPackager() {
        this(Iso87Fields.definitions(), StandardCharsets.US_ASCII);
    }

    public IsoPackager(Map<Integer, FieldDefinition> definitions, Charset charset) {
        this.definitions = definitions;
        this.charset = charset;
    }

    public Map<Integer, FieldDefinition> getDefinitions() {
        return definitions;
    }

    // ------------------------------------------------------------------ pack

    public byte[] pack(IsoMessage msg) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(msg.getMti().getBytes(charset));

        boolean secondary = msg.getFields().keySet().stream().anyMatch(f -> f > 64);
        byte[] bitmap = new byte[secondary ? 16 : 8];
        if (secondary) {
            setBit(bitmap, 1);
        }
        for (int field : msg.getFields().keySet()) {
            setBit(bitmap, field);
        }
        out.writeBytes(bitmap);

        for (Map.Entry<Integer, String> e : msg.getFields().entrySet()) {
            out.writeBytes(packField(definition(e.getKey()), e.getValue()));
        }
        return out.toByteArray();
    }

    private byte[] packField(FieldDefinition def, String value) {
        byte[] data = encodeValue(def, value);
        int max = def.maxLength();

        if (def.lengthType() == LengthType.FIXED) {
            if (data.length != max) {
                throw new IsoException(String.format("Campo %d: longitud %d, se esperaba %d", def.number(), data.length, max));
            }
            return data;
        }
        if (data.length > max) {
            throw new IsoException(String.format("Campo %d: longitud %d excede el maximo %d", def.number(), data.length, max));
        }
        String prefix = String.format("%0" + def.lengthType().prefixDigits() + "d", data.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(prefix.getBytes(charset));
        out.writeBytes(data);
        return out.toByteArray();
    }

    private byte[] encodeValue(FieldDefinition def, String value) {
        boolean fixed = def.lengthType() == LengthType.FIXED;
        switch (def.type()) {
            case NUMERIC:
                if (!value.chars().allMatch(Character::isDigit)) {
                    throw new IsoException("Campo " + def.number() + ": se esperaban solo digitos, se recibio '" + value + "'");
                }
                if (fixed && value.length() < def.maxLength()) {
                    value = "0".repeat(def.maxLength() - value.length()) + value;
                }
                return value.getBytes(charset);
            case ALPHA:
                if (fixed && value.length() < def.maxLength()) {
                    value = value + " ".repeat(def.maxLength() - value.length());
                }
                return value.getBytes(charset);
            case BINARY:
                try {
                    return HEX.parseHex(value);
                } catch (IllegalArgumentException ex) {
                    throw new IsoException("Campo " + def.number() + ": valor hexadecimal invalido", ex);
                }
            default:
                throw new IllegalStateException();
        }
    }

    // ---------------------------------------------------------------- unpack

    public IsoMessage unpack(byte[] data) {
        try {
            Cursor c = new Cursor(data);
            IsoMessage msg = new IsoMessage(new String(c.take(4), charset));

            byte[] bitmap = c.take(8);
            if (isBitSet(bitmap, 1)) {
                byte[] full = new byte[16];
                System.arraycopy(bitmap, 0, full, 0, 8);
                System.arraycopy(c.take(8), 0, full, 8, 8);
                bitmap = full;
            }
            int maxField = bitmap.length * 8;
            for (int field = 2; field <= maxField; field++) {
                if (isBitSet(bitmap, field)) {
                    msg.set(field, unpackField(definition(field), c));
                }
            }
            if (c.remaining() > 0) {
                throw new IsoException("Quedaron " + c.remaining() + " bytes sin procesar al final del mensaje");
            }
            return msg;
        } catch (ArrayIndexOutOfBoundsException | NegativeArraySizeException ex) {
            throw new IsoException("Mensaje truncado o mal formado", ex);
        }
    }

    private String unpackField(FieldDefinition def, Cursor c) {
        int length = def.maxLength();
        if (def.lengthType() != LengthType.FIXED) {
            String prefix = new String(c.take(def.lengthType().prefixDigits()), charset);
            try {
                length = Integer.parseInt(prefix);
            } catch (NumberFormatException ex) {
                throw new IsoException("Campo " + def.number() + ": prefijo de longitud invalido '" + prefix + "'");
            }
            if (length > def.maxLength()) {
                throw new IsoException(String.format("Campo %d: longitud %d excede el maximo %d", def.number(), length, def.maxLength()));
            }
        }
        byte[] raw = c.take(length);
        return def.type() == FieldType.BINARY ? HEX.formatHex(raw) : new String(raw, charset);
    }

    // --------------------------------------------------------------- helpers

    private FieldDefinition definition(int field) {
        FieldDefinition def = definitions.get(field);
        if (def == null) {
            throw new IsoException("No hay definicion para el campo " + field);
        }
        return def;
    }

    /** Los bits se numeran desde 1 (bit mas significativo del primer byte). */
    private static void setBit(byte[] bitmap, int bit) {
        bitmap[(bit - 1) / 8] |= (byte) (0x80 >>> ((bit - 1) % 8));
    }

    private static boolean isBitSet(byte[] bitmap, int bit) {
        return (bitmap[(bit - 1) / 8] & (0x80 >>> ((bit - 1) % 8))) != 0;
    }

    private static final class Cursor {
        private final byte[] data;
        private int pos;

        Cursor(byte[] data) {
            this.data = data;
        }

        byte[] take(int n) {
            if (pos + n > data.length) {
                throw new IsoException("Mensaje truncado: se necesitaban " + n + " bytes en la posicion " + pos);
            }
            byte[] out = new byte[n];
            System.arraycopy(data, pos, out, 0, n);
            pos += n;
            return out;
        }

        int remaining() {
            return data.length - pos;
        }
    }
}
