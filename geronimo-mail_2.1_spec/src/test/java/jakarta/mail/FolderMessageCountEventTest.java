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
package jakarta.mail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test: MessageCountEvents used to be queued to the
 * MessageChangedListener list, so registered MessageCountListeners never
 * received any notification at all.
 */
public class FolderMessageCountEventTest {

    static class NotifyingFolder extends SimpleFolder {
        NotifyingFolder(final Store store) {
            super(store);
        }

        void fireAdded(final Message[] messages) {
            notifyMessageAddedListeners(messages);
        }

        void fireRemoved(final Message[] messages) {
            notifyMessageRemovedListeners(true, messages);
        }
    }

    @Test
    public void testMessageCountListenerReceivesEvents() throws Exception {
        final Session session = Session.getInstance(new java.util.Properties());
        final Store store = session.getStore(new Provider(Provider.Type.STORE, "test", NullStore.class.getName(), "test", "1.0"));
        final NotifyingFolder folder = new NotifyingFolder(store);

        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicInteger added = new AtomicInteger();
        final AtomicInteger removed = new AtomicInteger();
        final AtomicReference<MessageCountEvent> lastEvent = new AtomicReference<>();

        folder.addMessageCountListener(new MessageCountListener() {
            @Override
            public void messagesAdded(final MessageCountEvent event) {
                added.incrementAndGet();
                lastEvent.set(event);
                latch.countDown();
            }

            @Override
            public void messagesRemoved(final MessageCountEvent event) {
                removed.incrementAndGet();
                latch.countDown();
            }
        });
        // a changed-listener must NOT see count events
        final AtomicInteger changed = new AtomicInteger();
        folder.addMessageChangedListener(event -> changed.incrementAndGet());

        folder.fireAdded(new Message[0]);
        folder.fireRemoved(new Message[0]);

        assertTrue(latch.await(30, TimeUnit.SECONDS), "MessageCountListener was never notified");
        assertEquals(1, added.get());
        assertEquals(1, removed.get());
        assertEquals(MessageCountEvent.ADDED, lastEvent.get().getType());
        assertEquals(0, changed.get());
    }

    public static class NullStore extends Store {
        public NullStore(final Session session, final URLName name) {
            super(session, name);
        }

        @Override
        public Folder getDefaultFolder() {
            return null;
        }

        @Override
        public Folder getFolder(final String name) {
            return null;
        }

        @Override
        public Folder getFolder(final URLName name) {
            return null;
        }
    }

}
