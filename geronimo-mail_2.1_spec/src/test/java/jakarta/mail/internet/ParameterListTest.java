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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @version $Rev$ $Date$
 */
public class ParameterListTest {

    @Test
    public void testParameters() throws ParseException {
        final ParameterList list =
            new ParameterList(";thing=value;thong=vulue;thung=git");
        assertEquals("value", list.get("thing"));
        assertEquals("vulue", list.get("thong"));
        assertEquals("git", list.get("thung"));
    }

    @Test
    public void testQuotedParameter() throws ParseException {
        final ParameterList list = new ParameterList(";foo=one;bar=\"two\"");
        assertEquals("one", list.get("foo"));
        assertEquals("two", list.get("bar"));
    }

    @Test
    public void testQuotedParameterSet() throws ParseException {
        final ParameterList list = new ParameterList();
        list.set("foo", "one");
        list.set("bar", "\"two\"");
        assertEquals("one", list.get("foo"));
        assertEquals("\"two\"", list.get("bar"));
    }

    @Test
    public void testMultisegmentParameter() throws ParseException {
        final ParameterList list = new ParameterList(";foo*0=one;foo*1=\"two\"");
        assertEquals("onetwo", list.get("foo"));
    }

    @Test
    public void testMultisegmentParameterSet() throws ParseException {
        final ParameterList list = new ParameterList();
        list.set("foo*0", "one");
        list.set("foo*1", "\"two\"");
        list.combineSegments();
        assertEquals("one\"two\"", list.get("foo"));
    }

    @Test
    public void testMultisegmentParameterMoreSet() throws ParseException {
        final ParameterList list = new ParameterList();
        list.set("foo*0", "one");
        list.set("foo*1", "two");
        list.set("foo*2", "three");
        list.set("bar", "four");
        list.set("test2*0", "seven");
        list.set("test*1", "six");
        list.set("test*0", "five");
        list.combineSegments();
        assertEquals("onetwothree", list.get("foo"));
        assertEquals("four", list.get("bar"));
        assertEquals("fivesix", list.get("test"));
        assertEquals("seven", list.get("test2"));
    }

    @Test
    public void testMultisegmentParameterMore() throws ParseException {
        final ParameterList list = new ParameterList(";foo*0=one;foo*1=two;foo*2=three;bar=four;test2*0=seven;test*1=six;test*0=five");
        assertEquals("onetwothree", list.get("foo"));
        assertEquals("four", list.get("bar"));
        assertEquals("fivesix", list.get("test"));
        assertEquals("seven", list.get("test2"));
    }

    @Test
    public void testMultisegmentParameterEncodedMore() throws ParseException {
        final String value = " '*% abc \u0081\u0082\r\n\t";
        final String encodedTest = "UTF-8''%20%27%2A%25%20abc%20%C2%81%C2%82%0D%0A%09";
        final ParameterList list = new ParameterList(";foo*0=one;foo*1=two;foo*2*="+encodedTest+";bar=four;test2*0=seven;test*1=six;test*0=five");
        assertEquals("onetwo"+value, list.get("foo"));
        assertEquals("four", list.get("bar"));
        assertEquals("fivesix", list.get("test"));
        assertEquals("seven", list.get("test2"));
    }

    @Test
    public void testMultisegmentParameterEncodedMoreFail() throws ParseException {
        //final String value = " '*% abc \u0081\u0082\r\n\t";
        final String encodedTest = "UTF-8''%20%27%2A%25%20abc%20%C2%81%C2%82%0D%0A%09";
        final ParameterList list = new ParameterList(";foo*0=one;foo*1=two;foo*2="+encodedTest+";bar=four;test2*0=seven;test*1=six;test*0=five");
        assertEquals("onetwo"+encodedTest, list.get("foo"));
        assertEquals("four", list.get("bar"));
        assertEquals("fivesix", list.get("test"));
        assertEquals("seven", list.get("test2"));
    }

    @Test
    public void testMultisegmentParameterMoreMixedEncodedSet() throws ParseException {
        
        final String value = " '*% abc \u0081\u0082\r\n\t";
        final String encodedTest = "UTF-8''%20%27%2A%25%20abc%20%C2%81%C2%82%0D%0A%09";
        
        final ParameterList list = new ParameterList();
        list.set("foo*0", "one");
        list.set("foo*1", "two");
        list.set("foo*2*", encodedTest, "UTF-8");
        list.set("bar", "four");
        list.set("test2*0", "seven");
        list.set("test*1", "six");
        list.set("test*0", "five");
        list.set("test3*", encodedTest, "UTF-8");
        list.combineSegments();
        assertEquals("onetwo"+value, list.get("foo"));
        assertEquals("four", list.get("bar"));
        assertEquals("fivesix", list.get("test"));
        assertEquals("seven", list.get("test2"));
        //assertEquals(value, list.get("test3"));
    }
    
    public void emptyParameter() throws ParseException {
        final ParameterList list = new ParameterList("");
        assertEquals(0, list.size());
    }

    @Test
    public void testEncodeDecode() throws Exception {

        //since JavaMail 1.5 encodeparameters/decodeparameters are enabled by default
        //System.setProperty("mail.mime.encodeparameters", "true");
        //System.setProperty("mail.mime.decodeparameters", "true");

        final String value = " '*% abc \u0081\u0082\r\n\t";
        final String encodedTest = "; one*=UTF-8''%20%27%2A%25%20abc%20%C2%81%C2%82%0D%0A%09";

        final ParameterList list = new ParameterList();
        list.set("one", value, "UTF-8");

        assertEquals(value, list.get("one"));

        final String encoded = list.toString();

        assertEquals(encoded, encodedTest);

        final ParameterList list2 = new ParameterList(encoded);
        assertEquals(value, list.get("one"));
        assertEquals(list2.toString(), encodedTest);
    }

    // header reported in GERONIMO-6851: the type parameter value contains an
    // unquoted '/' which is a MIME special character
    private static final String UNQUOTED_SPECIALS_HEADER =
        "multipart/related; type=text/html; boundary=MErelboundary-32775405-3-1674841212";

    @AfterEach
    public void clearParametersStrictProperty() {
        System.clearProperty("mail.mime.parameters.strict");
    }

    @Test
    public void testLenientParsingOfUnquotedSpecials() throws Exception {
        System.setProperty("mail.mime.parameters.strict", "false");

        // the reporter's exact header must now parse
        final ContentType contentType = new ContentType(UNQUOTED_SPECIALS_HEADER);
        assertEquals("multipart", contentType.getPrimaryType());
        assertEquals("related", contentType.getSubType());
        assertEquals("text/html", contentType.getParameter("type"));
        assertEquals("MErelboundary-32775405-3-1674841212", contentType.getParameter("boundary"));

        // and isMimeType on a part carrying that raw header must work instead of
        // throwing a ParseException
        final InternetHeaders headers = new InternetHeaders();
        headers.addHeader("Content-Type", UNQUOTED_SPECIALS_HEADER);
        final MimeBodyPart part = new MimeBodyPart(headers, new byte[0]);
        assertFalse(part.isMimeType("text/plain"));
        assertTrue(part.isMimeType("multipart/related"));
    }

    @Test
    public void testStrictDefaultRejectsUnquotedSpecials() {
        // without the property set, strict parsing is the default and the
        // reporter's header must still be rejected
        assertThrows(ParseException.class, () -> new ContentType(UNQUOTED_SPECIALS_HEADER));
        assertThrows(ParseException.class, () ->
            new ParameterList("; type=text/html; boundary=MErelboundary-32775405-3-1674841212"));

        // an explicit "true" behaves the same way
        System.setProperty("mail.mime.parameters.strict", "true");
        assertThrows(ParseException.class, () -> new ContentType(UNQUOTED_SPECIALS_HEADER));
    }

    @Test
    public void testLenientParsingKeepsWellFormedValues() throws Exception {
        System.setProperty("mail.mime.parameters.strict", "false");

        // quoted values (including embedded ';' and whitespace) are untouched
        final ParameterList quoted = new ParameterList("; foo=\"a;b c\"; bar=plain");
        assertEquals("a;b c", quoted.get("foo"));
        assertEquals("plain", quoted.get("bar"));

        // RFC 2231 encoded parameters still decode
        final ParameterList encoded = new ParameterList("; title*=us-ascii'en-us'This%20is%20fun; charset=us-ascii");
        assertEquals("This is fun", encoded.get("title"));
        assertEquals("us-ascii", encoded.get("charset"));

        // RFC 2231 multi-segment parameters still combine
        final ParameterList multi = new ParameterList(";foo*0=one;foo*1=\"two\"");
        assertEquals("onetwo", multi.get("foo"));

        // an unquoted value with whitespace runs to the next ';' and is trimmed
        final ParameterList spaces = new ParameterList("; name=hello world stuff ; next=x");
        assertEquals("hello world stuff", spaces.get("name"));
        assertEquals("x", spaces.get("next"));
    }

    @Test
    public void testStrictDecodeRejectsBadHex() {
        System.setProperty("mail.mime.decodeparameters", "true");
        System.setProperty("mail.mime.decodeparameters.strict", "true");
        try {
            // "%2x" is not a valid hex escape, so strict decoding must raise a ParseException
            assertThrows(ParseException.class, () ->
                new ParameterList("; filename*=us-ascii'en-us'This%2xis%20%2A%2A%2Afun%2A%2A%2A"));
            // a valid encoding still parses in strict mode
            final ParameterList ok = new ParameterList("; filename*=us-ascii'en-us'This%20is%20fun");
            assertEquals("This is fun", ok.get("filename"));
        } catch (final ParseException e) {
            throw new AssertionError("valid RFC2231 value failed to parse", e);
        } finally {
            System.clearProperty("mail.mime.decodeparameters");
            System.clearProperty("mail.mime.decodeparameters.strict");
        }
    }
}
