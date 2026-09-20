/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geronimo.mail.authentication;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.geronimo.mail.util.ProtocolProperties;

public class AuthenticatorFactory {
    // the list of authentication mechanisms we have direct support for.  Others come from
    // SASL, if it's available.

    public static final String AUTHENTICATION_PLAIN = "PLAIN";
    public static final String AUTHENTICATION_LOGIN = "LOGIN";
    public static final String AUTHENTICATION_CRAMMD5 = "CRAM-MD5";
    public static final String AUTHENTICATION_DIGESTMD5 = "DIGEST-MD5";
    public static final String AUTHENTICATION_XOAUTH2 = "XOAUTH2";
    private static final String MAIL_SASL_MECHANISMS = "sasl.mechanisms";
    // mail.<protocol>.auth.mechanisms is what the reference implementation uses to restrict and order the
    // mechanisms to consider, and mail.<protocol>.auth.xoauth2.disable is how it enables XOAUTH2 without
    // going through SASL.  We honour both so that an application does not have to be rewritten to move
    // between the two implementations - see GERONIMO-6780.
    private static final String MAIL_AUTH_MECHANISMS = "auth.mechanisms";
    private static final String MAIL_XOAUTH2_DISABLE = "auth.xoauth2.disable";

    private static List getSaslMechanisms(ProtocolProperties props) {
        return parseMechanisms(props.getProperty(MAIL_SASL_MECHANISMS));
    }

    /**
     * Get the mechanisms the application has configured, from either the SASL-specific property or the
     * general one.  mail.&lt;protocol&gt;.sasl.mechanisms wins where both are set, because it is the more
     * specific of the two.
     *
     * @param props  The protocol properties.
     *
     * @return The configured mechanism names in upper case, empty if none were configured.
     */
    private static List getConfiguredMechanisms(ProtocolProperties props) {
        String mechList = props.getProperty(MAIL_SASL_MECHANISMS);
        if (mechList == null) {
            mechList = props.getProperty(MAIL_AUTH_MECHANISMS);
        }
        return parseMechanisms(mechList);
    }

    /**
     * Test whether XOAUTH2 may be selected.  Because it needs an access token rather than a password it
     * is never chosen on its own; the application enables it either by naming it in the mechanism list or
     * by clearing mail.&lt;protocol&gt;.auth.xoauth2.disable, which matches the reference implementation.
     *
     * @param props  The protocol properties.
     *
     * @return True if the application has asked for XOAUTH2.
     */
    public static boolean isXOAuth2Enabled(ProtocolProperties props) {
        if (getConfiguredMechanisms(props).contains(AUTHENTICATION_XOAUTH2)) {
            return true;
        }
        return !props.getBooleanProperty(MAIL_XOAUTH2_DISABLE, true);
    }

    private static List parseMechanisms(String mechList) {
        List mechanisms = new ArrayList();
        if (mechList != null) {
            // the mechanisms are a blank or comma-separated list
            StringTokenizer tokenizer = new StringTokenizer(mechList, " ,");

            while (tokenizer.hasMoreTokens()) {
                String mech = tokenizer.nextToken().toUpperCase();
                mechanisms.add(mech);
            }
        }
        return mechanisms;
    }

    static public ClientAuthenticator getAuthenticator(ProtocolProperties props, List mechanisms, String host, String username, String password, String authId, String realm)
    {
        // if the authorization id isn't given, then this is the same as the logged in user name.
        if (authId == null) {
            authId = username;
        }

        // if SASL is enabled, try getting a SASL authenticator first
        if (props.getBooleanProperty("sasl.enable", false)) {
            // we need to convert the mechanisms map into an array of strings for SASL.
            String [] mechs = (String [])mechanisms.toArray(new String[mechanisms.size()]);

            try {

                // need to try to load this using reflection since it has references to
                // the SASL API.  That's only available with 1.5 or later.
                Class authenticatorClass = null;

                //We obtain only the SASL mechanisms provided by the client properties
                final List saslMechanisms = getSaslMechanisms(props);

                if (saslMechanisms != null && saslMechanisms.contains(AUTHENTICATION_XOAUTH2)) {
                    authenticatorClass = Class.forName(
                        "org.apache.geronimo.mail.authentication.XOAUTH2Authenticator");
                } else {
                    authenticatorClass = Class.forName(
                        "org.apache.geronimo.mail.authentication.SASLAuthenticator");
                }

                Constructor c = authenticatorClass.getConstructor(new Class[] {
                    (new String[0]).getClass(),
                    Properties.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class
                });

                Object[] args = { mechs, props.getProperties(), props.getProtocol(), host, realm, authId, username, password };

                return (ClientAuthenticator)c.newInstance(args);
            } catch (Throwable e) {
                // Any exception is likely because we're running on 1.4 and can't use the Sasl API.
                // just ignore and use our fallback implementations.
            }
        }

        // XOAUTH2 needs no SASL support of its own, so it can be selected whether or not the SASL API is
        // in play.  It is only ever used when the application asked for it and the server offers it.
        if (isXOAuth2Enabled(props) && mechanisms.contains(AUTHENTICATION_XOAUTH2)) {
            try {
                return new XOAUTH2Authenticator(new String[] {AUTHENTICATION_XOAUTH2}, props.getProperties(),
                        props.getProtocol(), host, realm, authId, username, password);
            } catch (Throwable e) {
                // the authenticator only stores the credentials it was handed, so this is not expected;
                // fall through to the other mechanisms rather than failing the connection outright.
            }
        }

        // now go through the progression of mechanisms we support, from the
        // most secure to the least secure.

        if (mechanisms.contains(AUTHENTICATION_DIGESTMD5)) {
            return new DigestMD5Authenticator(host, username, password, realm);
        } else if (mechanisms.contains(AUTHENTICATION_CRAMMD5)) {
            return new CramMD5Authenticator(username, password);
        } else if (mechanisms.contains(AUTHENTICATION_LOGIN)) {
            return new LoginAuthenticator(username, password);
        } else if (mechanisms.contains(AUTHENTICATION_PLAIN)) {
            return new PlainAuthenticator(authId, username, password);
        } else {
            // can't find a mechanism we support in common
            return null;
        }
    }
}

