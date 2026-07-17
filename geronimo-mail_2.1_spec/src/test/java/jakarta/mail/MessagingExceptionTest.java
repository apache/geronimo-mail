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

package jakarta.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @version $Revision $ $Date$
 */
public class MessagingExceptionTest {
    private RuntimeException e;
    private MessagingException d;
    private MessagingException c;
    private MessagingException b;
    private MessagingException a;

    @BeforeEach
    public void setUp() throws Exception {
        
        //Initialize cause with null, make sure the getCause will not be affected
        a = new MessagingException("A", null);
        b = new MessagingException("B");
        c = new MessagingException("C");
        d = new MessagingException("D");
        e = new RuntimeException("E");
    }

    @Test
    public void testMessagingExceptionString() {
        assertEquals("A", a.getMessage());
    }

    @Test
    public void testNextException() {
        assertTrue(a.setNextException(b));
        assertEquals(b, a.getNextException());
        assertEquals(b, a.getCause());
        
        assertTrue(a.setNextException(c));
        assertEquals(b, a.getNextException());
        assertEquals(c, b.getNextException());
        assertEquals(c, b.getCause());
        
        assertTrue(a.setNextException(d));
        
        assertEquals(b, a.getNextException());
        assertEquals(b, a.getCause());
        
        assertEquals(c, b.getNextException());
        assertEquals(c, b.getCause());
        
        assertEquals(d, c.getNextException());
        assertEquals(d, c.getCause());
        
        final String message = a.getMessage();
        final int ap = message.indexOf("A");
        final int bp = message.indexOf("B");
        final int cp = message.indexOf("C");
        assertTrue(ap != -1, "A does not contain 'A'");
        assertTrue(bp != -1, "B does not contain 'B'");
        assertTrue(cp != -1, "C does not contain 'C'");
    }

    @Test
    public void testNextExceptionWrong() {
        assertTrue(a.setNextException(e));
        assertFalse(a.setNextException(b));
    }

    @Test
    public void testNextExceptionWrong2() {
        assertTrue(a.setNextException(e));
        assertFalse(a.setNextException(b));
    }

    @Test
    public void testMessagingExceptionStringException() {
        final MessagingException x = new MessagingException("X", a);
        assertEquals("X (jakarta.mail.MessagingException: A)", x.getMessage());
        assertEquals(a, x.getNextException());
        assertEquals(a, x.getCause());
    }
}
