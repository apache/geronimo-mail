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

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import jakarta.activation.ActivationDataFlavor;
import jakarta.activation.DataContentHandler;
import jakarta.activation.DataSource;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.internet.ParseException;

public class TextHandler implements DataContentHandler {
    /**
     * Field dataFlavor
     */
    ActivationDataFlavor dataFlavor;

    public TextHandler(){
        dataFlavor = new ActivationDataFlavor(java.lang.String.class, "text/plain", "Text String");
    }

    /**
     * Constructor TextHandler
     *
     * @param dataFlavor
     */
    public TextHandler(final ActivationDataFlavor dataFlavor) {
        this.dataFlavor = dataFlavor;
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
    @Override
    public Object getTransferData(final ActivationDataFlavor dataflavor, final DataSource datasource) throws IOException {
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
        final InputStream is = datasource.getInputStream();
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        int count;
        final byte[] buffer = new byte[1000];

        try {
            while ((count = is.read(buffer, 0, buffer.length)) > 0) {
                os.write(buffer, 0, count);
            }
        } finally {
            is.close();
        }
        try {
            return os.toString(getCharSet(datasource.getContentType()));
        } catch (final ParseException e) {
            throw new UnsupportedEncodingException(e.getMessage());
        }
    }


    /**
     * Write an object of "our" type out to the provided
     * output stream.  The content type might modify the
     * result based on the content type parameters.
     *
     * @param object The object to write.
     * @param contentType
     *               The content mime type, including parameters.
     * @param outputstream
     *               The target output stream.
     *
     * @throws IOException
     */
    public void writeTo(final Object object, final String contentType, final OutputStream outputstream)
            throws IOException {
        OutputStreamWriter os;
        try {
            final String charset = getCharSet(contentType);
            os = new OutputStreamWriter(outputstream, charset);
        } catch (final Exception ex) {
            throw new UnsupportedEncodingException(ex.toString());
        }
        final String content = (String) object;
        os.write(content, 0, content.length());
        os.flush();
    }

    /**
     * get the character set from content type
     * @param contentType
     * @return
     * @throws ParseException
     */
    protected String getCharSet(final String contentType) throws ParseException {
        final ContentType type = new ContentType(contentType);
        String charset = type.getParameter("charset");
        if (charset == null) {
            charset = "us-ascii";
        }
        return MimeUtility.javaCharset(charset);
    }
}
