/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.mail.store.imap.connection;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the low level response line reader.  Mainly a regression
 * test for the resp-cr-drop defect: a bare CR followed by a non-LF byte used
 * to silently drop the look-ahead byte, corrupting the stream (and a CR CR LF
 * sequence could prevent line termination entirely).
 */
public class IMAPResponseStreamTest {

    private IMAPResponseStream streamFor(final String data) throws UnsupportedEncodingException {
        return new IMAPResponseStream(new ByteArrayInputStream(data.getBytes("ISO8859-1")));
    }

    private byte[] bytes(final String data) throws UnsupportedEncodingException {
        return data.getBytes("ISO8859-1");
    }

    @Test
    public void testPlainLineIsReadWithoutTerminator() throws Exception {
        final IMAPResponseStream stream = streamFor("* OK ready\r\n");
        assertArrayEquals(bytes("* OK ready"), stream.readData());
    }

    @Test
    public void testBareCarriageReturnDropsNoBytes() throws Exception {
        // regression: the byte after a bare CR was silently discarded
        final IMAPResponseStream stream = streamFor("a\rZb\r\n");
        assertArrayEquals(bytes("a\rZb"), stream.readData());
    }

    @Test
    public void testCarriageReturnBeforeLineTerminatorIsKept() throws Exception {
        // regression: CR CR LF used to leave the line unterminated because the
        // second CR was consumed as look-ahead and never re-examined
        final IMAPResponseStream stream = streamFor("a\r\r\nnext\r\n");
        assertArrayEquals(bytes("a\r"), stream.readData());
        assertArrayEquals(bytes("next"), stream.readData());
    }

    @Test
    public void testConsecutiveBareCarriageReturns() throws Exception {
        final IMAPResponseStream stream = streamFor("a\r\rb\r\n");
        assertArrayEquals(bytes("a\r\rb"), stream.readData());
    }

    @Test
    public void testLiteralProcessingStillWorks() throws Exception {
        // a literal marker at the end of the line continues onto the next line;
        // the literal content may contain CR and LF bytes that must be kept verbatim
        final IMAPResponseStream stream = streamFor("* 1 FETCH (BODY[] {6}\r\nX\rY\nZW)\r\n");
        assertArrayEquals(bytes("* 1 FETCH (BODY[] {6}\r\nX\rY\nZW)"), stream.readData());
    }

    @Test
    public void testMultipleLinesAreSplitCorrectly() throws Exception {
        final IMAPResponseStream stream = streamFor("first\r\nsecond\r\nthird\r\n");
        assertArrayEquals(bytes("first"), stream.readData());
        assertArrayEquals(bytes("second"), stream.readData());
        assertArrayEquals(bytes("third"), stream.readData());
    }

    @Test
    public void testReadResponseParsesTaggedResponse() throws Exception {
        final IMAPResponseStream stream = streamFor("a1 OK LOGIN completed.\r\n");
        final IMAPResponse response = stream.readResponse();
        final IMAPTaggedResponse tagged = (IMAPTaggedResponse) response;
        assertTrue(tagged.isOK());
    }
}
