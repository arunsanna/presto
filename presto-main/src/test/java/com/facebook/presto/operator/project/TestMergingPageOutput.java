/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.operator.project;

import com.facebook.presto.spi.Page;
import com.facebook.presto.spi.type.Type;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import java.util.List;

import static com.facebook.presto.SequencePageBuilder.createSequencePage;
import static com.facebook.presto.execution.buffer.PageSplitterUtil.splitPage;
import static com.facebook.presto.operator.PageAssertions.assertPageEquals;
import static com.facebook.presto.spi.type.BigintType.BIGINT;
import static com.facebook.presto.spi.type.DoubleType.DOUBLE;
import static com.facebook.presto.spi.type.RealType.REAL;
import static java.lang.Math.toIntExact;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestMergingPageOutput
{
    private static final List<Type> TYPES = ImmutableList.of(BIGINT, REAL, DOUBLE);

    @Test
    public void testMinPageSizeThreshold()
            throws Exception
    {
        Page page = createSequencePage(TYPES, 10);

        MergingPageOutput output = new MergingPageOutput(TYPES, toIntExact(page.getSizeInBytes()), Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertTrue(output.needsInput());
        assertNull(output.getOutput());

        output.addInput(createPageProcessorOutput(page));
        assertFalse(output.needsInput());
        assertSame(output.getOutput(), page);
    }

    @Test
    public void testMinRowCountThreshold()
            throws Exception
    {
        Page page = createSequencePage(TYPES, 10);

        MergingPageOutput output = new MergingPageOutput(TYPES, Integer.MAX_VALUE, page.getPositionCount(), Integer.MAX_VALUE);

        assertTrue(output.needsInput());
        assertNull(output.getOutput());

        output.addInput(createPageProcessorOutput(page));
        assertFalse(output.needsInput());
        assertSame(output.getOutput(), page);
    }

    @Test
    public void testBufferSmallPages()
            throws Exception
    {
        int singlePageRowCount = 10;
        Page page = createSequencePage(TYPES, singlePageRowCount * 2);
        List<Page> splits = splitPage(page, page.getSizeInBytes() / 2);

        MergingPageOutput output = new MergingPageOutput(TYPES, toIntExact(page.getSizeInBytes() + 1), page.getPositionCount() + 1, Integer.MAX_VALUE);

        assertTrue(output.needsInput());
        assertNull(output.getOutput());

        output.addInput(createPageProcessorOutput(splits.get(0)));
        assertFalse(output.needsInput());
        assertNull(output.getOutput());
        assertTrue(output.needsInput());

        output.addInput(createPageProcessorOutput(splits.get(1)));
        assertFalse(output.needsInput());
        assertNull(output.getOutput());

        output.finish();

        assertFalse(output.needsInput());
        assertPageEquals(TYPES, output.getOutput(), page);
    }

    @Test
    public void testFlushOnBigPage()
            throws Exception
    {
        Page smallPage = createSequencePage(TYPES, 10);
        Page bigPage = createSequencePage(TYPES, 100);

        MergingPageOutput output = new MergingPageOutput(TYPES, toIntExact(bigPage.getSizeInBytes()), bigPage.getPositionCount(), Integer.MAX_VALUE);

        assertTrue(output.needsInput());
        assertNull(output.getOutput());

        output.addInput(createPageProcessorOutput(smallPage));
        assertFalse(output.needsInput());
        assertNull(output.getOutput());
        assertTrue(output.needsInput());

        output.addInput(createPageProcessorOutput(bigPage));
        assertFalse(output.needsInput());
        assertPageEquals(TYPES, output.getOutput(), smallPage);
        assertFalse(output.needsInput());
        assertSame(output.getOutput(), bigPage);
    }

    @Test
    public void testFlushOnFullPage()
            throws Exception
    {
        int singlePageRowCount = 10;
        List<Type> types = ImmutableList.of(BIGINT);
        Page page = createSequencePage(types, singlePageRowCount * 2);
        List<Page> splits = splitPage(page, page.getSizeInBytes() / 2);

        MergingPageOutput output = new MergingPageOutput(types, toIntExact(page.getSizeInBytes() + 1), page.getPositionCount() + 1, toIntExact(page.getSizeInBytes()));

        assertTrue(output.needsInput());
        assertNull(output.getOutput());

        output.addInput(createPageProcessorOutput(splits.get(0)));
        assertFalse(output.needsInput());
        assertNull(output.getOutput());
        assertTrue(output.needsInput());

        output.addInput(createPageProcessorOutput(splits.get(1)));
        assertFalse(output.needsInput());
        assertPageEquals(types, output.getOutput(), page);
    }

    private static PageProcessorOutput createPageProcessorOutput(Page page)
    {
        return new PageProcessorOutput(page);
    }
}
