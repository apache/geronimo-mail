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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * Representation of flags that may be associated with a message.
 * Flags can either be system flags, defined by the {@link Flags.Flag Flag} inner class,
 * or user-defined flags defined by a String. The system flags represent those expected
 * to be provided by most folder systems; user-defined flags allow for additional flags
 * on a per-provider basis.
 * <p/>
 * This class is Serializable but compatibility is not guaranteed across releases.
 *
 * @version $Rev$ $Date$
 */
public class Flags implements Cloneable, Serializable {

    private static final long serialVersionUID = 6243590407214169028L;

    public static final class Flag {
        /**
         * Flag that indicates that the message has been replied to; has a bit value of 1.
         */
        public static final Flag ANSWERED = new Flag(1);
        /**
         * Flag that indicates that the message has been marked for deletion and
         * should be removed on a subsequent expunge operation; has a bit value of 2.
         */
        public static final Flag DELETED = new Flag(2);
        /**
         * Flag that indicates that the message is a draft; has a bit value of 4.
         */
        public static final Flag DRAFT = new Flag(4);
        /**
         * Flag that indicates that the message has been flagged; has a bit value of 8.
         */
        public static final Flag FLAGGED = new Flag(8);
        /**
         * Flag that indicates that the message has been delivered since the last time
         * this folder was opened; has a bit value of 16.
         */
        public static final Flag RECENT = new Flag(16);
        /**
         * Flag that indicates that the message has been viewed; has a bit value of 32.
         * This flag is set by the {@link Message#getInputStream()} and {@link Message#getContent()}
         * methods.
         */
        public static final Flag SEEN = new Flag(32);
        /**
         * Flags that indicates if this folder supports user-defined flags; has a bit value of 0x80000000.
         */
        public static final Flag USER = new Flag(0x80000000);

        private final int mask;

        private Flag(final int mask) {
            this.mask = mask;
        }
    }

    // the Serialized form of this class required the following two fields to be persisted
    // this leads to a specific type of implementation
    private int system_flags;
    private final Hashtable<String, String> user_flags;

    /**
     * Construct a Flags instance with no flags set.
     */
    public Flags() {
        user_flags = new Hashtable<>();
    }

    /**
     * Construct a Flags instance with a supplied system flag set.
     * @param flag the system flag to set
     */
    public Flags(final Flag flag) {
        system_flags = flag.mask;
        user_flags = new Hashtable<>();
    }

    /**
     * Construct a Flags instance with a same flags set.
     * @param flags the instance to copy
     */
    public Flags(final Flags flags) {
        system_flags = flags.system_flags;
        user_flags = new Hashtable<>(flags.user_flags);
    }

    /**
     * Construct a Flags instance with the supplied user flags set.
     * Question: should this automatically set the USER system flag?
     * @param name the user flag to set
     */
    public Flags(final String name) {
        user_flags = new Hashtable<>();
        user_flags.put(name.toLowerCase(), name);
    }

    /**
     * Set a system flag.
     * @param flag the system flag to set
     */
    public void add(final Flag flag) {
        system_flags |= flag.mask;
    }

    /**
     * Set all system and user flags from the supplied Flags.
     * Question: do we need to check compatibility of USER flags?
     * @param flags the Flags to add
     */
    public void add(final Flags flags) {
        system_flags |= flags.system_flags;
        user_flags.putAll(flags.user_flags);
    }

    /**
     * Set a user flag.
     * Question: should this fail if the USER system flag is not set?
     * @param name the user flag to set
     */
    public void add(final String name) {
        user_flags.put(name.toLowerCase(), name);
    }

    /**
     * Return a copy of this instance.
     * @return a copy of this instance
     */
    @Override
    public Object clone() {
        return new Flags(this);
    }

    /**
     * See if the supplied system flags are set
     * @param flag the system flags to check for
     * @return true if the flags are set
     */
    public boolean contains(final Flag flag) {
        return (system_flags & flag.mask) != 0;
    }

    /**
     * See if all of the supplied Flags are set
     * @param flags the flags to check for
     * @return true if all the supplied system and user flags are set
     */
    public boolean contains(final Flags flags) {
        return ((system_flags & flags.system_flags) == flags.system_flags)
                && user_flags.keySet().containsAll(flags.user_flags.keySet());
    }

    /**
     * See if the supplied user flag is set
     * @param name the user flag to check for
     * @return true if the flag is set
     */
    public boolean contains(final String name) {
        return user_flags.containsKey(name.toLowerCase());
    }

    /**
     * Equality is defined as true if the other object is a instanceof Flags with the
     * same system and user flags set (using a case-insensitive name comparison for user flags).
     * @param other the instance to compare against
     * @return true if the two instance are the same
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
			return true;
		}
        if (other instanceof Flags == false) {
			return false;
		}
        final Flags flags = (Flags) other;
        return system_flags == flags.system_flags && user_flags.keySet().equals(flags.user_flags.keySet());
    }

    /**
     * Calculate a hashCode for this instance
     * @return a hashCode for this instance
     */
    @Override
    public int hashCode() {
        return system_flags ^ user_flags.keySet().hashCode();
    }

    /**
     * Return a list of {@link Flags.Flag Flags} containing the system flags that have been set
     * @return the system flags that have been set
     */
    public Flag[] getSystemFlags() {
        // assumption: it is quicker to calculate the size than it is to reallocate the array
        int size = 0;
        if ((system_flags & Flag.ANSWERED.mask) != 0) {
			size += 1;
		}
        if ((system_flags & Flag.DELETED.mask) != 0) {
			size += 1;
		}
        if ((system_flags & Flag.DRAFT.mask) != 0) {
			size += 1;
		}
        if ((system_flags & Flag.FLAGGED.mask) != 0) {
			size += 1;
		}
        if ((system_flags & Flag.RECENT.mask) != 0) {
			size += 1;
		}
        if ((system_flags & Flag.SEEN.mask) != 0) {
			size += 1;
		}
        if ((system_flags & Flag.USER.mask) != 0) {
			size += 1;
		}
        final Flag[] result = new Flag[size];
        if ((system_flags & Flag.USER.mask) != 0) {
			result[--size] = Flag.USER;
		}
        if ((system_flags & Flag.SEEN.mask) != 0) {
			result[--size] = Flag.SEEN;
		}
        if ((system_flags & Flag.RECENT.mask) != 0) {
			result[--size] = Flag.RECENT;
		}
        if ((system_flags & Flag.FLAGGED.mask) != 0) {
			result[--size] = Flag.FLAGGED;
		}
        if ((system_flags & Flag.DRAFT.mask) != 0) {
			result[--size] = Flag.DRAFT;
		}
        if ((system_flags & Flag.DELETED.mask) != 0) {
			result[--size] = Flag.DELETED;
		}
        if ((system_flags & Flag.ANSWERED.mask) != 0) {
			result[--size] = Flag.ANSWERED;
		}
        return result;
    }

    /**
     * Return a list of user flags that have been set
     * @return a list of user flags
     */
    public String[] getUserFlags() {
        return (String[]) user_flags.values().toArray(new String[user_flags.values().size()]);
    }

    /**
     * Unset the supplied system flag.
     * Question: what happens if we unset the USER flags and user flags are set?
     * @param flag the flag to clear
     */
    public void remove(final Flag flag) {
        system_flags &= ~flag.mask;
    }

    /**
     * Unset all flags from the supplied instance.
     * @param flags the flags to clear
     */
    public void remove(final Flags flags) {
        system_flags &= ~flags.system_flags;
        user_flags.keySet().removeAll(flags.user_flags.keySet());
    }

    /**
     * Unset the supplied user flag.
     * @param name the flag to clear
     */
    public void remove(final String name) {
        user_flags.remove(name.toLowerCase());
    }

    /**
     * Remove any flags <strong>not</strong> in the given Flags object.
     * Useful for clearing flags not supported by a server.  If the
     * given Flags object includes the Flags.Flag.USER flag, all user
     * flags in this Flags object are retained.
     *
     * @param	f	the flags to keep
     * @return		true if this Flags object changed
     * @since		JavaMail 1.6
     */
    public boolean retainAll(Flags f) {
        boolean changed = false;

        if (this.system_flags != f.system_flags) {
            this.system_flags = f.system_flags;
            changed = true;
        }

        final Set<String> keys = new HashSet<>(this.user_flags.keySet());
        for (final String user_flag : keys) {
            if (! f.user_flags.containsKey(user_flag)) {
                this.user_flags.remove(user_flag);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Clear all of the system flags.
     *
     * @since	JavaMail 1.6
     */
    public void clearSystemFlags() {
        this.system_flags = 0;
    }

    /**
     * Clear all of the user flags.
     *
     * @since	JavaMail 1.6
     */
    public void clearUserFlags() {
        this.user_flags.clear();
    }
}
