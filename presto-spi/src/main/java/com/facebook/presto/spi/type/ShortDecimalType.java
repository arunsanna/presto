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

package com.facebook.presto.spi.type;

import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.block.Block;
import com.facebook.presto.spi.block.BlockBuilder;

import java.math.BigInteger;

import static io.airlift.slice.SizeOf.SIZE_OF_LONG;
import static java.lang.Math.pow;
import static java.lang.Math.round;

public final class ShortDecimalType
        extends DecimalType
{
    private static final long[] TEN_TO_NTH = new long[TEN_TO_NTH_TABLE_LENGTH];

    static {
        for (int i = 0; i < TEN_TO_NTH.length; ++i) {
            TEN_TO_NTH[i] = round(pow(10, i));
        }
    }

    protected ShortDecimalType(int precision, int scale)
    {
        super(precision, scale, long.class, SIZE_OF_LONG);
        validatePrecisionScale(precision, scale, MAX_SHORT_PRECISION);
    }

    @Override
    public Object getObjectValue(ConnectorSession session, Block block, int position)
    {
        if (block.isNull(position)) {
            return null;
        }
        long unscaledValue = block.getLong(position, 0);
        return new SqlDecimal(BigInteger.valueOf(unscaledValue), precision, scale);
    }

    @Override
    public boolean equalTo(Block leftBlock, int leftPosition, Block rightBlock, int rightPosition)
    {
        long leftValue = leftBlock.getLong(leftPosition, 0);
        long rightValue = rightBlock.getLong(rightPosition, 0);
        return leftValue == rightValue;
    }

    @Override
    public long hash(Block block, int position)
    {
        return block.getLong(position, 0);
    }

    @Override
    public int compareTo(Block leftBlock, int leftPosition, Block rightBlock, int rightPosition)
    {
        long leftValue = leftBlock.getLong(leftPosition, 0);
        long rightValue = rightBlock.getLong(rightPosition, 0);
        return Long.compare(leftValue, rightValue);
    }

    @Override
    public void appendTo(Block block, int position, BlockBuilder blockBuilder)
    {
        if (block.isNull(position)) {
            blockBuilder.appendNull();
        }
        else {
            blockBuilder.writeLong(block.getLong(position, 0)).closeEntry();
        }
    }

    @Override
    public long getLong(Block block, int position)
    {
        return block.getLong(position, 0);
    }

    @Override
    public void writeLong(BlockBuilder blockBuilder, long value)
    {
        blockBuilder.writeLong(value).closeEntry();
    }

    public static long parseShortDecimalBytes(byte[] bytes)
    {
        long value = 0;
        if ((bytes[0] & 0x80) != 0) {
            for (int i = 0; i < 8 - bytes.length; ++i) {
                value |= 0xFFL << (8 * (7 - i));
            }
        }

        for (int i = 0; i < bytes.length; i++) {
            value |= ((long) bytes[bytes.length - i - 1] & 0xFFL) << (8 * i);
        }

        return value;
    }

    public static long tenToNth(int n)
    {
        return TEN_TO_NTH[n];
    }

    public static String toString(long unscaledValue, int precision, int scale)
    {
        return DecimalType.toString(Long.toString(unscaledValue), precision, scale);
    }
}
