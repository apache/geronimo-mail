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
package org.apache.geronimo.mail.transport.smtp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A message addressed to several recipients of the same type must be handed to a single transport and
 * therefore transmitted once, with one RCPT TO per recipient - not once per recipient (GERONIMO-6713).
 */
public class SMTPTransportMultipleRecipientsTest {

    @Test
    public void testMessageIsTransmittedOnceForMultipleRecipients() throws Exception {
        final RecordingSMTPServer server = new RecordingSMTPServer();
        server.start();
        try {
            final Properties props = new Properties();
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.smtp.host", "localhost");
            props.setProperty("mail.smtp.port", String.valueOf(server.getPort()));

            final Session session = Session.getInstance(props);
            final MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("mail@test.zz"));
            message.setRecipients(Message.RecipientType.TO, new InternetAddress[]{
                    new InternetAddress("to111@test.zz"),
                    new InternetAddress("to222@test.zz")});
            message.setSubject("Test Subject");
            message.setText("Some Mail Content");

            Transport.send(message);

            assertTrue(server.awaitQuiet(), "the fake SMTP server did not finish handling the session");
            assertEquals(1, server.getDataCount(), "the message must be transmitted exactly once");
            assertEquals(2, server.getRecipients().size(), "both recipients belong to the same transmission");
            assertTrue(server.getRecipients().get(0).contains("to111@test.zz"));
            assertTrue(server.getRecipients().get(1).contains("to222@test.zz"));
        } finally {
            server.stop();
        }
    }

    private static final class RecordingSMTPServer {
        private final ServerSocket serverSocket;
        private final AtomicInteger dataCount = new AtomicInteger();
        private final List<String> recipients = Collections.synchronizedList(new ArrayList<String>());
        private final CountDownLatch finished = new CountDownLatch(1);
        private volatile Thread acceptor;

        RecordingSMTPServer() throws IOException {
            serverSocket = new ServerSocket(0);
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        int getDataCount() {
            return dataCount.get();
        }

        List<String> getRecipients() {
            return recipients;
        }

        boolean awaitQuiet() throws InterruptedException {
            return finished.await(10, TimeUnit.SECONDS);
        }

        void start() {
            acceptor = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (!serverSocket.isClosed()) {
                            converse(serverSocket.accept());
                        }
                    } catch (final IOException e) {
                        // the socket was closed, we are done
                    }
                }
            }, "recording-smtp-server");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        void stop() throws IOException {
            serverSocket.close();
        }

        private void converse(final Socket socket) throws IOException {
            try (Socket client = socket;
                 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "US-ASCII"));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "US-ASCII"))) {

                reply(out, "220 localhost fake ESMTP");
                boolean inData = false;
                String line;
                while ((line = in.readLine()) != null) {
                    if (inData) {
                        if (".".equals(line)) {
                            inData = false;
                            reply(out, "250 2.0.0 Ok");
                        }
                        continue;
                    }
                    final String command = line.toUpperCase(Locale.ROOT);
                    if (command.startsWith("EHLO")) {
                        reply(out, "250-localhost");
                        reply(out, "250 8BITMIME");
                    } else if (command.startsWith("RCPT TO")) {
                        recipients.add(line);
                        reply(out, "250 2.1.5 Ok");
                    } else if (command.startsWith("DATA")) {
                        dataCount.incrementAndGet();
                        inData = true;
                        reply(out, "354 End data with <CR><LF>.<CR><LF>");
                    } else if (command.startsWith("QUIT")) {
                        reply(out, "221 2.0.0 Bye");
                        break;
                    } else {
                        reply(out, "250 2.0.0 Ok");
                    }
                }
            } finally {
                finished.countDown();
            }
        }

        private void reply(final BufferedWriter out, final String line) throws IOException {
            out.write(line);
            out.write("\r\n");
            out.flush();
        }
    }
}
