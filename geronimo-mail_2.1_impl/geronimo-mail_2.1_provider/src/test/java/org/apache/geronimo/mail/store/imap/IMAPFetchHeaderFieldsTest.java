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

import java.util.Date;
import java.util.Properties;

import jakarta.mail.FetchProfile;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the FETCH command syntax generated for a FetchProfile
 * that names explicit headers.
 *
 * IMAPCommand.appendFetchProfile used to emit
 * "BODY.PEEK[HEADER.FIELDS] (Subject From)" - but per RFC 3501 the header
 * name list belongs INSIDE the section brackets:
 * "BODY.PEEK[HEADER.FIELDS (Subject From)]".  RFC-strict servers such as
 * Apache James answer BAD, which surfaced as an InvalidCommandException
 * from Folder.fetch().
 */
public class IMAPFetchHeaderFieldsTest extends AbstractProtocolTest {

    private Store connect() throws Exception {
        final Properties props = new Properties();
        props.setProperty("mail.imap.port", String.valueOf(imapConf.getListenerPort()));
        props.setProperty("mail.debug", "true");
        final Session session = Session.getInstance(props);
        final Store store = session.getStore("imap");
        store.connect("127.0.0.1", "serveruser", "serverpass");
        return store;
    }

    @Test
    @Timeout(60)
    public void testFetchWithExplicitHeaderNames() throws Exception {
        start();

        final MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        msg.setFrom(new InternetAddress("serveruser@localhost"));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress("serveruser@localhost"));
        msg.setSubject("fetch profile test");
        msg.setSentDate(new Date());
        msg.setText("fetch profile test body");

        final Store store = connect();
        try {
            final Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            inbox.appendMessages(new Message[] { msg });
            assertEquals(1, inbox.getMessageCount());

            final Message[] messages = inbox.getMessages();

            // a profile with explicitly named headers plus regular items -
            // this used to make the server answer BAD and fetch() throw.
            final FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            profile.add("Subject");
            profile.add("From");
            inbox.fetch(messages, profile);

            // the prefetched headers must be usable afterwards
            final Message fetched = messages[0];
            assertEquals("fetch profile test", fetched.getSubject());
            final String[] from = fetched.getHeader("From");
            assertNotNull(from);
            assertTrue(from.length > 0);
            assertEquals("serveruser@localhost", from[0]);

            inbox.close(false);
        } finally {
            store.close();
        }
    }

}
