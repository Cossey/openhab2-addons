package org.openhab.binding.broadlink.internal;

import org.eclipse.smarthome.core.util.HexUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BroadlinkProtocolTest {

    byte[] mac = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
    };

    byte[] deviceId = {
            0x10, 0x11, 0x12, 0x13
    };

    byte[] iv = HexUtils.hexToBytes("562e17996d093d28ddb3ba695a2e6f58");

    byte[] deviceKey = HexUtils.hexToBytes("097628343fe99e23765c1513accf8b02");


    @Test
    public void canBuildMessageWithCorrectChecksums() {
        byte[] payload = {};
        byte[] result = BroadlinkProtocol.buildMessage(
                (byte) 0x0, payload, 0, mac,
                deviceId, iv,
                deviceKey, 1234
        );

        assertEquals(56, result.length);

        // bytes 0x34 and 0x35 contain the payload checksum,
        // which given we have an empty payload, should be the initial
        // 0xBEAF
        int payloadChecksum = ((result[0x35] & 0xff) << 8) + (result[0x34] & 0xff);
        assertEquals(0xbeaf, payloadChecksum);

        // bytes 0x20 and 0x21 contain the overall checksum
        int overallChecksum = ((result[0x21] & 0xff) << 8) + (result[0x20] & 0xff);
        assertEquals(0xc549, overallChecksum);
    }
}
