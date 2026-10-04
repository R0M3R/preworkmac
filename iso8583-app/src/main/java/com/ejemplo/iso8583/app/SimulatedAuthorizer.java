package com.ejemplo.iso8583.app;

import com.ejemplo.iso8583.core.IsoMessage;
import com.ejemplo.iso8583.net.MessageHandler;

import java.security.SecureRandom;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Autorizador de prueba usado por el servidor. Reemplazalo por tu propia logica
 * (consulta a base de datos, reenvio a otro host, etc.).
 *
 * Reglas:
 * <ul>
 *   <li>0800 -> 0810 con 39=00</li>
 *   <li>0100/0200 -> 39=30 si faltan campos, 14 si el PAN no pasa Luhn,
 *       54 si la tarjeta esta vencida, 51 si el monto supera {@link #LIMIT_CENTS}, 00 en otro caso</li>
 *   <li>0400/0420 -> 0410/0430 con 39=00</li>
 *   <li>Cualquier otra solicitud -> 39=12 (transaccion invalida)</li>
 * </ul>
 */
public class SimulatedAuthorizer implements MessageHandler {

    public static final long LIMIT_CENTS = 1_000_000; // 10,000.00

    private final SecureRandom random = new SecureRandom();

    @Override
    public IsoMessage handle(IsoMessage request) {
        if (!request.isRequest()) {
            return null; // nunca se responde a una respuesta
        }
        IsoMessage response = request.createResponse();
        switch (request.getMti()) {
            case "0800":
                return response.set(39, "00");
            case "0100":
            case "0200":
                return authorize(request, response);
            case "0400":
            case "0420":
                return response.set(39, "00");
            default:
                return response.set(39, "12");
        }
    }

    private IsoMessage authorize(IsoMessage req, IsoMessage resp) {
        for (int f : new int[] {2, 3, 4, 11}) {
            if (!req.has(f)) {
                return resp.set(39, "30"); // error de formato
            }
        }
        if (!luhnValid(req.get(2))) {
            return resp.set(39, "14"); // numero de tarjeta invalido
        }
        if (req.has(14) && isExpired(req.get(14))) {
            return resp.set(39, "54"); // tarjeta vencida
        }
        if (Long.parseLong(req.get(4)) > LIMIT_CENTS) {
            return resp.set(39, "51"); // fondos insuficientes
        }
        return resp.set(38, String.format("%06d", random.nextInt(1_000_000)))
                .set(39, "00");
    }

    private static boolean isExpired(String yymm) {
        try {
            YearMonth exp = YearMonth.parse(yymm, DateTimeFormatter.ofPattern("yyMM"));
            return exp.isBefore(YearMonth.now());
        } catch (RuntimeException ex) {
            return true;
        }
    }

    static boolean luhnValid(String pan) {
        int sum = 0;
        boolean dbl = false;
        for (int i = pan.length() - 1; i >= 0; i--) {
            int d = pan.charAt(i) - '0';
            if (d < 0 || d > 9) {
                return false;
            }
            if (dbl) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            dbl = !dbl;
        }
        return pan.length() >= 12 && sum % 10 == 0;
    }
}
