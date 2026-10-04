package com.ejemplo.iso8583.core;

import static com.ejemplo.iso8583.core.FieldDefinition.an;
import static com.ejemplo.iso8583.core.FieldDefinition.b;
import static com.ejemplo.iso8583.core.FieldDefinition.lllvar;
import static com.ejemplo.iso8583.core.FieldDefinition.lllvarB;
import static com.ejemplo.iso8583.core.FieldDefinition.llvar;
import static com.ejemplo.iso8583.core.FieldDefinition.llvarN;
import static com.ejemplo.iso8583.core.FieldDefinition.n;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Diccionario de campos de ISO 8583:1987.
 *
 * Cada red (Visa, Mastercard, switches locales, etc.) publica su propia variante;
 * si tu contraparte usa otra definicion para algun campo, crea una copia de este
 * mapa y reemplaza ese campo antes de construir el {@link IsoPackager}.
 */
public final class Iso87Fields {

    private Iso87Fields() {
    }

    public static Map<Integer, FieldDefinition> definitions() {
        Map<Integer, FieldDefinition> m = new TreeMap<>();
        put(m, llvarN(2, "Numero de cuenta primario (PAN)", 19));
        put(m, n(3, "Codigo de procesamiento", 6));
        put(m, n(4, "Monto de la transaccion", 12));
        put(m, n(5, "Monto de liquidacion", 12));
        put(m, n(6, "Monto de facturacion al tarjetahabiente", 12));
        put(m, n(7, "Fecha y hora de transmision (MMDDhhmmss)", 10));
        put(m, n(8, "Comision de facturacion al tarjetahabiente", 8));
        put(m, n(9, "Tasa de conversion de liquidacion", 8));
        put(m, n(10, "Tasa de conversion de facturacion", 8));
        put(m, n(11, "Numero de auditoria del sistema (STAN)", 6));
        put(m, n(12, "Hora local (hhmmss)", 6));
        put(m, n(13, "Fecha local (MMDD)", 4));
        put(m, n(14, "Fecha de expiracion (YYMM)", 4));
        put(m, n(15, "Fecha de liquidacion", 4));
        put(m, n(16, "Fecha de conversion", 4));
        put(m, n(17, "Fecha de captura", 4));
        put(m, n(18, "Tipo de comercio (MCC)", 4));
        put(m, n(19, "Codigo de pais de la institucion adquirente", 3));
        put(m, n(20, "Codigo de pais del PAN extendido", 3));
        put(m, n(21, "Codigo de pais de la institucion remitente", 3));
        put(m, n(22, "Modo de entrada POS", 3));
        put(m, n(23, "Numero de secuencia de tarjeta", 3));
        put(m, n(24, "Network International Identifier (NII)", 3));
        put(m, n(25, "Codigo de condicion POS", 2));
        put(m, n(26, "Codigo de captura de PIN POS", 2));
        put(m, n(27, "Longitud de respuesta de autorizacion", 1));
        put(m, an(28, "Comision de transaccion", 9));
        put(m, an(29, "Comision de liquidacion", 9));
        put(m, an(30, "Comision de procesamiento de transaccion", 9));
        put(m, an(31, "Comision de procesamiento de liquidacion", 9));
        put(m, llvarN(32, "Codigo de institucion adquirente", 11));
        put(m, llvarN(33, "Codigo de institucion remitente", 11));
        put(m, llvar(34, "PAN extendido", 28));
        put(m, llvar(35, "Datos de Track 2", 37));
        put(m, lllvar(36, "Datos de Track 3", 104));
        put(m, an(37, "Numero de referencia de recuperacion (RRN)", 12));
        put(m, an(38, "Codigo de autorizacion", 6));
        put(m, an(39, "Codigo de respuesta", 2));
        put(m, an(40, "Codigo de restriccion de servicio", 3));
        put(m, an(41, "Identificador de terminal", 8));
        put(m, an(42, "Identificador del comercio", 15));
        put(m, an(43, "Nombre y ubicacion del comercio", 40));
        put(m, llvar(44, "Datos adicionales de respuesta", 25));
        put(m, llvar(45, "Datos de Track 1", 76));
        put(m, lllvar(46, "Datos adicionales - ISO", 999));
        put(m, lllvar(47, "Datos adicionales - nacional", 999));
        put(m, lllvar(48, "Datos adicionales - privado", 999));
        put(m, an(49, "Codigo de moneda de la transaccion", 3));
        put(m, an(50, "Codigo de moneda de liquidacion", 3));
        put(m, an(51, "Codigo de moneda de facturacion", 3));
        put(m, b(52, "Bloque de PIN", 8));
        put(m, n(53, "Informacion de control de seguridad", 16));
        put(m, lllvar(54, "Montos adicionales", 120));
        put(m, lllvarB(55, "Datos ICC (EMV)", 999));
        put(m, lllvar(56, "Reservado ISO", 999));
        put(m, lllvar(57, "Reservado nacional", 999));
        put(m, lllvar(58, "Reservado nacional", 999));
        put(m, lllvar(59, "Reservado nacional", 999));
        put(m, lllvar(60, "Reservado privado", 999));
        put(m, lllvar(61, "Reservado privado", 999));
        put(m, lllvar(62, "Reservado privado", 999));
        put(m, lllvar(63, "Reservado privado", 999));
        put(m, b(64, "Codigo de autenticacion de mensaje (MAC)", 8));
        // El campo 65 es el "bitmap terciario" y practicamente no se usa.
        put(m, n(66, "Codigo de liquidacion", 1));
        put(m, n(67, "Codigo de pago extendido", 2));
        put(m, n(68, "Codigo de pais de la institucion receptora", 3));
        put(m, n(69, "Codigo de pais de la institucion de liquidacion", 3));
        put(m, n(70, "Codigo de gestion de red", 3));
        put(m, n(71, "Numero de mensaje", 4));
        put(m, n(72, "Ultimo numero de mensaje", 4));
        put(m, n(73, "Fecha de accion (YYMMDD)", 6));
        put(m, n(74, "Numero de creditos", 10));
        put(m, n(75, "Numero de reversos de creditos", 10));
        put(m, n(76, "Numero de debitos", 10));
        put(m, n(77, "Numero de reversos de debitos", 10));
        put(m, n(78, "Numero de transferencias", 10));
        put(m, n(79, "Numero de reversos de transferencias", 10));
        put(m, n(80, "Numero de consultas", 10));
        put(m, n(81, "Numero de autorizaciones", 10));
        put(m, n(82, "Comision de procesamiento de creditos", 12));
        put(m, n(83, "Comision de transaccion de creditos", 12));
        put(m, n(84, "Comision de procesamiento de debitos", 12));
        put(m, n(85, "Comision de transaccion de debitos", 12));
        put(m, n(86, "Monto de creditos", 16));
        put(m, n(87, "Monto de reversos de creditos", 16));
        put(m, n(88, "Monto de debitos", 16));
        put(m, n(89, "Monto de reversos de debitos", 16));
        put(m, n(90, "Elementos de datos originales", 42));
        put(m, an(91, "Codigo de actualizacion de archivo", 1));
        put(m, an(92, "Codigo de seguridad de archivo", 2));
        put(m, an(93, "Indicador de respuesta", 5));
        put(m, an(94, "Indicador de servicio", 7));
        put(m, an(95, "Montos de reemplazo", 42));
        put(m, b(96, "Codigo de seguridad del mensaje", 8));
        put(m, an(97, "Monto neto de liquidacion", 17));
        put(m, an(98, "Beneficiario", 25));
        put(m, llvarN(99, "Codigo de institucion de liquidacion", 11));
        put(m, llvarN(100, "Codigo de institucion receptora", 11));
        put(m, llvar(101, "Nombre de archivo", 17));
        put(m, llvar(102, "Identificacion de cuenta 1", 28));
        put(m, llvar(103, "Identificacion de cuenta 2", 28));
        put(m, lllvar(104, "Descripcion de la transaccion", 100));
        for (int i = 105; i <= 111; i++) {
            put(m, lllvar(i, "Reservado ISO", 999));
        }
        for (int i = 112; i <= 119; i++) {
            put(m, lllvar(i, "Reservado nacional", 999));
        }
        for (int i = 120; i <= 127; i++) {
            put(m, lllvar(i, "Reservado privado", 999));
        }
        put(m, b(128, "Codigo de autenticacion de mensaje (MAC)", 8));
        return Collections.unmodifiableMap(m);
    }

    private static void put(Map<Integer, FieldDefinition> m, FieldDefinition def) {
        m.put(def.number(), def);
    }
}
