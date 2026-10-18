package com.ejemplo.iso8583.net;

import com.ejemplo.iso8583.core.IsoException;
import com.ejemplo.iso8583.core.IsoMessage;
import com.ejemplo.iso8583.core.IsoPackager;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Servidor TCP ISO 8583. Acepta varias conexiones a la vez (un hilo por conexion)
 * y cada conexion puede enviar muchos mensajes (conexion persistente).
 */
public class Iso8583Server implements AutoCloseable {

    private final int port;
    private final IsoPackager packager;
    private final Framing framing;
    private final MessageHandler handler;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private Consumer<String> log = System.out::println;
    private volatile ServerSocket serverSocket;

    public Iso8583Server(int port, IsoPackager packager, Framing framing, MessageHandler handler) {
        this.port = port;
        this.packager = packager;
        this.framing = framing;
        this.handler = handler;
    }

    public void setLogger(Consumer<String> log) {
        this.log = log;
    }

    /** Abre el puerto y empieza a aceptar conexiones en segundo plano. */
    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        log.accept("[SERVIDOR] Escuchando en el puerto " + getPort() + " (encabezado " + framing + ")");
        pool.submit(this::acceptLoop);
    }

    /** Puerto real (util si se inicio con puerto 0 = aleatorio). */
    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                pool.submit(() -> serve(socket));
            } catch (IOException ex) {
                if (!serverSocket.isClosed()) {
                    log.accept("[SERVIDOR] Error aceptando conexion: " + ex.getMessage());
                }
            }
        }
    }

    private void serve(Socket socket) {
        String peer = socket.getRemoteSocketAddress().toString();
        log.accept("[SERVIDOR] Conexion desde " + peer);
        try (socket; InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
            while (true) {
                byte[] raw;
                try {
                    raw = framing.read(in);
                } catch (EOFException eof) {
                    break;
                }
                IsoMessage request;
                try {
                    request = packager.unpack(raw);
                } catch (IsoException ex) {
                    log.accept("[SERVIDOR] Mensaje invalido de " + peer + ": " + ex.getMessage());
                    continue;
                }
                log.accept("[SERVIDOR] <<< Recibido de " + peer + System.lineSeparator() + request);

                IsoMessage response;
                try {
                    response = handler.handle(request);
                } catch (RuntimeException ex) {
                    log.accept("[SERVIDOR] Error procesando mensaje: " + ex);
                    response = request.isRequest() ? request.createResponse().set(39, "96") : null;
                }
                if (response != null) {
                    framing.write(out, packager.pack(response));
                    log.accept("[SERVIDOR] >>> Enviado a " + peer + System.lineSeparator() + response);
                }
            }
        } catch (SocketException ex) {
            // conexion reiniciada por el cliente o servidor detenido
        } catch (IOException ex) {
            log.accept("[SERVIDOR] Error de E/S con " + peer + ": " + ex.getMessage());
        }
        log.accept("[SERVIDOR] Conexion cerrada: " + peer);
    }

    @Override
    public void close() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
        pool.shutdownNow();
    }
}
