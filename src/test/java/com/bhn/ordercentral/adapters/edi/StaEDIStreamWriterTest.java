package com.bhn.ordercentral.adapters.edi;


import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.junit.jupiter.api.Test;

import com.bhn.ordercentral.adapters.edi.EDIGenerator.*;

import java.io.IOException;

@SuppressWarnings("resource")
class StaEDIStreamWriterTest {

    private final String TEST_HEADER_X12 = "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~";
    EDIGenerator generator = new EDIGenerator();

    public static void write(EDIStreamWriter writer, String segmentTag, Object... elements) throws EDIStreamException {
        writer.writeStartSegment(segmentTag);

        for (Object e : elements) {
            if (e instanceof String) {
                writer.writeElement((String) e);
            } else if (e instanceof String[]) {
                writer.writeStartElement();
                for (String c : (String[]) e) {
                    writer.writeComponent(c);
                }
                writer.endElement();
            }
        }

        writer.writeEndSegment();
    }

    @Test
    void testGenerateEDI855() throws EDIStreamException, EDISchemaException, IOException {
        EDIGenerator.Order order = new EDIGenerator.Order("8048174406L",1, "86966B1999BDIGITALBCLOSEDLOOP");
        String payload = generator.generate855(order);
        System.out.println(payload);
    }




}