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

import jakarta.mail.Flags;

import org.apache.geronimo.mail.store.imap.connection.IMAPResponseTokenizer.Token;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the respcode-bracket-parsing defect: '[' is not a
 * delimiter in the tokenizer's default atom mode, so "[PERMANENTFLAGS"
 * arrived as a single ATOM token and every '* OK [keyword args]' response
 * code collapsed into a generic OK response.  getPermanentFlags() therefore
 * always returned null and the open-time UIDVALIDITY/UIDNEXT/UNSEEN metadata
 * was silently lost.
 */
public class IMAPOkResponseParsingTest {

    private IMAPResponse parse(final String line) throws Exception {
        final IMAPResponseStream stream =
                new IMAPResponseStream(new ByteArrayInputStream(line.getBytes("ISO8859-1")));
        return stream.readResponse();
    }

    @Test
    public void testPlainOkResponseHasOkKeyword() throws Exception {
        final IMAPOkResponse response = (IMAPOkResponse) parse("* OK ready to serve\r\n");
        assertTrue(response.isKeyword("OK"));
        assertEquals("ready to serve", response.getMessage().trim());
    }

    @Test
    public void testUidValidityResponseCodeIsRecognized() throws Exception {
        final IMAPOkResponse response = (IMAPOkResponse) parse("* OK [UIDVALIDITY 137045278] UIDs valid\r\n");
        assertTrue(response.isKeyword("UIDVALIDITY"),
                "the response code inside the brackets must become the response keyword");
        assertEquals(137045278L, ((Token) response.getStatus().get(0)).getLong());
    }

    @Test
    public void testUidNextResponseCodeIsRecognized() throws Exception {
        final IMAPOkResponse response = (IMAPOkResponse) parse("* OK [UIDNEXT 4] Predicted next UID\r\n");
        assertTrue(response.isKeyword("UIDNEXT"));
        assertEquals(4L, ((Token) response.getStatus().get(0)).getLong());
    }

    @Test
    public void testUnseenResponseCodeIsRecognized() throws Exception {
        final IMAPOkResponse response = (IMAPOkResponse) parse("* OK [UNSEEN 3] Message 3 is first unseen\r\n");
        assertTrue(response.isKeyword("UNSEEN"));
        assertEquals(3, ((Token) response.getStatus().get(0)).getInteger());
    }

    @Test
    public void testPermanentFlagsResponseCodeIsRecognized() throws Exception {
        final IMAPPermanentFlagsResponse response = (IMAPPermanentFlagsResponse)
                parse("* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Seen \\*)] Limited\r\n");
        assertTrue(response.isKeyword("PERMANENTFLAGS"));
        assertTrue(response.flags.contains(Flags.Flag.SEEN));
        assertTrue(response.flags.contains(Flags.Flag.ANSWERED));
        assertTrue(response.flags.contains(Flags.Flag.DELETED));
        assertTrue(response.flags.contains(Flags.Flag.USER),
                "the \\* wildcard must map to the USER flag");
    }

    @Test
    public void testMailboxStatusMergesOkResponseCodes() throws Exception {
        final IMAPMailboxStatus status = new IMAPMailboxStatus();
        status.mergeStatus((IMAPOkResponse) parse("* OK [UIDVALIDITY 137045278] UIDs valid\r\n"));
        status.mergeStatus((IMAPOkResponse) parse("* OK [UIDNEXT 4] Predicted next UID\r\n"));
        status.mergeStatus((IMAPOkResponse) parse("* OK [UNSEEN 3] first unseen\r\n"));

        assertEquals(137045278L, status.uidValidity);
        assertEquals(4L, status.uidNext);
        // regression: the UNSEEN branch used to assign uidValidity instead
        assertEquals(3, status.unseenMessages);
        assertEquals(137045278L, status.uidValidity,
                "merging UNSEEN must not clobber the uidValidity value");
    }
}
