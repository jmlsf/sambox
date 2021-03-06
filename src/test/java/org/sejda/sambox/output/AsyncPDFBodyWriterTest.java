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
package org.sejda.sambox.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * @author Andrea Vacondio
 *
 */
public class AsyncPDFBodyWriterTest
{

    private IndirectObjectsWriter writer;
    private AsyncPDFBodyWriter victim;
    private PDDocument document;
    private PDFWriteContext context;

    @Before
    public void setUp()
    {
        context = new PDFWriteContext(null,
                WriteOption.COMPRESS_STREAMS);
        writer = mock(IndirectObjectsWriter.class);
        victim = new AsyncPDFBodyWriter(writer, context);
        document = new PDDocument();
        document.getDocumentInformation().setAuthor("Chuck Norris");
        COSDictionary someDic = new COSDictionary();
        someDic.setInt(COSName.SIZE, 4);
        document.getDocument().getCatalog().setItem(COSName.G, someDic);
        document.getDocument().getCatalog().setItem(COSName.H, someDic);
    }

    @After
    public void tearDown()
    {
        IOUtils.closeQuietly(victim);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new AsyncPDFBodyWriter(null, null);
    }

    @Test
    public void writeBodyReusesDictionaryRef() throws IOException
    {
        victim.write(document.getDocument());
        assertEquals(document.getDocument().getCatalog().getItem(COSName.G), document.getDocument()
                .getCatalog().getItem(COSName.H));
        assertTrue(context.hasIndirectReferenceFor(document.getDocument().getCatalog().getItem(COSName.G)));
        verify(writer, timeout(1000).times(4)).writeObjectIfNotWritten(any()); // catalog,info,pages,someDic
    }

    @Test(expected = IOException.class)
    public void asyncExceptionIsProcessed() throws IOException
    {
        doThrow(IOException.class).when(writer).writeObjectIfNotWritten(any());
        victim.write(document.getDocument());
    }

    @Test(expected = IllegalStateException.class)
    public void cantWriteToClosedWriter() throws IOException
    {
        victim.close();
        victim.write(document.getDocument());
    }

    @Test
    public void writeBodyExistingDocument() throws Exception
    {
        try (PDDocument document = PDFParser.parse(SeekableSources
                .inMemorySeekableSourceFrom(getClass()
.getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            victim.write(document.getDocument());
        }
        verify(writer, timeout(1000).times(8)).writeObjectIfNotWritten(any());
    }
}
