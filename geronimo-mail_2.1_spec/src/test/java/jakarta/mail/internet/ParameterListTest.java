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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
