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

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public class InternetHeadersTest extends TestCase {
    private InternetHeaders headers;

    public void testLoadSingleHeader() throws MessagingException {
        final String stream = "content-type: text/plain\r\n\r\n";
        headers.load(new ByteArrayInputStream(stream.getBytes()));
        final String[] header = headers.getHeader("content-type");
        assertNotNull(header);
        assertEquals("text/plain", header[0]);
    }

    @Override
    protected void setUp() throws Exception {
        headers = new InternetHeaders();
    }


    public void testReturnPathHeaderIgnored() throws MessagingException {
        headers.addHeader("Return-Path", "first");
        headers.addHeader("Return-Path", "second");
    }

    public void testReceivedHeaderIgnored() throws MessagingException {
        headers.addHeader("Received", "first");
        headers.addHeader("Received", "second");
    }

    public void testOtherHeaderNotDuplicated() throws MessagingException {
        headers.addHeader("Other", "first");
    }

    public void testActuallyDuplicatedHeader() throws MessagingException {
        headers.addHeader("Other", "first");
        try {
            headers.addHeader("Other", "second");
            fail("No exception thrown");
        } catch (IllegalStateException e) {
            assertEquals("InternetHeaders cannot contain more than one value for header: Other", e.getMessage());
        }
    }
}
