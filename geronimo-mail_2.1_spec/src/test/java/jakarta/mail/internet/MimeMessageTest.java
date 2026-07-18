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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import jakarta.activation.CommandMap;
import jakarta.activation.MailcapCommandMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;

import jakarta.mail.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @version $Rev$ $Date$
 */
public class MimeMessageTest {
    private CommandMap defaultMap;
    private Session session;

    @Test
    public void testNoDuplicateTo() throws MessagingException, IOException {
        final InternetAddress[] addresses = new InternetAddress[]{
                new InternetAddress("test1ofaveryveryverylongemailaddressover71charwhichseemscrazyatfirstglance@example.com"),
                new InternetAddress("test2@example.com"),
                new InternetAddress("test3@example.com")
        };
        {
            final MimeMessage msg = new MimeMessage(session);
            msg.setContent("Hello World", "text/plain");
            msg.setRecipients(Message.RecipientType.TO, addresses);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            msg.writeTo(out);

            final String textMessage = new String(out.toByteArray());
            assertTrue(textMessage.contains(
                    "To: \r\n" +
                    "  test1ofaveryveryverylongemailaddressover71charwhichseemscrazyatfirstglance@example.com,\r\n" +
                    "  test2@example.com, test3@example.com"), textMessage);

        }
        {
            final String actual = InternetAddress.toString(addresses, MimeMessage.RecipientType.TO.toString().length() + 2);

            assertEquals("\r\n" +
                    "  test1ofaveryveryverylongemailaddressover71charwhichseemscrazyatfirstglance@example.com,\r\n" +
                    "  test2@example.com, test3@example.com", actual);
        }
    }

    @Test
    public void testWriteTo() throws MessagingException, IOException {
        final MimeMessage msg = new MimeMessage(session);
        msg.setSender(new InternetAddress("foo"));
        msg.setHeader("foo", "bar");
        final MimeMultipart mp = new MimeMultipart();
        final MimeBodyPart part1 = new MimeBodyPart();
        part1.setHeader("foo", "bar");
        part1.setContent("Hello World", "text/plain");
        mp.addBodyPart(part1);
        final MimeBodyPart part2 = new MimeBodyPart();
        part2.setContent("Hello Again", "text/plain");
        mp.addBodyPart(part2);
        msg.setContent(mp);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out);

        final InputStream in = new ByteArrayInputStream(out.toByteArray());

        MimeMessage newMessage = new MimeMessage(session, in);

        assertEquals(((InternetAddress)newMessage.getSender()).getAddress(), "foo");

        final String[] headers = newMessage.getHeader("foo");
        assertTrue(headers.length == 1);
        assertEquals(headers[0], "bar");


        newMessage = new MimeMessage(msg);

        assertEquals(((InternetAddress)newMessage.getSender()).getAddress(), "foo");
        assertEquals(newMessage.getHeader("foo")[0], "bar");
    }


    @Test
    public void testFrom() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);

        final InternetAddress dev = new InternetAddress("geronimo-dev@apache.org");
        final InternetAddress user = new InternetAddress("geronimo-user@apache.org");

        msg.setSender(dev);

        Address[] from = msg.getFrom();
        assertTrue(from.length == 1);
        assertEquals(from[0], dev);

        msg.setFrom(user);
        from = msg.getFrom();
        assertTrue(from.length == 1);
        assertEquals(from[0], user);

        msg.addFrom(new Address[] { dev });
        from = msg.getFrom();
        assertTrue(from.length == 2);
        assertEquals(from[0], user);
        assertEquals(from[1], dev);

        msg.setFrom();
        final InternetAddress local = InternetAddress.getLocalAddress(session);
        from = msg.getFrom();

        assertTrue(from.length == 1);
        assertEquals(local, from[0]);

        msg.setFrom((Address) null);
        from = msg.getFrom();

        assertTrue(from.length == 1);
        assertEquals(dev, from[0]);

        msg.setSender(null);
        from = msg.getFrom();
        assertNull(from);
    }


    @Test
    public void testSender() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);

        final InternetAddress dev = new InternetAddress("geronimo-dev@apache.org");
        final InternetAddress user = new InternetAddress("geronimo-user@apache.org");

        msg.setSender(dev);

        final Address[] from = msg.getFrom();
        assertTrue(from.length == 1);
        assertEquals(from[0], dev);

        assertEquals(msg.getSender(), dev);

        msg.setSender(null);
        assertNull(msg.getSender());
    }

    @Test
    public void testJavaMail15GetSession() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);
        assertTrue(session == msg.getSession());
        
        final MimeMessage msg2 = new MimeMessage((Session) null);
        assertTrue(null == msg2.getSession());
    }

    @Test
    public void testJava15From() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);
        
        final InternetAddress dev = new InternetAddress("geronimo-dev@apache.org");
        final InternetAddress user = new InternetAddress("geronimo-user@apache.org");

        
        msg.setFrom("geronimo-dev@apache.org,geronimo-user@apache.org");

        Address[] from = msg.getFrom();
        assertTrue(from.length == 2);
        assertEquals(from[0], dev);
        assertEquals(from[1], user);
        
        msg.setFrom("test@apache.org");
        
        from = msg.getFrom();
        assertTrue(from.length == 1);
        assertEquals(from[0], new InternetAddress("test@apache.org"));
    }

    @Test
    public void testGetAllRecipients() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);

        final InternetAddress dev = new InternetAddress("geronimo-dev@apache.org");
        final InternetAddress user = new InternetAddress("geronimo-user@apache.org");
        final InternetAddress user1 = new InternetAddress("geronimo-user1@apache.org");
        final InternetAddress user2 = new InternetAddress("geronimo-user2@apache.org");
        final NewsAddress group = new NewsAddress("comp.lang.rexx");

        Address[] recipients = msg.getAllRecipients();
        assertNull(recipients);

        msg.setRecipients(Message.RecipientType.TO, new Address[] { dev });

        recipients = msg.getAllRecipients();
        assertTrue(recipients.length == 1);
        assertEquals(recipients[0], dev);

        msg.addRecipients(Message.RecipientType.BCC, new Address[] { user });

        recipients = msg.getAllRecipients();
        assertTrue(recipients.length == 2);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);

        msg.addRecipients(Message.RecipientType.CC, new Address[] { user1, user2} );

        recipients = msg.getAllRecipients();
        assertTrue(recipients.length == 4);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user1);
        assertEquals(recipients[2], user2);
        assertEquals(recipients[3], user);


        msg.addRecipients(MimeMessage.RecipientType.NEWSGROUPS, new Address[] { group } );

        recipients = msg.getAllRecipients();
        assertTrue(recipients.length == 5);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user1);
        assertEquals(recipients[2], user2);
        assertEquals(recipients[3], user);
        assertEquals(recipients[4], group);

        msg.setRecipients(Message.RecipientType.CC, (String)null);

        recipients = msg.getAllRecipients();

        assertTrue(recipients.length == 3);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);
        assertEquals(recipients[2], group);
    }

    @Test
    public void testGetRecipients() throws MessagingException {
        doRecipientTest(Message.RecipientType.TO);
        doRecipientTest(Message.RecipientType.CC);
        doRecipientTest(Message.RecipientType.BCC);
        doNewsgroupRecipientTest(MimeMessage.RecipientType.NEWSGROUPS);
    }

    private void doRecipientTest(final Message.RecipientType type) throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);

        final InternetAddress dev = new InternetAddress("geronimo-dev@apache.org");
        final InternetAddress user = new InternetAddress("geronimo-user@apache.org");

        Address[] recipients = msg.getRecipients(type);
        assertNull(recipients);

        msg.setRecipients(type, "geronimo-dev@apache.org");
        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 1);
        assertEquals(recipients[0], dev);

        msg.addRecipients(type, "geronimo-user@apache.org");

        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 2);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);

        msg.setRecipients(type, (String)null);

        recipients = msg.getRecipients(type);
        assertNull(recipients);

        msg.setRecipients(type, new Address[] { dev });
        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 1);
        assertEquals(recipients[0], dev);

        msg.addRecipients(type, new Address[] { user });

        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 2);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);

        msg.setRecipients(type, (Address[])null);

        recipients = msg.getRecipients(type);
        assertNull(recipients);

        msg.setRecipients(type, new Address[] { dev, user });

        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 2);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);
    }


    private void doNewsgroupRecipientTest(final Message.RecipientType type) throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);

        final Address dev = new NewsAddress("geronimo-dev");
        final Address user = new NewsAddress("geronimo-user");

        Address[] recipients = msg.getRecipients(type);
        assertNull(recipients);

        msg.setRecipients(type, "geronimo-dev");
        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 1);
        assertEquals(recipients[0], dev);

        msg.addRecipients(type, "geronimo-user");

        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 2);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);

        msg.setRecipients(type, (String)null);

        recipients = msg.getRecipients(type);
        assertNull(recipients);

        msg.setRecipients(type, new Address[] { dev });
        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 1);
        assertEquals(recipients[0], dev);

        msg.addRecipients(type, new Address[] { user });

        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 2);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);

        msg.setRecipients(type, (Address[])null);

        recipients = msg.getRecipients(type);
        assertNull(recipients);

        msg.setRecipients(type, new Address[] { dev, user });

        recipients = msg.getRecipients(type);
        assertTrue(recipients.length == 2);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);
    }

    @Test
    public void testReplyTo() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);

        final InternetAddress dev = new InternetAddress("geronimo-dev@apache.org");
        final InternetAddress user = new InternetAddress("geronimo-user@apache.org");

        msg.setReplyTo(new Address[] { dev });

        Address[] recipients = msg.getReplyTo();
        assertTrue(recipients.length == 1);
        assertEquals(recipients[0], dev);

        msg.setReplyTo(new Address[] { dev, user });

        recipients = msg.getReplyTo();
        assertTrue(recipients.length == 2);
        assertEquals(recipients[0], dev);
        assertEquals(recipients[1], user);

        msg.setReplyTo(null);

        recipients = msg.getReplyTo();
        assertNull(recipients);
    }

    @Test
    public void testJavaMail15Reply() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);
        final InternetAddress dev = new InternetAddress("geronimo-dev@apache.org");
        
        msg.setFrom("test@apache.org");
        msg.setRecipient(Message.RecipientType.TO, dev);

        final Message replyMsg = msg.reply(true, true);
        assertNotNull(replyMsg);
        assertTrue(msg.isSet(Flags.Flag.ANSWERED));
        assertEquals(new InternetAddress("test@apache.org"), replyMsg.getRecipients(Message.RecipientType.TO)[0]);
    }

    @Test
    public void testJavaMail15Reply2() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);
        final InternetAddress dev = new InternetAddress("geronimo-dev@apache.org");
        
        msg.setFrom("test@apache.org");
        msg.setRecipient(Message.RecipientType.TO, dev);

        final Message replyMsg = msg.reply(false, false);
        assertNotNull(replyMsg);
        assertFalse(msg.isSet(Flags.Flag.ANSWERED));
        assertEquals(new InternetAddress("test@apache.org"), replyMsg.getRecipients(Message.RecipientType.TO)[0]);
    }


    @Test
    public void testSetSubject() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);

        final String simpleSubject = "Yada, yada";

        final String complexSubject = "Yada, yada\u0081";

        final String mungedSubject = "Yada, yada\u003F";

        msg.setSubject(simpleSubject);
        assertEquals(msg.getSubject(), simpleSubject);

        msg.setSubject(complexSubject, "UTF-8");
        assertEquals(msg.getSubject(), complexSubject);

        msg.setSubject(null);
        assertNull(msg.getSubject());
    }


    @Test
    public void testSetDescription() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);

        final String simpleSubject = "Yada, yada";

        final String complexSubject = "Yada, yada\u0081";

        final String mungedSubject = "Yada, yada\u003F";

        msg.setDescription(simpleSubject);
        assertEquals(msg.getDescription(), simpleSubject);

        msg.setDescription(complexSubject, "UTF-8");
        assertEquals(msg.getDescription(), complexSubject);

        msg.setDescription(null);
        assertNull(msg.getDescription());
    }


    @Test
    public void testGetContentType() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);
        assertEquals(msg.getContentType(), "text/plain");

        msg.setHeader("Content-Type", "text/xml");
        assertEquals(msg.getContentType(), "text/xml");
    }


    @Test
    public void testSetText() throws MessagingException {
        MimeMessage msg = new MimeMessage(session);

        msg.setText("Yada, yada");
        msg.saveChanges();
        ContentType type = new ContentType(msg.getContentType());
        assertTrue(type.match("text/plain"));

        msg = new MimeMessage(session);
        msg.setText("Yada, yada", "UTF-8");
        msg.saveChanges();
        type = new ContentType(msg.getContentType());
        assertTrue(type.match("text/plain"));
        assertEquals(type.getParameter("charset"), "UTF-8");

        msg = new MimeMessage(session);
        msg.setText("Yada, yada", "UTF-8", "xml");
        msg.saveChanges();
        type = new ContentType(msg.getContentType());
        assertTrue(type.match("text/xml"));
        assertEquals(type.getParameter("charset"), "UTF-8");
    }

    @Test
    public void testAddDateinUpdateHeaders() throws MessagingException, ParseException {
        final MimeMessage msg = new MimeMessage(session);
        msg.updateHeaders();

        final String[] dateHeader = msg.getHeader("Date");
        assertNotNull(dateHeader);
        assertEquals(1, dateHeader.length);

        final Date date = new MailDateFormat().parse(dateHeader[0]);
        final Date now = new Date();

        // this should be within 5 seconds
        assertTrue((now.getTime() - date.getTime()) < 5000);
    }


    @Test
    public void testGetFileNameFromDisposition() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);
        // the filename must survive a set/get round trip even though getDisposition()
        // only returns the bare disposition value.
        msg.setFileName("mailworld.txt");
        assertEquals("mailworld.txt", msg.getFileName());
        assertEquals(Part.ATTACHMENT, msg.getDisposition());

        // a repeated set must fully replace the previous filename parameter
        msg.setFileName("other.txt");
        assertEquals("other.txt", msg.getFileName());

        // a filename on a received header must be visible too
        final MimeMessage msg2 = new MimeMessage(session);
        msg2.setHeader("Content-Disposition", "attachment; filename=incoming.txt");
        assertEquals("incoming.txt", msg2.getFileName());
    }

    @Test
    public void testFileNameSystemProperties() throws MessagingException {
        // the encode/decode filename controls are System properties, and must be honored
        // even when the session knows nothing about them.
        System.setProperty("mail.mime.encodefilename", "false");
        System.setProperty("mail.mime.decodefilename", "true");
        try {
            final MimeMessage msg = new MimeMessage(session);
            msg.setFileName("=?ISO646-US?Q?=3F=3F-a=5Fgerman=5Fcharacter?=");
            assertEquals("??-a_german_character", msg.getFileName());
        } finally {
            System.clearProperty("mail.mime.encodefilename");
            System.clearProperty("mail.mime.decodefilename");
        }
    }

    @Test
    public void testSetFileNameEncoded() throws MessagingException {
        System.setProperty("mail.mime.charset", "utf-8");
        try {
            // a non-ASCII filename must be written in RFC 2231 encoded form by default
            final MimeMessage msg = new MimeMessage(session);
            msg.setFileName("¡");
            final String disposition = msg.getHeader("Content-Disposition", null);
            assertTrue(disposition.contains("filename*=utf-8''%C2%A1"),
                "unexpected disposition: " + disposition);
            assertEquals("¡", msg.getFileName());
        } finally {
            System.clearProperty("mail.mime.charset");
        }
    }

    @Test
    public void testContentLanguageSplit() throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);
        final String[] languages = {"en", "fr", "de"};
        msg.setContentLanguage(languages);
        // the comma-separated header must be split back into individual tags
        final String[] retrieved = msg.getContentLanguage();
        assertEquals(3, retrieved.length);
        assertEquals("en", retrieved[0]);
        assertEquals("fr", retrieved[1]);
        assertEquals("de", retrieved[2]);
    }

    @Test
    public void testAllowUtf8Headers() throws MessagingException, IOException {
        final String mailbox = "testα@exampleα.com";
        final String personal = "testα userα";

        final Properties props = new Properties();
        props.setProperty("mail.mime.allowutf8", "true");
        final Session utf8Session = Session.getInstance(props);

        final MimeMessage msg = new MimeMessage(utf8Session);
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailbox));
        msg.setHeader("Header", personal);
        msg.setText("");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out);
        final String written = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(written.contains("To: " + mailbox + "\r\n"), "missing UTF-8 To header:\n" + written);
        assertTrue(written.contains("Header: " + personal + "\r\n"), "missing UTF-8 custom header:\n" + written);

        // and reading the message back with the same session restores the unicode values
        final MimeMessage reread = new MimeMessage(utf8Session, new ByteArrayInputStream(out.toByteArray()));
        assertEquals(mailbox, ((InternetAddress) reread.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        assertEquals(personal, reread.getHeader("Header", null));

        // without the property, header output remains ISO8859-1 based
        final MimeMessage plain = new MimeMessage(session);
        plain.setHeader("X-Test", "abc");
        plain.setText("");
        final ByteArrayOutputStream plainOut = new ByteArrayOutputStream();
        plain.writeTo(plainOut);
        assertTrue(new String(plainOut.toByteArray(), java.nio.charset.StandardCharsets.ISO_8859_1)
            .contains("X-Test: abc\r\n"));
    }

    @BeforeEach
    public void setUp() throws Exception {
        defaultMap = CommandMap.getDefaultCommandMap();
        final MailcapCommandMap myMap = new MailcapCommandMap();
        myMap.addMailcap("text/plain;;    x-java-content-handler=" + MimeMultipartTest.DummyTextHandler.class.getName());
        myMap.addMailcap("multipart/*;;    x-java-content-handler=" + MimeMultipartTest.DummyMultipartHandler.class.getName());
        CommandMap.setDefaultCommandMap(myMap);
        final Properties props = new Properties();
        props.put("mail.user", "tester");
        props.put("mail.host", "apache.org");

        session = Session.getInstance(props);
    }

    @AfterEach
    public void tearDown() throws Exception {
        CommandMap.setDefaultCommandMap(defaultMap);
    }

    @Test
    public void testGetFileNameWithBlankDispositionHeader() throws Exception {
        // headers merged from store metadata can carry an empty
        // Content-Disposition value, which must read as "no file name"
        // rather than failing to parse
        MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        msg.setHeader("Content-Disposition", "");
        assertNull(msg.getFileName());
    }
}
