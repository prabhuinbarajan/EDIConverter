package com.bhn.ordercentral.adapters.edi;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


//https://www.dandh.com/docs/EDI_Guides%5CCustomer%5CImplementation%20Guide%20855,%204010.pdf
// https://www.dandh.com/docs/EDI_Guides%5CCustomer%5CImplementation%20Guide%20856,%204010.pdf

public class EDIGenerator {
    public static final String BHN_INTERCHANGE_ID = "BHNETWORK";
    public static final String TARGET_INTERCHANGE_ID = "TGTDVS";
    public static final String BHN_INTERNAL_VENDOR_NUMBER = "192565";

    private EDIOutputFactory factory = EDIOutputFactory.newFactory();
    String pattern = "yyyyMMdd hhmm";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    public EDIGenerator() {
    }

    String generate855(Order order) throws EDIStreamException, EDISchemaException, IOException {
        Random r = new Random( System.currentTimeMillis() );

        int lineItemQuantity = order.lineItem.quantity;    //from order line item
        int transactionSetCount = 1;  //from order line item
        String purchaseOrderNumber = order.poNumber;  //from order
        String productIdentifier = order.lineItem.productId;  //from line item


        String transactionSetPurposeCode = "00";
        Date now = new Date();
        String[] formattedDate = simpleDateFormat.format(now).split(" ");

        String exchangeTime =  formattedDate[1]; //"1109"; //current time
        String exchangeDate = formattedDate[0]; //"20201129"; //current date
        int groupControlNumber =   r.nextInt(9999999);  //1064786;  //generate on the fly
        int interchangeControlNumber = r.nextInt(999999);  // 781803;  //generate on the fly


        ByteArrayOutputStream stream = new ByteArrayOutputStream(4096);
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        int groupCount = 0;
        writer.startInterchange();

        // Write interchange header segment
        writer.writeStartSegment("ISA")
                .writeElement("00")
                .writeElement(StringUtils.rightPad("", 10, " "))
                .writeElement("00")
                .writeElement(StringUtils.rightPad("", 10, " "))
                .writeElement("ZZ")
                .writeElement(StringUtils.rightPad(BHN_INTERCHANGE_ID, 15, " "))
                .writeElement("ZZ")
                .writeElement(StringUtils.rightPad(TARGET_INTERCHANGE_ID, 15, " "))
                .writeElement("201129")
                .writeElement(exchangeTime)
                .writeElement("U")
                .writeElement("00401")
                .writeElement(StringUtils.leftPad(String.valueOf(interchangeControlNumber), 9, "0"))
                .writeElement("0")
                .writeElement("P")
                .writeElement(">")
                .writeEndSegment();

        // Write functional group header segment
        groupCount++;
        writer.writeStartSegment("GS")
                .writeElement("PR")
                .writeElement(BHN_INTERCHANGE_ID)
                .writeElement(TARGET_INTERCHANGE_ID)
                .writeElement(exchangeDate)
                .writeElement(exchangeTime)
                .writeElement(String.valueOf(groupControlNumber))
                .writeElement("X")
                .writeElement("004010")
                .writeEndSegment();

        // Continue writing remainder of functional group

        String transactionSetIdentifierCode = "855";
        int segmentCount = 1;
        writer.writeStartSegment("ST")
                .writeElement(transactionSetIdentifierCode)
                .writeElement(StringUtils.leftPad(String.valueOf(transactionSetCount), 9, "0"))
                .writeEndSegment();

        segmentCount++;

        writer.writeStartSegment("BAK")
                .writeElement(transactionSetPurposeCode)
                .writeElement("AC")
                .writeElement(purchaseOrderNumber)
                .writeElement(exchangeDate)
                .writeEndSegment();
        segmentCount++;

        writer.writeStartSegment("REF")
                .writeElement("VR")
                .writeElement(BHN_INTERNAL_VENDOR_NUMBER)
                .writeEndSegment();
        segmentCount++;

        writer.writeStartSegment("PO1")
                .writeElement("1")
                .writeElement(String.valueOf(lineItemQuantity))
                .writeEmptyElement()        //PO103
                .writeEmptyElement()        //PO104
                .writeEmptyElement()        //PO106
                .writeElement("SK")        //PO106
                .writeElement(productIdentifier)        //Product ID
                .writeEndSegment();
        segmentCount++;

        writer.writeStartSegment("ACK")
                .writeElement("IA")
                .writeEndSegment();
        segmentCount++;

        writer.writeStartSegment("CTT")
                .writeElement(String.valueOf(transactionSetCount))
                .writeEndSegment();
        segmentCount++;

        writer.writeStartSegment("SE")
                .writeElement(String.valueOf(segmentCount))
                .writeElement(StringUtils.leftPad(String.valueOf(transactionSetCount), 9, "0"))
                .writeEndSegment();

        writer.writeStartSegment("GE")
                .writeElement(String.valueOf(transactionSetCount))
                .writeElement(String.valueOf(groupControlNumber))

                .writeEndSegment();

        writer.writeStartSegment("IEA")
                .writeElement(String.valueOf(groupCount))
                .writeElement(StringUtils.leftPad(String.valueOf(interchangeControlNumber), 9, "0"))
                .writeEndSegment();

        writer.endInterchange();
        writer.flush();
        String payload = new String(stream.toByteArray());
        stream.close();
        return new String(payload);
    }

    static class Order {
        String poNumber;
        LineItem lineItem;
        Order (String poNumber, int quantity, String productId) {
            this.lineItem = new LineItem();
            this.lineItem.quantity = quantity;
            this.lineItem.productId = productId;
            this.poNumber = poNumber;
        }
    }
    static class LineItem {
        int quantity;
        String productId;
    }
}