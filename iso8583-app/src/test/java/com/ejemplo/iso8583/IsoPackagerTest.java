package com.ejemplo.iso8583;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ejemplo.iso8583.core.IsoException;
import com.ejemplo.iso8583.core.IsoMessage;
import com.ejemplo.iso8583.core.IsoPackager;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class IsoPackagerTest {

    private final IsoPackager packager = new IsoPackager();

    @Test
    void packsEchoWithSecondaryBitmap() {
        IsoMessage msg = new IsoMessage("0800")
                .set(7, "1004123000")
                .set(11, "000001")
                .set(70, "301");

        byte[] packed = packager.pack(msg);

        // MTI + bitmap primario (bits 1, 7, 11) + bitmap secundario (bit 70) + campos
        String expectedHex = HexFormat.of().formatHex("0800".getBytes(StandardCharsets.US_ASCII))
                + "8220000000000000" + "0400000000000000"
                + HexFormat.of().formatHex("1004123000000001301".getBytes(StandardCharsets.US_ASCII));
        assertEquals(expectedHex.toUpperCase(), HexFormat.of().withUpperCase().formatHex(packed));

        IsoMessage back = packager.unpack(packed);
        assertEquals(msg.getMti(), back.getMti());
        assertEquals(msg.getFields(), back.getFields());
    }

    @Test
    void roundTripsVariableAndBinaryFields() {
        IsoMessage msg = new IsoMessage("0200")
                .set(2, "4111111111111111")
                .set(3, "000000")
                .set(4, "000000015075")
                .set(11, "000123")
                .set(35, "4111111111111111=3012101")
                .set(41, "TERM0001")
                .set(52, "0123456789ABCDEF")
                .set(55, "9F2608AABBCCDDEEFF0011");

        byte[] packed = packager.pack(msg);
        String text = new String(packed, StandardCharsets.ISO_8859_1);

        assertEquals("0200", text.substring(0, 4));
        // Despues del bitmap, el campo 2 lleva prefijo LL "16"
        assertEquals("164111111111111111", text.substring(12, 30));

        IsoMessage back = packager.unpack(packed);
        assertEquals(msg.getFields(), back.getFields());
    }

    @Test
    void padsFixedFields() {
        IsoMessage msg = new IsoMessage("0200").set(4, "1050").set(41, "T1");
        IsoMessage back = packager.unpack(packager.pack(msg));
        assertEquals("000000001050", back.get(4));
        assertEquals("T1      ", back.get(41));
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IsoException.class, () -> packager.pack(new IsoMessage("0200").set(4, "12A")));
        assertThrows(IsoException.class, () -> packager.pack(new IsoMessage("0200").set(4, "1234567890123")));
        assertThrows(IsoException.class, () -> packager.pack(new IsoMessage("0200").set(2, "1".repeat(20))));
        assertThrows(IsoException.class, () -> new IsoMessage("02A0"));
    }

    @Test
    void rejectsTruncatedMessages() {
        byte[] packed = packager.pack(new IsoMessage("0800").set(11, "000001").set(70, "301"));
        assertThrows(IsoException.class, () -> packager.unpack(Arrays.copyOf(packed, packed.length - 2)));
        byte[] extra = Arrays.copyOf(packed, packed.length + 1);
        assertThrows(IsoException.class, () -> packager.unpack(extra));
    }

    @Test
    void createsResponseMti() {
        IsoMessage req = new IsoMessage("0200").set(11, "000007").set(4, "000000000100");
        IsoMessage resp = req.createResponse();
        assertEquals("0210", resp.getMti());
        assertEquals("000007", resp.get(11));
        assertArrayEquals(new Object[] {4, 11}, resp.getFields().keySet().toArray());
    }
}
