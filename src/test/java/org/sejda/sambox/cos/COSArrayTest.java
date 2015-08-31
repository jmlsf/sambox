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
package org.sejda.sambox.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.sejda.sambox.pdmodel.PDPage;

/**
 * @author Andrea Vacondio
 *
 */
public class COSArrayTest
{

    @Test
    public void setNull()
    {
        COSArray victim = new COSArray();
        victim.growToSize(2);
        victim.set(1, null);
        assertNull(victim.get(1));
    }

    @Test
    public void setIndex()
    {
        COSArray victim = new COSArray();
        victim.growToSize(2);
        victim.set(1, COSInteger.THREE);
        assertEquals(COSInteger.THREE, victim.get(1));
    }

    @Test
    public void setNullCOSObjectable()
    {
        COSArray victim = new COSArray();
        victim.growToSize(2);
        victim.set(1, (COSObjectable) null);
        assertNull(victim.get(1));
    }

    @Test
    public void setIndexCOSObjectable()
    {
        COSArray victim = new COSArray();
        victim.growToSize(2);
        PDPage page = new PDPage();
        victim.set(1, page);
        assertEquals(page.getCOSObject(), victim.get(1));
    }
}