/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jakarta.mail.internet;

import java.io.ByteArrayInputStream;

import jakarta.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * @version $Rev$ $Date$
 */
public class InternetHeadersTest {
    private InternetHeaders headers;

    @Test
    public void testLoadSingleHeader() throws MessagingException {
        final String stream = "content-type: text/plain\r\n\r\n";
        headers.load(new ByteArrayInputStream(stream.getBytes()));
        final String[] header = headers.getHeader("content-type");
        assertNotNull(header);
        assertEquals("text/plain", header[0]);
    }

    @BeforeEach
    public void setUp() throws Exception {
        headers = new InternetHeaders();
    }


    @Test
    public void testReturnPathHeaderIgnored() throws MessagingException {
        headers.addHeader("Return-Path", "first");
        headers.addHeader("Return-Path", "second");
    }

    @Test
    public void testReceivedHeaderIgnored() throws MessagingException {
        headers.addHeader("Received", "first");
        headers.addHeader("Received", "second");
    }

    @Test
    public void testOtherHeaderNotDuplicated() throws MessagingException {
        headers.addHeader("Other", "first");
    }

    @Test
    public void testActuallyDuplicatedHeader() throws MessagingException {
        headers.addHeader("Subject", "first");
        try {
            headers.addHeader("Subject", "second");
            fail("No exception thrown");
        } catch (IllegalStateException e) {
            assertEquals("InternetHeaders cannot contain more than one value for header: Subject", e.getMessage());
        }
    }

    /**
     * The single-value policy covers the fields RFC 5322 section 3.6 and RFC 2045 restrict to one
     * occurrence, and it is case-insensitive.  See GERONIMO-6909.
     */
    @Test
    public void testDuplicateSingletonHeaderIsRejectedRegardlessOfCase() throws MessagingException {
        headers.addHeader("Content-Type", "text/plain");
        try {
            headers.addHeader("content-type", "text/html");
            fail("No exception thrown");
        } catch (IllegalStateException e) {
            assertEquals("InternetHeaders cannot contain more than one value for header: content-type", e.getMessage());
        }
    }

    /**
     * Headers that are not singletons may be added more than once - RFC 5322 section 3.6.5 marks
     * Comments and Keywords as unlimited, and application-defined headers are unconstrained.
     * See GERONIMO-6909.
     */
    @Test
    public void testRepeatableHeadersMayBeAddedMoreThanOnce() throws MessagingException {
        headers.addHeader("X-Trace-Id", "abc");
        headers.addHeader("X-Trace-Id", "def");
        headers.addHeader("Comments", "first remark");
        headers.addHeader("Comments", "second remark");

        assertArrayEquals(new String[]{"abc", "def"}, headers.getHeader("X-Trace-Id"));
        assertArrayEquals(new String[]{"first remark", "second remark"}, headers.getHeader("Comments"));
    }

    /**
     * The single-value policy guards what we emit; it must never reject a message we are reading.
     * Repeated headers such as Authentication-Results (RFC 8601), the ARC-* set (RFC 8617) and
     * DKIM-Signature (RFC 6376) are legal and routine on inbound mail.  See GERONIMO-6908.
     */
    @Test
    public void testLoadKeepsLegallyRepeatedHeaders() throws MessagingException {
        final String stream =
                "Authentication-Results: mx.google.com; dkim=pass\r\n" +
                "Authentication-Results: mx.example.com; spf=pass\r\n" +
                "ARC-Seal: i=1; a=rsa-sha256\r\n" +
                "ARC-Seal: i=2; a=rsa-sha256\r\n" +
                "Subject: a subject\r\n" +
                "\r\n";
        headers.load(new ByteArrayInputStream(stream.getBytes()));

        assertArrayEquals(new String[]{"mx.google.com; dkim=pass", "mx.example.com; spf=pass"},
                headers.getHeader("Authentication-Results"));
        assertArrayEquals(new String[]{"i=1; a=rsa-sha256", "i=2; a=rsa-sha256"},
                headers.getHeader("ARC-Seal"));
        assertArrayEquals(new String[]{"a subject"}, headers.getHeader("Subject"));
    }

    /**
     * Loading must not leave the single-value policy switched off for headers added afterwards.
     */
    @Test
    public void testPolicyStillAppliesAfterLoad() throws MessagingException {
        final String stream = "Authentication-Results: a\r\nAuthentication-Results: b\r\n\r\n";
        headers.load(new ByteArrayInputStream(stream.getBytes()));

        headers.addHeader("Subject", "first");
        try {
            headers.addHeader("Subject", "second");
            fail("No exception thrown");
        } catch (IllegalStateException e) {
            assertEquals("InternetHeaders cannot contain more than one value for header: Subject", e.getMessage());
        }
    }
}
