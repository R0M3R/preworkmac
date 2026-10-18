package com.ejemplo.iso8583;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.iso8583.app.MessageFactory;
import com.ejemplo.iso8583.app.SimulatedAuthorizer;
import com.ejemplo.iso8583.core.IsoMessage;
import com.ejemplo.iso8583.core.IsoPackager;
import com.ejemplo.iso8583.net.Framing;
import com.ejemplo.iso8583.net.Iso8583Client;
import com.ejemplo.iso8583.net.Iso8583Server;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ClientServerTest {

    @ParameterizedTest
    @EnumSource(Framing.class)
    void fullConversation(Framing framing) throws Exception {
        try (Iso8583Server server = new Iso8583Server(0, new IsoPackager(), framing, new SimulatedAuthorizer())) {
            server.setLogger(s -> { });
            server.start();
            MessageFactory factory = new MessageFactory("TERM0001", "COMERCIO0000001", "068");

            try (Iso8583Client client = new Iso8583Client("localhost", server.getPort(), new IsoPackager(), framing, 5_000)) {
                client.setLogger(s -> { });
                client.connect();

                IsoMessage echo = client.send(factory.echo());
                assertEquals("0810", echo.getMti());
                assertEquals("00", echo.get(39));
                assertEquals("301", echo.get(70));

                IsoMessage purchase = factory.purchase("4111111111111111", "3012", 15_075);
                IsoMessage approved = client.send(purchase);
                assertEquals("0210", approved.getMti());
                assertEquals("00", approved.get(39));
                assertEquals(purchase.get(11), approved.get(11));
                assertTrue(approved.get(38).matches("\\d{6}"));

                assertEquals("51", client.send(factory.purchase("4111111111111111", "3012", 2_500_000)).get(39));
                assertEquals("14", client.send(factory.purchase("4111111111111112", "3012", 100)).get(39));
                assertEquals("54", client.send(factory.purchase("4111111111111111", "2001", 100)).get(39));

                IsoMessage reversal = client.send(factory.reversal(purchase));
                assertEquals("0410", reversal.getMti());
                assertEquals("00", reversal.get(39));
            }
        }
    }
}
