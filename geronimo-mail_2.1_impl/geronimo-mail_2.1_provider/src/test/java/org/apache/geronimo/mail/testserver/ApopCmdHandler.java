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
package org.apache.geronimo.mail.testserver;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.james.core.Username;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.pop3server.mailbox.MailboxAdapterFactory;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.lib.POP3BeforeSMTPHelper;
import org.apache.james.protocols.pop3.POP3Response;
import org.apache.james.protocols.pop3.POP3Session;
import org.apache.james.protocols.pop3.core.AbstractApopCmdHandler;
import org.apache.james.protocols.pop3.mailbox.Mailbox;

import reactor.core.publisher.Mono;

public class ApopCmdHandler extends AbstractApopCmdHandler {

    private final MailboxManager manager;
    private final MailboxAdapterFactory mailboxAdapterFactory;

    @Inject
    public ApopCmdHandler(@Named("mailboxmanager") final MailboxManager manager,
                          final MailboxAdapterFactory mailboxAdapterFactory,
                          final MetricFactory metricFactory) {
        super(metricFactory);
        this.manager = manager;
        this.mailboxAdapterFactory = mailboxAdapterFactory;
    }

    @Override
    public Response onCommand(final POP3Session session, final Request request) {
        final Response response = super.onCommand(session, request);
        if (POP3Response.OK_RESPONSE.equals(response.getRetCode())) {
            POP3BeforeSMTPHelper.addIPAddress(session.getRemoteAddress().getAddress().getHostAddress());
        }
        return response;
    }

    @Override
    protected Mailbox auth(final POP3Session session, final String apopTimestamp, final Username user, final String digest) throws Exception {
        final String plaintextpassword = MailServer.PASSWORD;

        final String toHash = apopTimestamp.trim() + plaintextpassword;
        if (!getMD5(toHash).equals(digest)) {
            System.out.println("Digests does not match");
            return null;
        }

        MailboxSession mSession = null;
        try {
            mSession = manager.createSystemSession(user);
            manager.startProcessingRequest(mSession);
            final MailboxPath inbox = MailboxPath.inbox(mSession);

            // check if the mailbox exists, if not create it
            if (!Mono.from(manager.mailboxExists(inbox, mSession)).block()) {
                manager.createMailbox(inbox, mSession);
            }
            final MessageManager mailbox = manager.getMailbox(inbox, mSession);
            return mailboxAdapterFactory.create(mailbox, mSession);
        } catch (final BadCredentialsException e) {
            return null;
        } catch (final MailboxException e) {
            throw new IOException("Unable to access mailbox for user " + user.asString(), e);
        } finally {
            if (mSession != null) {
                manager.endProcessingRequest(mSession);
            }
        }

    }

    private static String getMD5(final String input) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] messageDigest = md.digest(input.getBytes());
            final BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
