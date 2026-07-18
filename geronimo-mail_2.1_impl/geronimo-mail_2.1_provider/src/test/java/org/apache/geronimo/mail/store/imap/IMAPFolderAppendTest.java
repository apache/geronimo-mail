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
package org.apache.geronimo.mail.store.imap;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.apache.geronimo.mail.testserver.AbstractProtocolTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test for IMAP APPEND against Apache James.
 *
 * IMAPCommand.writeTo used to write the command segment announcing a literal
 * ("APPEND ... {n}") into the buffered output stream and then block waiting
 * for the server's continuation response WITHOUT flushing the stream: the
 * announcement never reached the server, so client and server waited on each
 * other forever.  The timeout on these tests guards against a regression of
 * that deadlock.  A second defect fixed alongside: the INTERNALDATE sent with
 * APPEND shortened single-digit days ("8-Dec-...") where RFC 3501 requires
 * space padding (" 8-Dec-..."), which strict servers reject with BAD.
 */
public class IMAPFolderAppendTest extends AbstractProtocolTest {

    private Store connect() throws Exception {
        final Properties props = new Properties();
        props.setProperty("mail.imap.port", String.valueOf(imapConf.getListenerPort()));
        props.setProperty("mail.debug", "true");
        final Session session = Session.getInstance(props);
        final Store store = session.getStore("imap");
        store.connect("127.0.0.1", "serveruser", "serverpass");
        return store;
    }

    private MimeMessage message(final String subject, final Date sentDate) throws Exception {
        final MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        msg.setFrom(new InternetAddress("serveruser@localhost"));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress("serveruser@localhost"));
        msg.setSubject(subject);
        msg.setSentDate(sentDate);
        msg.setText("append test body");
        return msg;
    }

    @Test
    @Timeout(60)
    public void testAppendMessages() throws Exception {
        start();

        final Store store = connect();
        try {
            final Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            assertEquals(0, inbox.getMessageCount());

            inbox.appendMessages(new Message[] { message("appended", new Date()) });

            assertEquals(1, inbox.getMessageCount());
            assertEquals("appended", inbox.getMessage(1).getSubject());
            inbox.close(false);
        } finally {
            store.close();
        }
    }

    @Test
    @Timeout(60)
    public void testAppendMessageWithSingleDigitDay() throws Exception {
        start();

        // a sent date on a single-digit day exercises the RFC 3501
        // space-padded INTERNALDATE form
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2025, Calendar.DECEMBER, 8, 12, 0, 0);

        final Store store = connect();
        try {
            final Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            inbox.appendMessages(new Message[] { message("single digit day", cal.getTime()) });

            assertEquals(1, inbox.getMessageCount());
            assertEquals("single digit day", inbox.getMessage(1).getSubject());
            inbox.close(false);
        } finally {
            store.close();
        }
    }

}
