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

import java.util.*;

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public class FlagsTest extends TestCase {
    private List flagtypes;
    private Flags flags;
    /**
     * Constructor for FlagsTest.
     * @param arg0
     */
    public FlagsTest(final String name) {
        super(name);
    }
    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        flags = new Flags();
        flagtypes = new LinkedList();
        flagtypes.add(Flags.Flag.ANSWERED);
        flagtypes.add(Flags.Flag.DELETED);
        flagtypes.add(Flags.Flag.DRAFT);
        flagtypes.add(Flags.Flag.FLAGGED);
        flagtypes.add(Flags.Flag.RECENT);
        flagtypes.add(Flags.Flag.SEEN);
        Collections.shuffle(flagtypes);
    }
    public void testHashCode() {
        final int before = flags.hashCode();
        flags.add("Test");
        assertTrue(
            "Before: " + before + ", now " + flags.hashCode(),
            flags.hashCode() != before);
        assertTrue(flags.hashCode() != 0);
    }
    /*
     * Test for void add(Flag)
     */
    public void testAddAndRemoveFlag() {
        Iterator it = flagtypes.iterator();
        while (it.hasNext()) {
            final Flags.Flag flag = (Flags.Flag) it.next();
            assertFalse(flags.contains(flag));
            flags.add(flag);
            assertTrue(flags.contains(flag));
        }
        it = flagtypes.iterator();
        while (it.hasNext()) {
            final Flags.Flag flag = (Flags.Flag) it.next();
            flags.remove(flag);
            assertFalse(flags.contains(flag));
        }
    }
    /*
     * Test for void add(String)
     */
    public void testAddString() {
        assertFalse(flags.contains("Frog"));
        flags.add("Frog");
        assertTrue(flags.contains("Frog"));
        flags.remove("Frog");
        assertFalse(flags.contains("Frog"));
    }
    /*
     * Test for void add(Flags)
     */
    public void testAddFlags() {
        final Flags other = new Flags();
        other.add("Stuff");
        other.add(Flags.Flag.RECENT);
        flags.add(other);
        assertTrue(flags.contains("Stuff"));
        assertTrue(flags.contains(Flags.Flag.RECENT));
        assertTrue(flags.contains(other));
        assertTrue(flags.contains(flags));
        flags.add("Thing");
        assertTrue(flags.contains("Thing"));
        flags.remove(other);
        assertFalse(flags.contains("Stuff"));
        assertFalse(flags.contains(Flags.Flag.RECENT));
        assertFalse(flags.contains(other));
        assertTrue(flags.contains("Thing"));
    }
    /*
     * Test for boolean equals(Object)
     */
    public void testEqualsObject() {
        final Flags other = new Flags();
        other.add("Stuff");
        other.add(Flags.Flag.RECENT);
        flags.add(other);
        assertEquals(flags, other);
    }
    public void testGetSystemFlags() {
        flags.add("Stuff");
        flags.add("Another");
        flags.add(Flags.Flag.FLAGGED);
        flags.add(Flags.Flag.RECENT);
        final Flags.Flag[] array = flags.getSystemFlags();
        assertEquals(2, array.length);
        assertTrue(
            (array[0] == Flags.Flag.FLAGGED && array[1] == Flags.Flag.RECENT)
                || (array[0] == Flags.Flag.RECENT
                    && array[1] == Flags.Flag.FLAGGED));
    }
    public void testGetUserFlags() {
        final String stuff = "Stuff";
        final String another = "Another";
        flags.add(stuff);
        flags.add(another);
        flags.add(Flags.Flag.FLAGGED);
        flags.add(Flags.Flag.RECENT);
        final String[] array = flags.getUserFlags();
        assertEquals(2, array.length);
        assertTrue(
            (array[0] == stuff && array[1] == another)
                || (array[0] == another && array[1] == stuff));
    }
    public void testClone() throws CloneNotSupportedException {
        flags.add("Thing");
        flags.add(Flags.Flag.RECENT);
        final Flags other = (Flags) flags.clone();
        assertTrue(other != flags);
        assertEquals(other, flags);
    }

    public void testClearSystemFlags() {
        Flags f = new Flags();
        f.add(Flags.Flag.ANSWERED);
        f.add(Flags.Flag.DELETED);
        f.add(Flags.Flag.DRAFT);
        f.add(Flags.Flag.FLAGGED);
        f.add(Flags.Flag.RECENT);
        f.add(Flags.Flag.SEEN);
        f.add("TEST");

        f.clearSystemFlags();

        assertEquals(0, f.getSystemFlags().length);
        assertEquals(1, f.getUserFlags().length);
        assertEquals("TEST", f.getUserFlags()[0]);
    }

    public void testClearuserFlags() {
        Flags f = new Flags();
        f.add(Flags.Flag.ANSWERED);
        f.add(Flags.Flag.DELETED);
        f.add(Flags.Flag.DRAFT);
        f.add(Flags.Flag.FLAGGED);
        f.add(Flags.Flag.RECENT);
        f.add(Flags.Flag.SEEN);
        f.add("TEST");

        f.clearUserFlags();

        assertEquals(6, f.getSystemFlags().length);
        assertTrue(Arrays.asList(f.getSystemFlags()).contains(Flags.Flag.ANSWERED));
        assertTrue(Arrays.asList(f.getSystemFlags()).contains(Flags.Flag.DELETED));
        assertTrue(Arrays.asList(f.getSystemFlags()).contains(Flags.Flag.DRAFT));
        assertTrue(Arrays.asList(f.getSystemFlags()).contains(Flags.Flag.FLAGGED));
        assertTrue(Arrays.asList(f.getSystemFlags()).contains(Flags.Flag.RECENT));
        assertTrue(Arrays.asList(f.getSystemFlags()).contains(Flags.Flag.SEEN));
        assertEquals(0, f.getUserFlags().length);
    }

    public void testRetainAllFlags() {
        Flags f = new Flags();
        f.add(Flags.Flag.ANSWERED);
        f.add(Flags.Flag.DELETED);
        f.add(Flags.Flag.DRAFT);
        f.add(Flags.Flag.FLAGGED);
        f.add(Flags.Flag.RECENT);
        f.add(Flags.Flag.SEEN);
        f.add("TEST");
        f.add("FLAG");

        Flags retain = new Flags();
        retain.add(Flags.Flag.SEEN);
        retain.add(Flags.Flag.ANSWERED);
        retain.add("TEST");
        retain.add("SUPER IMPORTANT");

        f.retainAll(retain);

        assertEquals(2, f.getSystemFlags().length);
        assertTrue(Arrays.asList(f.getSystemFlags()).contains(Flags.Flag.ANSWERED));
        assertTrue(Arrays.asList(f.getSystemFlags()).contains(Flags.Flag.SEEN));
        assertEquals(1, f.getUserFlags().length);
        assertEquals("TEST", f.getUserFlags()[0]);
    }
}
