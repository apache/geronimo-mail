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

package jakarta.mail.internet;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import jakarta.mail.Address;
import jakarta.mail.Session;

/**
 * A representation of an Internet email address as specified by RFC822 in
 * conjunction with a human-readable personal name that can be encoded as
 * specified by RFC2047.
 * A typical address is "user@host.domain" and personal name "Joe User"
 *
 * @version $Rev$ $Date$
 */
public class InternetAddress extends Address implements Cloneable {
	
	private static final long serialVersionUID = -7507595530758302903L;
	
    /**
     * The address in RFC822 format.
     */
    protected String address;

    /**
     * The personal name in RFC2047 format.
     * Subclasses must ensure that this field is updated if the personal field
     * is updated; alternatively, it can be invalidated by setting to null
     * which will cause it to be recomputed.
     */
    protected String encodedPersonal;

    /**
     * The personal name as a Java String.
     * Subclasses must ensure that this field is updated if the encodedPersonal field
     * is updated; alternatively, it can be invalidated by setting to null
     * which will cause it to be recomputed.
     */
    protected String personal;

    public InternetAddress() {
    }

    public InternetAddress(final String address) throws AddressException {
        this(address, true);
    }

    public InternetAddress(final String address, final boolean strict) throws AddressException {
        // use the parse method to process the address.  This has the wierd side effect of creating a new
        // InternetAddress instance to create an InternetAddress, but these are lightweight objects and
        // we need access to multiple pieces of data from the parsing process.
        final AddressParser parser = new AddressParser(address, strict ? AddressParser.STRICT : AddressParser.NONSTRICT);

        final InternetAddress parsedAddress = parser.parseAddress();
        // copy the important information, which right now is just the address and
        // personal info.
        this.address = parsedAddress.address;
        this.personal = parsedAddress.personal;
        this.encodedPersonal = parsedAddress.encodedPersonal;
    }

    public InternetAddress(final String address, final String personal) throws UnsupportedEncodingException {
        this(address, personal, null);
    }

    public InternetAddress(final String address, final String personal, final String charset) throws UnsupportedEncodingException {
        this.address = address;
        setPersonal(personal, charset);
    }

    /**
     * Clone this object.
     *
     * @return a copy of this object as created by Object.clone()
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new Error();
        }
    }

    /**
     * Return the type of this address.
     *
     * @return the type of this address; always "rfc822"
     */
    @Override
    public String getType() {
        return "rfc822";
    }

    /**
     * Set the address.
     * No validation is performed; validate() can be used to check if it is valid.
     *
     * @param address the address to set
     */
    public void setAddress(final String address) {
        this.address = address;
    }

    /**
     * Set the personal name.
     * The name is first checked to see if it can be encoded; if this fails then an
     * UnsupportedEncodingException is thrown and no fields are modified.
     *
     * @param name    the new personal name
     * @param charset the charset to use; see {@link MimeUtility#encodeWord(String, String, String) MimeUtilityencodeWord}
     * @throws UnsupportedEncodingException if the name cannot be encoded
     */
    public void setPersonal(final String name, final String charset) throws UnsupportedEncodingException {
        personal = name;
        if (name != null) {
            encodedPersonal = MimeUtility.encodeWord(name, charset, null);
        }
        else {
            encodedPersonal = null;
        }
    }

    /**
     * Set the personal name.
     * The name is first checked to see if it can be encoded using {@link MimeUtility#encodeWord(String)}; if this fails then an
     * UnsupportedEncodingException is thrown and no fields are modified.
     *
     * @param name the new personal name
     * @throws UnsupportedEncodingException if the name cannot be encoded
     */
    public void setPersonal(final String name) throws UnsupportedEncodingException {
        personal = name;
        if (name != null) {
            encodedPersonal = MimeUtility.encodeWord(name);
        }
        else {
            encodedPersonal = null;
        }
    }

    /**
     * Return the address.
     *
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Return the personal name.
     * If the personal field is null, then an attempt is made to decode the encodedPersonal
     * field using {@link MimeUtility#decodeWord(String)}; if this is sucessful, then
     * the personal field is updated with that value and returned; if there is a problem
     * decoding the text then the raw value from encodedPersonal is returned.
     *
     * @return the personal name
     */
    public String getPersonal() {
        if (personal == null && encodedPersonal != null) {
            try {
                personal = MimeUtility.decodeWord(encodedPersonal);
            } catch (final ParseException e) {
                return encodedPersonal;
            } catch (final UnsupportedEncodingException e) {
                return encodedPersonal;
            }
        }
        return personal;
    }

    /**
     * Return the encoded form of the personal name.
     * If the encodedPersonal field is null, then an attempt is made to encode the
     * personal field using {@link MimeUtility#encodeWord(String)}; if this is
     * successful then the encodedPersonal field is updated with that value and returned;
     * if there is a problem encoding the text then null is returned.
     *
     * @return the encoded form of the personal name
     */
    private String getEncodedPersonal() {
        if (encodedPersonal == null && personal != null) {
            try {
                encodedPersonal = MimeUtility.encodeWord(personal);
            } catch (final UnsupportedEncodingException e) {
                // as we could not encode this, return null
                return null;
            }
        }
        return encodedPersonal;
    }


    /**
     * Return a string representation of this address using only US-ASCII characters.
     *
     * @return a string representation of this address
     */
    @Override
    public String toString() {
        // group addresses are always returned without modification.
        if (isGroup()) {
            return address;
        }

        // if we have personal information, then we need to return this in the route-addr form:
        // "personal <address>".  If there is no personal information, then we typically return
        // the address without the angle brackets.  However, if the address contains anything other
        // than atoms, '@', and '.' (e.g., uses domain literals, has specified routes, or uses
        // quoted strings in the local-part), we bracket the address.
        final String p = getEncodedPersonal();
        if (p == null) {
            return formatAddress(address);
        }
        else {
            final StringBuffer buf = new StringBuffer(p.length() + 8 + address.length() + 3);
            buf.append(AddressParser.quoteString(p));
            buf.append(" <").append(address).append(">");
            return buf.toString();
        }
    }

    /**
     * Check the form of an address, and enclose it within brackets
     * if they are required for this address form.
     *
     * @param a      The source address.
     *
     * @return A formatted address, which can be the original address string.
     */
    private String formatAddress(final String a)
    {
        // this could be a group address....we don't muck with those.
        if (address.endsWith(";") && address.indexOf(":") > 0) {
            return address;
        }

        if (AddressParser.containsCharacters(a, "()<>,;:\"[]")) {
            final StringBuffer buf = new StringBuffer(address.length() + 3);
            buf.append("<").append(address).append(">");
            return buf.toString();
        }
        return address;
    }

    /**
     * Return a string representation of this address using Unicode characters.
     *
     * @return a string representation of this address
     */
    public String toUnicodeString() {
        // group addresses are always returned without modification.
        if (isGroup()) {
            return address;
        }

        // if we have personal information, then we need to return this in the route-addr form:
        // "personal <address>".  If there is no personal information, then we typically return
        // the address without the angle brackets.  However, if the address contains anything other
        // than atoms, '@', and '.' (e.g., uses domain literals, has specified routes, or uses
        // quoted strings in the local-part), we bracket the address.

        // NB:  The difference between toString() and toUnicodeString() is the use of getPersonal()
        // vs. getEncodedPersonal() for the personal portion.  If the personal information contains only
        // ASCII-7 characters, these are the same.
        final String p = getPersonal();
        if (p == null) {
            return formatAddress(address);
        }
        else {
            final StringBuffer buf = new StringBuffer(p.length() + 8 + address.length() + 3);
            buf.append(AddressParser.quoteString(p));
            buf.append(" <").append(address).append(">");
            return buf.toString();
        }
    }

    /**
     * Compares two addresses for equality.
     * We define this as true if the other object is an InternetAddress
     * and the two values returned by getAddress() are equal in a
     * case-insensitive comparison.
     *
     * @param o the other object
     * @return true if the addresses are the same
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
			return true;
		}
        if (!(o instanceof InternetAddress)) {
			return false;
		}

        final InternetAddress other = (InternetAddress) o;
        final String myAddress = getAddress();
        return myAddress == null ? (other.getAddress() == null) : myAddress.equalsIgnoreCase(other.getAddress());
    }

    /**
     * Return the hashCode for this address.
     * We define this to be the hashCode of the address after conversion to lowercase.
     *
     * @return a hashCode for this address
     */
    @Override
    public int hashCode() {
        return (address == null) ? 0 : address.toLowerCase().hashCode();
    }

    /**
     * Return true is this address is an RFC822 group address in the format
     * <code>phrase ":" [#mailbox] ";"</code>.
     * We check this by using the presense of a ':' character in the address, and a
     * ';' as the very last character.
     *
     * @return true is this address represents a group
     */
    public boolean isGroup() {
        if (address == null) {
            return false;
        }

        return address.endsWith(";") && address.indexOf(":") > 0;
    }

    /**
     * Return the members of a group address.
     *
     * If strict is true and the address does not contain an initial phrase then an AddressException is thrown.
     * Otherwise the phrase is skipped and the remainder of the address is checked to see if it is a group.
     * If it is, the content and strict flag are passed to parseHeader to extract the list of addresses;
     * if it is not a group then null is returned.
     *
     * @param strict whether strict RFC822 checking should be performed
     * @return an array of InternetAddress objects for the group members, or null if this address is not a group
     * @throws AddressException if there was a problem parsing the header
     */
    public InternetAddress[] getGroup(final boolean strict) throws AddressException {
        if (address == null) {
            return null;
        }

        // create an address parser and use it to extract the group information.
        final AddressParser parser = new AddressParser(address, strict ? AddressParser.STRICT : AddressParser.NONSTRICT);
        return parser.extractGroupList();
    }

    /**
     * Return an InternetAddress representing the current user.
     * <P/>
     * If session is not null, we first look for an address specified in its
     * "mail.from" property; if this is not set, we look at its "mail.user"
     * and "mail.host" properties and if both are not null then an address of
     * the form "${mail.user}@${mail.host}" is created.
     * If this fails to give an address, then an attempt is made to create
     * an address by combining the value of the "user.name" System property
     * with the value returned from InetAddress.getLocalHost().getHostName().
     * Any SecurityException raised accessing the system property or any
     * UnknownHostException raised getting the hostname are ignored.
     * <P/>
     * Finally, an attempt is made to convert the value obtained above to
     * an InternetAddress. If this fails, then null is returned.
     *
     * @param session used to obtain mail properties
     * @return an InternetAddress for the current user, or null if it cannot be determined
     */
    public static InternetAddress getLocalAddress(final Session session) {
        String host = null;
        String user = null;

        // ok, we have several steps for resolving this.  To start with, we could have a from address
        // configured already, which will be a full InternetAddress string.  If we don't have that, then
        // we need to resolve a user and host to compose an address from.
        if (session != null) {
            final String address = session.getProperty("mail.from");
            // if we got this, we can skip out now
            if (address != null) {
                try {
                    return new InternetAddress(address);
                } catch (final AddressException e) {
                    // invalid address on the from...treat this as an error and return null.
                    return null;
                }
            }

            // now try for user and host information.  We have both session and system properties to check here.
            // we'll just handle the session ones here, and check the system ones below if we're missing information.
            user = session.getProperty("mail.user");
            host = session.getProperty("mail.host");
        }

        try {

            // if either user or host is null, then we check non-session sources for the information.
            if (user == null) {
                user = System.getProperty("user.name");
            }

            if (host == null) {
                host = InetAddress.getLocalHost().getHostName();
            }

            if (user != null && host != null) {
                // if we have both a user and host, we can create a local address
                return new InternetAddress(user + '@' + host);
            }
        } catch (final AddressException e) {
            // ignore
        } catch (final UnknownHostException e) {
            // ignore
        } catch (final SecurityException e) {
            // ignore
        }
        return null;
    }

    /**
     * Convert the supplied addresses into a single String of comma-separated text as
     * produced by {@link InternetAddress#toString() toString()}.
     * No line-break detection is performed.
     *
     * @param addresses the array of addresses to convert
     * @return a one-line String of comma-separated addresses
     */
    public static String toString(final Address[] addresses) {
        return toString(addresses, 0);
    }

    /**
     * Convert the supplies addresses into a String of comma-separated text,
     * inserting line-breaks between addresses as needed to restrict the line
     * length to 72 characters. Splits will only be introduced between addresses
     * so an address longer than 71 characters will still be placed on a single
     * line.
     *
     * @param addresses the array of addresses to convert
     * @param used      the starting column
     * @return a String of comma-separated addresses with optional line breaks
     */
    public static String toString(final Address[] addresses, int used) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        if (addresses.length == 1) {
            String s = addresses[0].toString();
            if (used + s.length() > 72) {
                s = "\r\n  " + s;
            }
            return s;
        } else {
            final StringBuffer buf = new StringBuffer(addresses.length * 32);
            for (int i = 0; i < addresses.length; i++) {
                final String s = addresses[i].toString();
                if (i == 0) {
                    if (used + s.length() + 1 > 72) {
                        buf.append("\r\n  ");
                        used = 2;
                    }
                } else {
                    if (used + s.length() + 1 > 72) {
                        buf.append(",\r\n  ");
                        used = 2;
                    } else {
                        buf.append(", ");
                        used += 2;
                    }
                }
                buf.append(s);
                used += s.length();
            }
            return buf.toString();
        }
    }

    /**
     * Parse addresses out of the string with basic checking.
     *
     * @param addresses the addresses to parse
     * @return an array of InternetAddresses parsed from the string
     * @throws AddressException if addresses checking fails
     */
    public static InternetAddress[] parse(final String addresses) throws AddressException {
        return parse(addresses, true);
    }

    /**
     * Parse addresses out of the string.
     *
     * @param addresses the addresses to parse
     * @param strict if true perform detailed checking, if false just perform basic checking
     * @return an array of InternetAddresses parsed from the string
     * @throws AddressException if address checking fails
     */
    public static InternetAddress[] parse(final String addresses, final boolean strict) throws AddressException {
        return parse(addresses, strict ? AddressParser.STRICT : AddressParser.NONSTRICT);
    }

    /**
     * Parse addresses out of the string.
     *
     * @param addresses the addresses to parse
     * @param strict if true perform detailed checking, if false perform little checking
     * @return an array of InternetAddresses parsed from the string
     * @throws AddressException if address checking fails
     */
    public static InternetAddress[] parseHeader(final String addresses, final boolean strict) throws AddressException {
        return parse(addresses, strict ? AddressParser.STRICT : AddressParser.PARSE_HEADER);
    }

    /**
     * Parse addresses with increasing degrees of RFC822 compliance checking.
     *
     * @param addresses the string to parse
     * @param level     The required strictness level.
     *
     * @return an array of InternetAddresses parsed from the string
     * @throws AddressException
     *                if address checking fails
     */
    private static InternetAddress[] parse(final String addresses, final int level) throws AddressException {
        // create a parser and have it extract the list using the requested strictness leve.
        final AddressParser parser = new AddressParser(addresses, level);
        return parser.parseAddressList();
    }

    /**
     * Validate the address portion of an internet address to ensure
     * validity.   Throws an AddressException if any validity
     * problems are encountered.
     *
     * @exception AddressException
     */
    public void validate() throws AddressException {

        // create a parser using the strictest validation level.
        final AddressParser parser = new AddressParser(formatAddress(address), AddressParser.STRICT);
        parser.validateAddress();
    }

    /**
     * Convert the given array of InternetAddress objects into
     * a comma separated sequence of address strings. The
     * resulting string contains Unicode characters. <p>
     *
     * @param addresses	array of InternetAddress objects
     * @exception 	ClassCastException if any address object in the
     *			given array is not an InternetAddress object.
     *			Note that this is a RuntimeException.
     * @return		comma separated string of addresses
     * @since		JavaMail 1.6
     */
    public static String toUnicodeString(Address[] addresses) {
        return toUnicodeString(addresses, 0);
    }

    /**
     * Convert the given array of InternetAddress objects into
     * a comma separated sequence of address strings. The
     * resulting string contains Unicode characters. <p>
     *
     * The 'used' parameter specifies the number of character positions
     * already taken up in the field into which the resulting address
     * sequence string is to be inserted. It is used to determine the
     * line-break positions in the resulting address sequence string.
     *
     * @param addresses	array of InternetAddress objects
     * @param used	number of character positions already used, in
     *			the field into which the address string is to
     *			be inserted.
     * @exception 	ClassCastException if any address object in the
     *			given array is not an InternetAddress object.
     *			Note that this is a RuntimeException.
     * @return		comma separated string of addresses
     * @Since JavaMail 1.6
     */
    public static String toUnicodeString(Address[] addresses, int used) {

        if (addresses == null || addresses.length == 0) {
            return null;
        }

        boolean sawNonAsciiCharacters = false;

        if (addresses.length == 1) {

            String converted = ((InternetAddress)addresses[0]).toUnicodeString();
            if (MimeUtility.verifyAscii(converted) != MimeUtility.ALL_ASCII){
                sawNonAsciiCharacters = true;
                converted = new String(converted.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

            }

            if (used + converted.length() > 72) {
                converted = "\r\n  " + converted;
            }

            if(sawNonAsciiCharacters){
                return new String(converted.getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8);
            }

            return converted;
        } else {
            final StringBuffer buf = new StringBuffer(addresses.length * 32);
            for (int i = 0; i < addresses.length; i++) {

                String converted = ((InternetAddress)addresses[0]).toUnicodeString();

                if (MimeUtility.verifyAscii(converted) != MimeUtility.ALL_ASCII){
                    sawNonAsciiCharacters = true;
                    converted = new String(converted.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

                }

                if (i == 0) {
                    if (used + converted.length() + 1 > 72) {
                        buf.append("\r\n  ");
                        used = 2;
                    }
                } else {
                    if (used + converted.length() + 1 > 72) {
                        buf.append(",\r\n  ");
                        used = 2;
                    } else {
                        buf.append(", ");
                        used += 2;
                    }
                }
                buf.append(converted);
                used += converted.length();
            }

            String finalString = buf.toString();

            if(sawNonAsciiCharacters){
                return new String(finalString.getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8);
            }
            return finalString;
        }
    }
}
