package com.ejemplo.iso8583.app;

import com.ejemplo.iso8583.core.IsoException;
import com.ejemplo.iso8583.core.IsoMessage;
import com.ejemplo.iso8583.core.IsoPackager;
import com.ejemplo.iso8583.net.Framing;
import com.ejemplo.iso8583.net.Iso8583Client;
import com.ejemplo.iso8583.net.Iso8583Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto de entrada.
 *
 * <pre>
 *   java -jar iso8583-app.jar server [puerto]              Inicia el servidor (autorizador simulado)
 *   java -jar iso8583-app.jar client [host] [puerto]       Cliente interactivo
 *   java -jar iso8583-app.jar demo                         Servidor + cliente en el mismo proceso
 *
 * Opciones:
 *   --framing BINARY_2|ASCII_4   Encabezado de longitud TCP (por defecto BINARY_2)
 *   --hex                        Muestra los bytes en hexadecimal (cliente)
 *   --timeout ms                 Tiempo de espera del cliente (por defecto 30000)
 * </pre>
 */
public final class Main {

    private static final String TEST_PAN = "4111111111111111";
    private static final String TEST_EXPIRY = "3012";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        List<String> positional = new ArrayList<>();
        Framing framing = Framing.BINARY_2;
        boolean hex = false;
        int timeout = 30_000;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--framing" -> framing = Framing.valueOf(args[++i].toUpperCase());
                case "--hex" -> hex = true;
                case "--timeout" -> timeout = Integer.parseInt(args[++i]);
                default -> positional.add(args[i]);
            }
        }
        String mode = positional.isEmpty() ? "help" : positional.get(0);
        switch (mode) {
            case "server" -> runServer(intArg(positional, 1, 8583), framing);
            case "client" -> runClient(strArg(positional, 1, "localhost"), intArg(positional, 2, 8583), framing, hex, timeout);
            case "demo" -> runDemo(framing, hex);
            default -> printUsage();
        }
    }

    private static void runServer(int port, Framing framing) throws IOException, InterruptedException {
        Iso8583Server server = new Iso8583Server(port, new IsoPackager(), framing, new SimulatedAuthorizer());
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
            } catch (IOException ignored) {
                // cerrando
            }
        }));
        System.out.println("[SERVIDOR] Presiona Ctrl+C para detener");
        Thread.currentThread().join();
    }

    private static void runClient(String host, int port, Framing framing, boolean hex, int timeout) throws IOException {
        MessageFactory factory = new MessageFactory("TERM0001", "COMERCIO0000001", "068");
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        IsoMessage lastPurchase = null;

        try (Iso8583Client client = new Iso8583Client(host, port, new IsoPackager(), framing, timeout)) {
            client.setDumpHex(hex);
            client.connect();
            while (true) {
                System.out.println();
                System.out.println("=== Menu ISO 8583 ===");
                System.out.println(" 1) Echo test (0800)");
                System.out.println(" 2) Sign on (0800)");
                System.out.println(" 3) Compra (0200)");
                System.out.println(" 4) Reverso de la ultima compra (0400)");
                System.out.println(" 0) Salir");
                System.out.print("> ");
                String option = console.readLine();
                if (option == null || option.trim().equals("0")) {
                    break;
                }
                try {
                    switch (option.trim()) {
                        case "1" -> show(client.send(factory.echo()));
                        case "2" -> show(client.send(factory.signOn()));
                        case "3" -> {
                            String pan = ask(console, "PAN", TEST_PAN);
                            String exp = ask(console, "Expiracion YYMM", TEST_EXPIRY);
                            long cents = new BigDecimal(ask(console, "Monto", "100.00"))
                                    .movePointRight(2).longValueExact();
                            lastPurchase = factory.purchase(pan, exp, cents);
                            show(client.send(lastPurchase));
                        }
                        case "4" -> {
                            if (lastPurchase == null) {
                                System.out.println("No hay compra previa para reversar");
                            } else {
                                show(client.send(factory.reversal(lastPurchase)));
                                lastPurchase = null;
                            }
                        }
                        default -> System.out.println("Opcion invalida");
                    }
                } catch (IsoException | ArithmeticException | NumberFormatException ex) {
                    System.out.println("Error en los datos: " + ex.getMessage());
                } catch (IOException ex) {
                    System.out.println("Error de comunicacion: " + ex.getMessage() + " (se reintentara la conexion)");
                    client.close();
                }
            }
        }
    }

    private static void runDemo(Framing framing, boolean hex) throws Exception {
        try (Iso8583Server server = new Iso8583Server(0, new IsoPackager(), framing, new SimulatedAuthorizer())) {
            server.start();
            MessageFactory factory = new MessageFactory("TERM0001", "COMERCIO0000001", "068");
            try (Iso8583Client client = new Iso8583Client("localhost", server.getPort(), new IsoPackager(), framing, 5_000)) {
                client.setDumpHex(hex);
                client.connect();

                title("1. Echo test");
                show(client.send(factory.echo()));

                title("2. Compra aprobada de 150.75");
                IsoMessage purchase = factory.purchase(TEST_PAN, TEST_EXPIRY, 15_075);
                show(client.send(purchase));

                title("3. Compra rechazada por monto (25,000.00)");
                show(client.send(factory.purchase(TEST_PAN, TEST_EXPIRY, 2_500_000)));

                title("4. Compra con tarjeta invalida");
                show(client.send(factory.purchase("4111111111111112", TEST_EXPIRY, 1_000)));

                title("5. Reverso de la compra aprobada");
                show(client.send(factory.reversal(purchase)));
            }
        }
    }

    private static void title(String text) {
        System.out.println();
        System.out.println("------------------------------------------------------------");
        System.out.println(text);
        System.out.println("------------------------------------------------------------");
    }

    private static void show(IsoMessage response) {
        String code = response.get(39);
        System.out.println("==> Respuesta " + response.getMti() + ": "
                + (code == null ? "sin campo 39" : code + " - " + ResponseCodes.describe(code))
                + (response.has(38) ? " | Autorizacion: " + response.get(38) : ""));
    }

    private static String ask(BufferedReader console, String label, String def) throws IOException {
        System.out.print(label + " [" + def + "]: ");
        String line = console.readLine();
        return line == null || line.isBlank() ? def : line.trim();
    }

    private static int intArg(List<String> args, int idx, int def) {
        return args.size() > idx ? Integer.parseInt(args.get(idx)) : def;
    }

    private static String strArg(List<String> args, int idx, String def) {
        return args.size() > idx ? args.get(idx) : def;
    }

    private static void printUsage() {
        System.out.println("""
                Uso:
                  java -jar iso8583-app.jar server [puerto]           Inicia el servidor (autorizador simulado)
                  java -jar iso8583-app.jar client [host] [puerto]    Cliente interactivo
                  java -jar iso8583-app.jar demo                      Servidor + cliente en el mismo proceso

                Opciones:
                  --framing BINARY_2|ASCII_4   Encabezado de longitud TCP (por defecto BINARY_2)
                  --hex                        Muestra los bytes en hexadecimal
                  --timeout ms                 Tiempo de espera del cliente (por defecto 30000)
                """);
    }
}
