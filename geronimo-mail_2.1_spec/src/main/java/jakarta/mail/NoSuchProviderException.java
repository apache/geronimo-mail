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

/**
 * @version $Rev$ $Date$
 */
public class NoSuchProviderException extends MessagingException {
	
	private static final long serialVersionUID = 8058319293154708827L;
	
    public NoSuchProviderException() {
        super();
    }

    public NoSuchProviderException(final String message) {
        super(message);
    }
    
    /**
     * Constructs a NoSuchProviderException with the specified
     * detail message and embedded exception.  The exception is chained
     * to this exception.
     *
     * @param message    The detailed error message
     * @param e      The embedded exception
     * @since        JavaMail 1.5
     */
    public NoSuchProviderException(final String message, final Exception e) {
        super(message, e);
    }
}
