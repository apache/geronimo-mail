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

package org.apache.geronimo.mail.handlers;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.activation.ActivationDataFlavor;
import jakarta.activation.DataContentHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Message;
import jakarta.mail.MessageAware;
import jakarta.mail.MessageContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

public class MessageHandler implements DataContentHandler {
    /**
     * Field dataFlavor
     */
    ActivationDataFlavor dataFlavor;

    public MessageHandler(){
        dataFlavor = new ActivationDataFlavor(java.lang.String.class, "message/rfc822", "Text");
    }


    /**
     * Method getDF
     *
     * @return dataflavor
     */
    protected ActivationDataFlavor getDF() {
        return dataFlavor;
    }

    /**
     * Method getTransferDataFlavors
     *
     * @return dataflavors
     */
    public ActivationDataFlavor[] getTransferDataFlavors() {
        return (new ActivationDataFlavor[]{dataFlavor});
    }

    /**
     * Method getTransferData
     *
     * @param dataflavor
     * @param datasource
     * @return
     * @throws IOException
     */
    public Object getTransferData(final ActivationDataFlavor dataflavor, final DataSource datasource)
            throws IOException {
        if (getDF().equals(dataflavor)) {
            return getContent(datasource);
        }
        return null;
    }

    /**
     * Method getContent
     *
     * @param datasource
     * @return
     * @throws IOException
     */
    public Object getContent(final DataSource datasource) throws IOException {

        try {
            // if this is a proper message, it implements the MessageAware interface.  We need this to
            // get the associated session.
            if (datasource instanceof MessageAware) {
                final MessageContext context = ((MessageAware)datasource).getMessageContext();
                // construct a mime message instance from the stream, associating it with the
                // data source session.
                return new MimeMessage(context.getSession(), datasource.getInputStream());
            }
        } catch (final MessagingException e) {
            // we need to transform any exceptions into an IOException.
            throw new IOException("Exception writing MimeMultipart: " + e.toString());
        }
        return null;
    }

    /**
     * Method writeTo
     *
     * @param object
     * @param s
     * @param outputstream
     * @throws IOException
     */
    public void writeTo(final Object object, final String s, final OutputStream outputstream) throws IOException {
        // proper message type?
        if (object instanceof Message) {
            try {
                ((Message)object).writeTo(outputstream);
            } catch (final MessagingException e) {
                throw new IOException("Error parsing message: " + e.toString());
            }
        }
    }
}

