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

package jakarta.mail.event;

import jakarta.mail.Folder;
import jakarta.mail.TestData;

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public class MessageCountEventTest extends TestCase {
    public MessageCountEventTest(final String name) {
        super(name);
    }
    public void testEvent() {
        doEventTests(MessageCountEvent.ADDED);
        doEventTests(MessageCountEvent.REMOVED);
        try {
            doEventTests(-12345);
            fail("Expected exception due to invalid type -12345");
        } catch (final IllegalArgumentException e) {
        }
    }
    private void doEventTests(final int type) {
        final Folder folder = TestData.getTestFolder();
        final MessageCountEvent event =
            new MessageCountEvent(folder, type, false, null);
        assertEquals(folder, event.getSource());
        assertEquals(type, event.getType());
        final MessageCountListenerTest listener = new MessageCountListenerTest();
        event.dispatch(listener);
        assertEquals("Unexpcted method dispatched", type, listener.getState());
    }
    public static class MessageCountListenerTest
        implements MessageCountListener {
        private int state = 0;
        public void messagesAdded(final MessageCountEvent event) {
            if (state != 0) {
                fail("Recycled Listener");
            }
            state = MessageCountEvent.ADDED;
        }
        public void messagesRemoved(final MessageCountEvent event) {
            if (state != 0) {
                fail("Recycled Listener");
            }
            state = MessageCountEvent.REMOVED;
        }
        public int getState() {
            return state;
        }
    }
}
