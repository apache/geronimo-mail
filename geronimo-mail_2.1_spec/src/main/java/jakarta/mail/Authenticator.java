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

import java.net.InetAddress;

/**
 * @version $Rev$ $Date$
 */
public abstract class Authenticator {
    private InetAddress host;
    private int port;
    private String prompt;
    private String protocol;
    private String username;

    synchronized PasswordAuthentication authenticate(final InetAddress host, final int port, final String protocol, final String prompt, final String username) {
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.prompt = prompt;
        this.username = username;
        return getPasswordAuthentication();
    }

    protected final String getDefaultUserName() {
        return username;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return null;
    }

    protected final int getRequestingPort() {
        return port;
    }

    protected final String getRequestingPrompt() {
        return prompt;
    }

    protected final String getRequestingProtocol() {
        return protocol;
    }

    protected final InetAddress getRequestingSite() {
        return host;
    }
}
