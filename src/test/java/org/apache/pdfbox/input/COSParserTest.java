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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.util.Charsets;
import org.junit.After;
import org.junit.Test;
import org.sejda.util.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class COSParserTest
{

    private COSParser victim;

    @After
    public void tearDown() throws Exception
    {
        IOUtils.close(victim);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullArgument()
    {
        new COSParser(null);
    }

    @Test
    public void nextNull() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("53 0 obj <</key null>>".getBytes()));
        victim.position(16);
        assertEquals(COSNull.NULL, victim.nextNull());
    }

    @Test(expected = IOException.class)
    public void nextNullFailing() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("53 0 obj <</key value>>".getBytes()));
        victim.position(16);
        victim.nextNull();
    }

    @Test
    public void nextBooleanTrue() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("53 0 obj <</key true>>".getBytes()));
        victim.position(16);
        assertEquals(COSBoolean.TRUE, victim.nextBoolean());
    }

    @Test
    public void nextBooleanFalse() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("53 0 obj <</key false>>".getBytes()));
        victim.position(16);
        assertEquals(COSBoolean.FALSE, victim.nextBoolean());
    }

    @Test(expected = IOException.class)
    public void nextBooleanFailing() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("53 0 obj <</key value>>".getBytes()));
        victim.position(16);
        victim.nextBoolean();
    }

    @Test
    public void nextName() throws IOException
    {
        victim = new COSParser(
                inMemorySeekableSourceFrom("53 0 obj <</Creator me>>".getBytes()));
        victim.position(11);
        assertEquals(COSName.CREATOR, victim.nextName());
    }

    @Test
    public void nextCustomName() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("53 0 obj <</Chuck me>>".getBytes()));
        victim.position(11);
        assertEquals(COSName.getPDFName("Chuck"), victim.nextName());
    }

    @Test
    public void nextLiteral() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("(Chuck Norris)".getBytes()));
        assertEquals(COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1)),
                victim.nextLiteralString());
    }

    @Test
    public void nextHexString() throws IOException
    {
        victim = new COSParser(
                inMemorySeekableSourceFrom("<436875636B204E6f72726973>".getBytes()));
        COSString expected = COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1));
        expected.setForceHexForm(true);
        assertEquals(expected, victim.nextHexadecimalString());
    }

    @Test
    public void nextString() throws IOException
    {
        victim = new COSParser(
                inMemorySeekableSourceFrom("(Chuck Norris) <436875636B204E6f72726973>".getBytes()));
        assertEquals(COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1)),
                victim.nextString());
        victim.skipSpaces();
        COSString expected = COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1));
        expected.setForceHexForm(true);
        assertEquals(expected, victim.nextString());
    }

    @Test(expected = IOException.class)
    public void nextHexStringFailing() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("53 0 obj <</Chuck me>>".getBytes()));
        victim.nextString();
    }

    @Test
    public void nextNumber() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("1".getBytes()));
        assertEquals(COSInteger.ONE, victim.nextNumber());
    }

    @Test
    public void nextArray() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("[10 (A String)]".getBytes()));
        COSArray result = victim.nextArray();
        assertEquals(10, result.getInt(0));
        assertEquals(COSString.newInstance("A String".getBytes(Charsets.ISO_8859_1)), result.get(1));
    }

    @Test
    public void nextArrayWorkaroundEndobj() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("[10 (A String) endobj".getBytes()));
        COSArray result = victim.nextArray();
        assertEquals(10, result.getInt(0));
        assertEquals(COSString.newInstance("A String".getBytes(Charsets.ISO_8859_1)), result.get(1));
    }

    @Test
    public void nextArrayWorkaroundEndstream() throws IOException
    {
        victim = new COSParser(
                inMemorySeekableSourceFrom("[10 (A String) endstream".getBytes()));
        COSArray result = victim.nextArray();
        assertEquals(10, result.getInt(0));
        assertEquals(COSString.newInstance("A String".getBytes(Charsets.ISO_8859_1)), result.get(1));
    }

    @Test
    public void nextArraySkipsInvalid() throws IOException
    {
        victim = new COSParser(
                inMemorySeekableSourceFrom("[10 (A String) invalid (valid)]".getBytes()));
        COSArray result = victim.nextArray();
        assertEquals(10, result.getInt(0));
        assertEquals(COSString.newInstance("A String".getBytes(Charsets.ISO_8859_1)), result.get(1));
        assertEquals(COSString.newInstance("valid".getBytes(Charsets.ISO_8859_1)), result.get(2));
    }

    @Test(expected = IOException.class)
    public void nextArrayTrunkatedEOF() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("[10 (A String)".getBytes()));
        victim.nextArray();
    }

    @Test
    public void nextDictionary() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("<</R 10>>".getBytes()));
        COSDictionary result = victim.nextDictionary();
        assertEquals(10, result.getInt(COSName.R));
    }

    @Test
    public void nextDictionarySpaces() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("<< /R 10 >>".getBytes()));
        COSDictionary result = victim.nextDictionary();
        assertEquals(10, result.getInt(COSName.R));
    }

    @Test
    public void nextDictionaryWorkaroundEndobj() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("<< /R 10  endobj".getBytes()));
        COSDictionary result = victim.nextDictionary();
        assertEquals(10, result.getInt(COSName.R));
    }

    @Test
    public void nextDictionaryWorkaroundEndstream() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("<< /R 10  endstream".getBytes()));
        COSDictionary result = victim.nextDictionary();
        assertEquals(10, result.getInt(COSName.R));
    }

    @Test
    public void nextDictionaryInvalid() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("<< /R 10  invalid".getBytes()));
        COSDictionary result = victim.nextDictionary();
        assertEquals(10, result.getInt(COSName.R));
    }

    @Test
    public void nextDictionaryInvalidSkipped() throws IOException
    {
        victim = new COSParser(
                inMemorySeekableSourceFrom("<< /R 10  invalid /S (A String) >>".getBytes()));
        COSDictionary result = victim.nextDictionary();
        assertEquals(10, result.getInt(COSName.R));
        assertEquals(COSString.newInstance("A String".getBytes(Charsets.ISO_8859_1)),
                result.getItem(COSName.S));
    }

    @Test
    public void nextDictionaryBadValue() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("<< /R Chuck >>".getBytes()));
        COSDictionary result = victim.nextDictionary();
        assertNull(result.getItem(COSName.R));
    }

    @Test(expected = IOException.class)
    public void nextDictionaryTrunkatedEOF() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("<< /R 10 ".getBytes()));
        victim.nextDictionary();
    }

    @Test
    public void nextNumberOrIndirectReference() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("1.23".getBytes()));
        COSBase result = victim.nextNumberOrIndirectReference();
        assertThat(result, is(instanceOf(COSFloat.class)));
        assertEquals(1.23f, ((COSFloat) result).floatValue(), 0);
    }

    @Test
    public void nextNumberOrIndirectReferenceTwoNumbers() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("1.23 5".getBytes()));
        COSBase result = victim.nextNumberOrIndirectReference();
        assertThat(result, is(instanceOf(COSFloat.class)));
        assertEquals(1.23f, ((COSFloat) result).floatValue(), 0);
        assertEquals(4, victim.position());
    }

    @Test
    public void nextNumberOrIndirectReferenceObj() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("10 0 R".getBytes()));
        COSBase result = victim.nextNumberOrIndirectReference();
        assertThat(result, is(instanceOf(ExistingIndirectCOSObject.class)));
    }

    @Test(expected = IOException.class)
    public void nextNumberOrIndirectReferenceMalformed() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom("1.23 5 R".getBytes()));
        victim.nextNumberOrIndirectReference();
    }

    @Test
    public void nextStream() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        streamDictionary.setInt(COSName.LENGTH, 63);
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamNoLength() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamIgnoresSpacesAfterStreamKeyword() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_spaced.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        streamDictionary.setInt(COSName.LENGTH, 63);
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamMissingLineFeed() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_missing_linefeed.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        streamDictionary.setInt(COSName.LENGTH, 63);
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamCRLF() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_cr_lf.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        streamDictionary.setInt(COSName.LENGTH, 63);
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamCR() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_cr_alone.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        streamDictionary.setInt(COSName.LENGTH, 63);
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamEndobj() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_endobj.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        streamDictionary.setInt(COSName.LENGTH, 63);
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test(expected = IOException.class)
    public void nextStreamInvalidLength() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_cr_alone.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        streamDictionary.setBoolean(COSName.LENGTH, false);
        victim.nextStream(streamDictionary);
    }

    @Test
    public void nextStreamWrongLength() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        streamDictionary.setInt(COSName.LENGTH, 163);
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamNoLengthOnlyCRBeforeEndstream() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_cr_endstream.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamNoLengthNoEOLBeforeEndstream() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_no_eol_before_endstream.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamNoLengthCRLFBeforeEndstream() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_cr_lf_before_endstream.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test
    public void nextStreamEndobjNoLength() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_endobj.txt")));
        COSDictionary streamDictionary = new COSDictionary();
        try (COSStream result = victim.nextStream(streamDictionary))
        {
            assertEquals(63, result.getFilteredLength());
        }
    }

    @Test(expected = IOException.class)
    public void nextStreamTrunkated() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/stream_trunkated.txt")));
        victim.nextStream(new COSDictionary());
    }

    @Test
    public void nextParsedToken() throws IOException
    {
        victim = new COSParser(
                inMemorySeekableSourceFrom("   3 true false (Chuck Norris) <436875636B204E6f72726973> [10 (A String)] null <</R 10>> +2 /R"
                        .getBytes()));
        assertEquals(COSInteger.THREE, victim.nextParsedToken());
        assertEquals(COSBoolean.TRUE, victim.nextParsedToken());
        assertEquals(COSBoolean.FALSE, victim.nextParsedToken());
        COSString expected = COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1));
        assertEquals(expected, victim.nextParsedToken());
        expected.setForceHexForm(true);
        assertEquals(expected, victim.nextParsedToken());
        COSBase array = victim.nextParsedToken();
        assertThat(array, is(instanceOf(COSArray.class)));
        assertEquals(COSNull.NULL, victim.nextParsedToken());
        assertThat(victim.nextParsedToken(), is(instanceOf(COSDictionary.class)));
        assertEquals(COSInteger.TWO, victim.nextParsedToken());
        assertEquals(COSName.R, victim.nextParsedToken());
        assertNull(victim.nextParsedToken());
    }

    @Test
    public void nextBadParsedToken() throws IOException
    {
        victim = new COSParser(inMemorySeekableSourceFrom(" Chuck".getBytes()));
        assertNull(victim.nextParsedToken());
    }
}