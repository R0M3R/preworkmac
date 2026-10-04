package com.ejemplo.iso8583.net;

import com.ejemplo.iso8583.core.IsoException;
import com.ejemplo.iso8583.core.IsoMessage;
import com.ejemplo.iso8583.core.IsoPackager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Cliente TCP ISO 8583 sincrono: envia una solicitud y espera la respuesta
 * cuyo STAN (campo 11) coincida. Mantiene la conexion abierta entre envios.
 */
public class Iso8583Client implements AutoCloseable {

    private final String host;
    private final int port;
    private final IsoPackager packager;
    private final Framing framing;
    private final int timeoutMillis;
    private Consumer<String> log = System.out::println;
    private boolean dumpHex;

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public Iso8583Client(String host, int port, IsoPackager packager, Framing framing, int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.packager = packager;
        this.framing = framing;
        this.timeoutMillis = timeoutMillis;
    }

    public void setLogger(Consumer<String> log) {
        this.log = log;
    }

    /** Si es true, imprime tambien los bytes enviados/recibidos en hexadecimal. */
    public void setDumpHex(boolean dumpHex) {
        this.dumpHex = dumpHex;
    }

    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMillis);
        socket.setSoTimeout(timeoutMillis);
        socket.setTcpNoDelay(true);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        log.accept("[CLIENTE] Conectado a " + host + ":" + port);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Envia la solicitud y bloquea hasta recibir la respuesta o agotar el tiempo de espera
     * ({@link java.net.SocketTimeoutException}).
     */
    public synchronized IsoMessage send(IsoMessage request) throws IOException {
        if (!isConnected()) {
            connect();
        }
        byte[] packed = packager.pack(request);
        log.accept("[CLIENTE] >>> Enviando" + System.lineSeparator() + request);
        if (dumpHex) {
            log.accept("[CLIENTE]     HEX: " + HexFormat.of().withUpperCase().formatHex(packed));
        }
        framing.write(out, packed);

        while (true) {
            byte[] raw = framing.read(in);
            if (dumpHex) {
                log.accept("[CLIENTE]     HEX: " + HexFormat.of().withUpperCase().formatHex(raw));
            }
            IsoMessage response;
            try {
                response = packager.unpack(raw);
            } catch (IsoException ex) {
                log.accept("[CLIENTE] Respuesta invalida ignorada: " + ex.getMessage());
                continue;
            }
            if (Objects.equals(request.get(11), response.get(11))) {
                log.accept("[CLIENTE] <<< Recibido" + System.lineSeparator() + response);
                return response;
            }
            log.accept("[CLIENTE] Mensaje con STAN " + response.get(11) + " no corresponde a la solicitud, se descarta");
        }
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
            log.accept("[CLIENTE] Desconectado");
        }
    }
}
