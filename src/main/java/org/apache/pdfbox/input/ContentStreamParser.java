/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.input;

import static org.apache.pdfbox.contentstream.operator.Operator.BI_OPERATOR;
import static org.apache.pdfbox.contentstream.operator.Operator.ID_OPERATOR;
import static org.apache.pdfbox.util.CharUtils.isEOF;
import static org.apache.pdfbox.util.CharUtils.isWhitespace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.util.IOUtils;
/**
 * Component responsible for parsing a a content stream to extract operands and such.
 * 
 * @author Andrea Vacondio
 */
public class ContentStreamParser extends SourceReader
{
    private ContentStreamCOSParser cosParser;
    private List<Object> tokens = new ArrayList<>();

    public ContentStreamParser(PDContentStream stream) throws IOException
    {
        this(SeekableSources.inMemorySeekableSourceFrom(stream.getContents()));
    }

    public ContentStreamParser(SeekableSource source)
    {
        super(source);
        this.cosParser = new ContentStreamCOSParser(source());
    }

    /**
     * @return a list of tokens retrieved parsing the source this parser was created from.
     * @throws IOException
     */
    public List<Object> tokens() throws IOException
    {
        tokens.clear();
        Object token;
        while ((token = nextParsedToken()) != null)
        {
            tokens.add(token);
        }
        return Collections.unmodifiableList(tokens);
    }

    /**
     * @return the next token parsed from the content stream
     * @throws IOException
     */
    public Object nextParsedToken() throws IOException
    {
        skipSpaces();
        COSBase token = cosParser.nextParsedToken();
        if (token != null)
        {
            return token;
        }
        return nextOperator();
    }

    private Object nextOperator() throws IOException
    {
        if ('B' == (char) source().peek())
        {
            Operator operator = Operator.getOperator(readToken());
            if (BI_OPERATOR.equals(operator.getName()))
            {
                nextInlineImage(operator);
            }
            return operator;
        }
        return Optional.ofNullable(readToken()).filter(s -> s.length() > 0)
                .map(Operator::getOperator).orElse(null);

    }

    private void nextInlineImage(Operator operator) throws IOException
    {
        COSDictionary imageParams = new COSDictionary();
        operator.setImageParameters(imageParams);
        COSBase nextToken = null;
        while ((nextToken = cosParser.nextParsedToken()) instanceof COSName)
        {
            imageParams.setItem((COSName) nextToken, cosParser.nextParsedToken());
        }
        operator.setImageData(nextImageData());
    }

    /**
     * Reads data until it finds an "EI" operator followed by a whitespace.
     * 
     * @return the image data
     * @throws IOException
     */
    private byte[] nextImageData() throws IOException
    {
        skipSpaces();
        skipExpected(ID_OPERATOR);
        if (!isWhitespace(source().read()))
        {
            source().back();
        }
        ByteArrayOutputStream imageData = new ByteArrayOutputStream();
        int current;

        while ((current = source().read()) != -1)
        {
            long position = source().position();
            if ((current == 'E' && isEndOfImageFrom(position - 1))
                    || (isWhitespace(current) && isEndOfImageFrom(position)))
            {
                break;
            }
            imageData.write(current);
        }
        return imageData.toByteArray();
    }

    private boolean isEndOfImageFrom(long position) throws IOException
    {
        long currentPosition = source().position();
        try
        {
            source().position(position);
            int current = source().read();
            if (current == 'E')
            {
                current = source().read();
                return current == 'I' && (isWhitespace(source().peek()) || isEOF(source().peek()));
            }
            return false;
        }
        finally
        {
            source().position(currentPosition);
        }
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        IOUtils.closeQuietly(cosParser);
    }
}
