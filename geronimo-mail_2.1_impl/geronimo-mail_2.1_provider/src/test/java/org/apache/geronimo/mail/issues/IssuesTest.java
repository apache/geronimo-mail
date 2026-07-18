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
package org.apache.geronimo.mail.issues;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.junit.jupiter.api.Test;

import org.apache.geronimo.mail.testserver.AbstractProtocolTest;
import org.apache.geronimo.mail.testserver.MailServer;

public class IssuesTest extends AbstractProtocolTest {

    @Test
    public void testGERONIMO6519() throws Exception {

        PrintStream original = System.out;

        try {

            start();
            // Setup mail session
            Properties props = new Properties();
            props.setProperty("mail.debug", "true");
            props.setProperty("mail.smtp.port", String.valueOf(smtpConf.getListenerPort()));
            props.setProperty("mail.smtp.localhost", "some.full.qualified.name.com");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true));

            Session session = Session.getInstance(props);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("test@localhost"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress("test@localhost"));
            message.setText("test");

            Transport.send(message);
            Assertions.assertTrue(baos.toString().contains("EHLO some.full.qualified.name.com"));

        } finally {
            System.setOut(original);
        }

    }

    @Test
    public void testGERONIMO4594() throws Exception {
        Assertions.assertTrue(doGERONIMO4594(true, true));
    }

    @Test
    public void testGERONIMO4594Fail0() throws Exception {
        Assertions.assertFalse(doGERONIMO4594(false, true));
    }

    @Test
    public void testGERONIMO4594Fail1() throws Exception {
        // with mail.mime.encodefilename=false the filename is now written as an
        // RFC 2231 encoded parameter (filename*=charset''...), which decodes
        // transparently on read, so the round trip succeeds even without the
        // encode/decodefilename convenience properties.
        Assertions.assertTrue(doGERONIMO4594(false, false));
    }

    @Test
    public void testGERONIMO4594Fail2() throws Exception {
        // as above: RFC 2231 parameter encoding makes the filename round-trip
        // without the RFC 2047 convenience properties.
        Assertions.assertTrue(doGERONIMO4594(true, false));
    }
        
    private boolean doGERONIMO4594(boolean decode, boolean encode) throws Exception {

        final String specialFileName = "encoded_filename_\u00C4\u00DC\u00D6\u0226(test).pdf";
        
        System.setProperty("mail.mime.decodefilename", String.valueOf(decode));
        System.setProperty("mail.mime.encodefilename", String.valueOf(encode));
        try {

            start();

            // Setup mail session
            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.smtp.port", String.valueOf(smtpConf.getListenerPort()));
            // Retrieve via POP3: the raw MIME bytes round-trip unaltered, so the client-side
            // filename decoding is what gets tested. James 3.9's IMAP BODYSTRUCTURE decodes
            // encoded-word parameters on the server side and replaces non-ASCII with '?'.
            props.setProperty("mail.store.protocol", "pop3");
            props.setProperty("mail.pop3.port", String.valueOf(pop3Conf.getListenerPort()));
            //props.setProperty("mail.debug","true");
            Session session = Session.getInstance(props);

            MimeMessage msg = new MimeMessage(session);
            msg.setSubject("a file for you");
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress("serveruser@localhost"));
            msg.setFrom(new InternetAddress("serveruser@localhost"));

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            Multipart multipart = new MimeMultipart();
            messageBodyPart.setText("This is message body");
            File file = MailServer.getAbsoluteFilePathFromClassPath("pdf-test.pdf");
            Assertions.assertTrue(file.exists());
            DataSource source = new FileDataSource(file.getAbsoluteFile());
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(specialFileName);
            multipart.addBodyPart(messageBodyPart);
            msg.setContent(multipart);
            sendMessage(msg);
            server.ensureMsgCount(1);

            Session jmsession = Session.getInstance(props);
            Store store = jmsession.getStore();
            store.connect("127.0.0.1", "serveruser", "serverpass");
            Folder f = store.getFolder("INBOX");
            f.open(Folder.READ_ONLY); //TODO STAT only when folder open???
            Assertions.assertEquals(1, f.getMessageCount());
            Message[] messages = new Message[2];
            messages[0] = f.getMessage(1);
            boolean match = specialFileName.equals(((Multipart) messages[0].getContent()).getBodyPart(0).getFileName());
            f.close(false);
            store.close();
            return match;

        } finally {
            System.setProperty("mail.mime.decodefilename", "false");
            System.setProperty("mail.mime.encodefilename", "false");
        }

    }

}
