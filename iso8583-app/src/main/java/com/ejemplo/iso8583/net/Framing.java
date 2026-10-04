package com.ejemplo.iso8583.net;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Encabezado de longitud que antecede a cada mensaje en TCP.
 * ISO 8583 no define el transporte, asi que cada red elige uno; estos son los dos mas comunes.
 */
public enum Framing {
    /** 2 bytes binarios big-endian con la longitud del mensaje (maximo 65535). */
    BINARY_2,
    /** 4 digitos ASCII con la longitud del mensaje, p. ej. "0123" (maximo 9999). */
    ASCII_4;

    public void write(OutputStream out, byte[] message) throws IOException {
        int len = message.length;
        switch (this) {
            case BINARY_2:
                if (len > 0xFFFF) {
                    throw new IOException("Mensaje demasiado grande para encabezado de 2 bytes: " + len);
                }
                out.write((len >>> 8) & 0xFF);
                out.write(len & 0xFF);
                break;
            case ASCII_4:
                if (len > 9999) {
                    throw new IOException("Mensaje demasiado grande para encabezado ASCII de 4 digitos: " + len);
                }
                out.write(String.format("%04d", len).getBytes(StandardCharsets.US_ASCII));
                break;
            default:
                throw new IllegalStateException();
        }
        out.write(message);
        out.flush();
    }

    /** Lee un mensaje completo. Lanza {@link EOFException} si la conexion se cerro limpiamente. */
    public byte[] read(InputStream in) throws IOException {
        DataInputStream din = new DataInputStream(in);
        int len;
        switch (this) {
            case BINARY_2:
                len = din.readUnsignedShort();
                break;
            case ASCII_4:
                byte[] header = new byte[4];
                din.readFully(header);
                String text = new String(header, StandardCharsets.US_ASCII);
                try {
                    len = Integer.parseInt(text);
                } catch (NumberFormatException ex) {
                    throw new IOException("Encabezado de longitud invalido: '" + text + "'");
                }
                break;
            default:
                throw new IllegalStateException();
        }
        byte[] body = new byte[len];
        din.readFully(body);
        return body;
    }
}
