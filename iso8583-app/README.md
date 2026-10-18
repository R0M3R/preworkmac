# ISO 8583 App (Java)

Aplicación Java sin dependencias externas para enviar y recibir mensajes **ISO 8583 (versión 1987)** por TCP.
Incluye:

- **Codificador/decodificador** (`IsoPackager`): MTI, bitmap primario y secundario, campos fijos, LLVAR, LLLVAR y binarios.
- **Diccionario completo de campos 2–128** (`Iso87Fields`), fácil de ajustar a la especificación de tu red.
- **Servidor TCP** multi-conexión (`Iso8583Server`) con un autorizador simulado.
- **Cliente TCP** (`Iso8583Client`) que empareja respuestas por STAN (campo 11).
- **Mensajes listos**: echo/sign-on (0800), compra (0200) y reverso (0400).

## Requisitos

- Java 17 o superior
- Maven 3.8+

## Compilar y probar

```bash
cd iso8583-app
mvn package          # compila, corre los tests y genera target/iso8583-app-1.0.0.jar
```

## Uso

```bash
# Demo: levanta servidor y cliente en el mismo proceso y ejecuta varias transacciones
java -jar target/iso8583-app-1.0.0.jar demo --hex

# Terminal 1: servidor en el puerto 8583
java -jar target/iso8583-app-1.0.0.jar server 8583

# Terminal 2: cliente interactivo (menú: echo, sign on, compra, reverso)
java -jar target/iso8583-app-1.0.0.jar client localhost 8583
```

Opciones:

| Opción | Descripción |
|---|---|
| `--framing BINARY_2` | Encabezado TCP de 2 bytes binarios con la longitud (por defecto) |
| `--framing ASCII_4` | Encabezado TCP de 4 dígitos ASCII, p. ej. `0123` |
| `--hex` | Imprime los bytes enviados/recibidos en hexadecimal |
| `--timeout ms` | Tiempo de espera del cliente (por defecto 30000) |

## Formato en el cable

```
[encabezado longitud][MTI 4 ASCII][bitmap 8 o 16 bytes][campos...]
```

- Numéricos fijos: se rellenan con ceros a la izquierda.
- Alfanuméricos fijos: se rellenan con espacios a la derecha.
- LLVAR / LLLVAR: prefijo de 2 / 3 dígitos ASCII.
- Binarios (52, 55, 64, 96, 128): bytes crudos; en `IsoMessage` se manejan como texto hexadecimal.

## Usarlo desde tu código

```java
IsoPackager packager = new IsoPackager();

try (Iso8583Client client = new Iso8583Client("10.0.0.5", 8583, packager, Framing.BINARY_2, 30_000)) {
    client.connect();

    IsoMessage request = new IsoMessage("0200")
            .set(2, "4111111111111111")
            .set(3, "000000")
            .set(4, "000000015075")      // 150.75
            .set(11, "000123")
            .set(41, "TERM0001")
            .set(49, "068");

    IsoMessage response = client.send(request);
    System.out.println("Codigo de respuesta: " + response.get(39));
}
```

Servidor con tu propia lógica:

```java
MessageHandler handler = request -> {
    IsoMessage response = request.createResponse();   // 0200 -> 0210, copia campos comunes
    response.set(39, "00");
    return response;
};
new Iso8583Server(8583, new IsoPackager(), Framing.BINARY_2, handler).start();
```

## Reglas del autorizador simulado

| Caso | Campo 39 |
|---|---|
| Echo / sign on (0800) | `00` |
| Faltan campos 2, 3, 4 u 11 | `30` Error de formato |
| PAN no pasa Luhn | `14` Tarjeta inválida |
| Tarjeta vencida (campo 14) | `54` |
| Monto > 10,000.00 | `51` Fondos insuficientes |
| Otro caso | `00` + código de autorización (campo 38) |
| Reverso (0400/0420) | `00` |

## Adaptarlo a tu red / procesador

Cada procesador publica su propia especificación. Lo habitual es tener que ajustar:

1. **Encabezado TCP**: `BINARY_2`, `ASCII_4`, o agregar un *TPDU*/header propio en `Framing`.
2. **Definición de campos**: copia `Iso87Fields.definitions()` en un `TreeMap`, reemplaza los campos
   que difieran y pásalo a `new IsoPackager(misCampos, StandardCharsets.US_ASCII)`.
3. **Codificación**: algunas redes usan BCD o EBCDIC en lugar de ASCII; habría que extender `IsoPackager`.
4. **Seguridad**: PIN block (campo 52) y MAC (64/128) requieren llaves y un HSM; esta app solo los transporta.
5. **TLS**: si la conexión debe ir cifrada, reemplaza `Socket`/`ServerSocket` por `SSLSocketFactory`/`SSLServerSocketFactory`.

## Estructura

```
src/main/java/com/ejemplo/iso8583/
├── core/   IsoMessage, IsoPackager, Iso87Fields, FieldDefinition, ...
├── net/    Iso8583Client, Iso8583Server, Framing, MessageHandler
└── app/    Main (CLI), MessageFactory, SimulatedAuthorizer, ResponseCodes
```
