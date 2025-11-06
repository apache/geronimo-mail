package org.apache.geronimo.mail.issues;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import jakarta.activation.DataHandler;
import jakarta.mail.BodyPart;
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
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.geronimo.mail.testserver.AbstractProtocolTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;

public class GERONIMO6884Test extends AbstractProtocolTest {
    public void testGERONIMO6884_0() throws Exception {

        final MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Text body!");

        byte[] bytes = new byte[47100];
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-img.png")) {
            is.read(bytes);
        }

        final MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(bytes, "image/png")));
        attachmentPart.setHeader("Content-Transfer-Encoding", "base64"); // crucial

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(attachmentPart);

        final Message message = sendAndGetMessage(multipart);

        final MimeMultipart content = (MimeMultipart) message.getContent();
        final BodyPart attachment = content.getBodyPart(1);

        final InputStream is = (InputStream) attachment.getContent();
        byte[] attachmentBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            attachmentBytes = baos.toByteArray();
        }
        assertArrayEquals(bytes, attachmentBytes);
    }

    public void testGERONIMO6884_1() throws Exception {

        final MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Text body!");

        // Use just 1 byte to trigger the "small buffer" case
        byte[] bytes = new byte[] { 42 }; // any value works

        final MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(bytes, "image/png")));
        attachmentPart.setHeader("Content-Transfer-Encoding", "base64"); // crucial

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(attachmentPart);

        final Message message = sendAndGetMessage(multipart);
        final MimeMultipart content = (MimeMultipart) message.getContent();
        final BodyPart attachment = content.getBodyPart(1);

        final InputStream is = (InputStream) attachment.getContent();
        byte[] attachmentBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            attachmentBytes = baos.toByteArray();
        }
        assertArrayEquals(bytes, attachmentBytes);
    }

    public void testGERONIMO6884_2() throws Exception {

        final MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Text body!");

        // Use just 1 byte to trigger the "small buffer" case
        byte[] bytes = new byte[]{42}; // any value works

        final MimeBodyPart attachmentPart = new MimeBodyPart(new ByteArrayInputStream(bytes));

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(attachmentPart);

        final Message message = sendAndGetMessage(multipart);

        final MimeMultipart content = (MimeMultipart) message.getContent();
        final BodyPart attachment = content.getBodyPart(1);

        final Object o = attachment.getContent();
        assertNotNull(o); //this will be an empty string
    }


    private Message sendAndGetMessage(Multipart multipart) throws Exception {

        start();
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.imap.port", String.valueOf(imapConf.getListenerPort()));
        props.setProperty("mail.smtp.port", String.valueOf(smtpConf.getListenerPort()));
        //props.setProperty("mail.debug", "true");
        Session session = Session.getInstance(props);

        Message message = new MimeMessage(session);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("test@mockserver.com"));
        message.setSubject("Test attachment content-type");
        message.setContent(multipart);

        Transport.send(message);

        return getAttachmentContentType(session);
    }

    private Message getAttachmentContentType(Session session) throws Exception {
        final Store store = session.getStore();
        store.connect("127.0.0.1", "serveruser", "serverpass");

        Folder folder = store.getDefaultFolder();
        folder = folder.getFolder("inbox");
        folder.open(Folder.READ_ONLY);

        server.ensureMsgCount(1);

        return folder.getMessage(1);
    }

}
