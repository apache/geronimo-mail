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

import jakarta.mail.event.AllEventTests;
import jakarta.mail.internet.AllInternetTests;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @version $Revision $ $Date$
 */
public class AllTests {
    public static Test suite() {
        final TestSuite suite = new TestSuite("Test for jakarta.mail");
        //$JUnit-BEGIN$
        suite.addTest(new TestSuite(FlagsTest.class));
        suite.addTest(new TestSuite(HeaderTest.class));
        suite.addTest(new TestSuite(MessagingExceptionTest.class));
        suite.addTest(new TestSuite(URLNameTest.class));
        suite.addTest(new TestSuite(PasswordAuthenticationTest.class));
        suite.addTest(AllEventTests.suite());
        suite.addTest(AllInternetTests.suite());
        //$JUnit-END$
        return suite;
    }
}
