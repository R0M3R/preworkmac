package com.ejemplo.iso8583.app;

import com.ejemplo.iso8583.core.IsoMessage;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/** Construye los mensajes mas habituales de un terminal/adquirente. */
public class MessageFactory {

    private static final DateTimeFormatter F7 = DateTimeFormatter.ofPattern("MMddHHmmss");
    private static final DateTimeFormatter F12 = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter F13 = DateTimeFormatter.ofPattern("MMdd");

    private final AtomicInteger stan = new AtomicInteger();
    private final String terminalId;
    private final String merchantId;
    private final String currency;
    private final Clock clock;

    /**
     * @param terminalId campo 41 (8 caracteres)
     * @param merchantId campo 42 (15 caracteres)
     * @param currency   campo 49, codigo ISO 4217 numerico (p. ej. 068 = BOB, 840 = USD)
     */
    public MessageFactory(String terminalId, String merchantId, String currency) {
        this(terminalId, merchantId, currency, Clock.systemDefaultZone());
    }

    MessageFactory(String terminalId, String merchantId, String currency, Clock clock) {
        this.terminalId = terminalId;
        this.merchantId = merchantId;
        this.currency = currency;
        this.clock = clock;
    }

    /** STAN (campo 11): 000001..999999 y vuelve a empezar. */
    public String nextStan() {
        return String.format("%06d", stan.updateAndGet(v -> v >= 999_999 ? 1 : v + 1));
    }

    /** 0800 - Echo test (campo 70 = 301). Sirve para verificar que el enlace esta vivo. */
    public IsoMessage echo() {
        return network("301");
    }

    /** 0800 - Sign on (campo 70 = 001). */
    public IsoMessage signOn() {
        return network("001");
    }

    /** 0800 - Sign off (campo 70 = 002). */
    public IsoMessage signOff() {
        return network("002");
    }

    private IsoMessage network(String code) {
        ZonedDateTime utc = ZonedDateTime.now(clock).withZoneSameInstant(ZoneOffset.UTC);
        return new IsoMessage("0800")
                .set(7, utc.format(F7))
                .set(11, nextStan())
                .set(70, code);
    }

    /**
     * 0200 - Compra (financial request).
     *
     * @param pan           numero de tarjeta
     * @param expiry        fecha de expiracion YYMM
     * @param amountCents   monto en centavos (1050 = 10.50)
     */
    public IsoMessage purchase(String pan, String expiry, long amountCents) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime utc = now.withZoneSameInstant(ZoneOffset.UTC);
        String stanValue = nextStan();
        return new IsoMessage("0200")
                .set(2, pan)
                .set(3, "000000")                       // compra, cuenta por defecto
                .set(4, String.format("%012d", amountCents))
                .set(7, utc.format(F7))
                .set(11, stanValue)
                .set(12, now.format(F12))
                .set(13, now.format(F13))
                .set(14, expiry)
                .set(22, "012")                         // PAN ingresado manualmente, sin PIN
                .set(25, "00")                          // condicion normal
                .set(37, now.format(DateTimeFormatter.ofPattern("yyDDDHH")) + stanValue.substring(1))
                .set(41, terminalId)
                .set(42, merchantId)
                .set(49, currency);
    }

    /**
     * 0400 - Reverso de una transaccion previa. El campo 90 identifica la transaccion original:
     * MTI original (4) + STAN (6) + fecha/hora de transmision (10) + adquirente (11) + remitente (11).
     */
    public IsoMessage reversal(IsoMessage original) {
        ZonedDateTime utc = ZonedDateTime.now(clock).withZoneSameInstant(ZoneOffset.UTC);
        String f90 = original.getMti() + original.get(11) + original.get(7) + "0".repeat(22);
        IsoMessage msg = new IsoMessage("0400");
        for (int f : new int[] {2, 3, 4, 12, 13, 14, 22, 25, 37, 41, 42, 49}) {
            if (original.has(f)) {
                msg.set(f, original.get(f));
            }
        }
        return msg.set(7, utc.format(F7))
                .set(11, nextStan())
                .set(39, "17")                          // motivo: cancelado por el cliente
                .set(90, f90);
    }
}
