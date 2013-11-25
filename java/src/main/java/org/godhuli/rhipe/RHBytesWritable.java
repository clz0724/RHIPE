/**
 * Copyright 2009 Saptarshi Guha
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.godhuli.rhipe;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;
import org.godhuli.rhipe.REXPProtos.REXP;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class RHBytesWritable implements WritableComparable<RHBytesWritable> {
    protected static final Log LOG = LogFactory.getLog(RHBytesWritable.class.getName());
    public static String fieldSep = " ";
    private int size;
    private byte[] bytes;
    private static final byte[] EMPTY_BYTES = {};

    public RHBytesWritable() {
        this(EMPTY_BYTES);
    }

    public RHBytesWritable(final byte[] bytes) {
        this(bytes, bytes.length);
    }

    public RHBytesWritable(final byte[] bytes, final int length) {
        this.bytes = bytes;
        this.size = length;
    }

    public int getLength() {
        return size;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public byte[] getActualBytes() {
        final byte[] b = new byte[getLength()];
        System.arraycopy(bytes, 0, b, 0, getLength());
        return b;
    }

    public void setSize(final int size) {
        if (size > getCapacity()) {
            setCapacity(size * 3 / 2);
        }
        this.size = size;
    }

    public int getCapacity() {
        return bytes.length;
    }

    public void setCapacity(final int new_cap) {
        if (new_cap != getCapacity()) {
            final byte[] new_data = new byte[new_cap];
            if (new_cap < size) {
                size = new_cap;
            }
            if (size != 0) {
                System.arraycopy(bytes, 0, new_data, 0, size);
            }
            bytes = new_data;
        }
    }

    public void set(final byte[] b) {
        set(b, 0, b.length);
    }

    public void set(final RHBytesWritable newData) {
        set(newData.bytes, 0, newData.size);
    }

    public void set(final byte[] newData, final int offset, final int length) {
        setSize(0);
        setSize(length);
        System.arraycopy(newData, offset, bytes, 0, size);
    }

    public void readFields(final DataInput in) throws IOException {
        setSize(0);
        setSize(readVInt(in));
        // LOG.info("Read Size="+size);
        in.readFully(bytes, 0, size);
        // LOG.info("PrettyBYtes: "+RHBytesWritable.bytesPretty(bytes));

        // readIntFields(in);
    }

    public void readIntFields(final DataInput in) throws IOException {
        setSize(0); // clear the old data
        final int d = in.readInt();
        setSize(d);
        in.readFully(bytes, 0, size);
    }

    public void write(final DataOutput out) throws IOException {
        WritableUtils.writeVInt(out, size);
        out.write(bytes, 0, size);
    }

    public void writeAsInt(final DataOutput out) throws IOException {
        out.writeInt(size);
        out.write(bytes, 0, size);
    }

    //Equality
    public int hashCode() {
        return WritableComparator.hashBytes(bytes, size);
    }

    public boolean equals(final Object other) {
        if (!(other instanceof RHBytesWritable)) {
            return false;
        }
        final RHBytesWritable that = (RHBytesWritable) other;
        if (this.getLength() != that.getLength()) {
            return false;
        }
        return this.compareTo(that) == 0;
    }

    public int compareTo(final byte[] other, final int off, final int len) {
        return WritableComparator.compareBytes(this.bytes, 0, this.size, other, off, len);
    }

    public int compareTo(final RHBytesWritable that) {
        return WritableComparator.compareBytes(this.bytes, 0, this.size, that.bytes, 0, that.size);
    }

    public static class Comparator extends WritableComparator {
        private final BytesWritable.Comparator comparator = new BytesWritable.Comparator();

        public Comparator() {
            super(RHBytesWritable.class);
        }

        public int compare(final byte[] b1, final int s1, final int l1, final byte[] b2, final int s2, final int l2) {
            // return comparator.compare(b1, s1, l1, b2, s2, l2);
            final int off1 = decodeVIntSize(b1[s1]);
            final int off2 = decodeVIntSize(b2[s2]);
            //TEMPCHANGE
            return compareBytes(b1, s1 + off1, l1 - off1, b2, s2 + off2, l2 - off2); //why this serialized form?
        }
    }

    static { // register this comparator
        WritableComparator.define(RHBytesWritable.class, new Comparator());
    }

    //PARSING
    REXP getParsed() throws com.google.protobuf.InvalidProtocolBufferException {
        return REXP.newBuilder().mergeFrom(bytes, 0, size).build();
    }

    //DISPLAY
    public String toByteString() {
        final StringBuilder sb = new StringBuilder(3 * this.size);
        for (int idx = 0; idx < this.size; idx++) {
            if (idx != 0) {
                sb.append(" 0x");
            }
            else {
                sb.append("0x");
            }
            final String num = Integer.toHexString(0xff & bytes[idx]);
            if (num.length() < 2) {
                sb.append('0');
            }
            sb.append(num);
        }
        return sb.toString();
    }

    public String toString() {
        return REXPHelper.toString(bytes, 0, size);
    }

    public static String bytesPretty(final byte[] b) {
        return RHBytesWritable.bytesPretty(b, 0, b.length);
    }

    public static String bytesPretty(final byte[] b, final int offset, final int length) {
        final StringBuffer sb = new StringBuffer(3 * length);
        for (int idx = 0; idx < length; idx++) {
            if (idx != 0) {
                sb.append(" 0x");
            }
            else {
                sb.append("0x");
            }
            final String num = Integer.toHexString(0xff & b[offset + idx]);
            if (num.length() < 2) {
                sb.append('0');
            }
            sb.append(num);
        }
        return sb.toString();
    }

    public static boolean isNegativeVInt(final byte value) {
        return value < -120 || (value >= -112 && value < 0);
    }

    // UTILITY
    public static int decodeVIntSize(final byte value) {
        if (value >= -112) {
            return 1;
        }
        else if (value < -120) {
            return -119 - value;
        }
        return -111 - value;
    }


    public static int readVInt(final DataInput stream) throws IOException {
        final byte firstByte = stream.readByte();
        final int len = decodeVIntSize(firstByte);
        if (len == 1) {
            return firstByte;
        }
        long i = 0;
        for (int idx = 0; idx < len - 1; idx++) {
            final byte b = stream.readByte();
            i = i << 8;
            i = i | (b & 0xFF);
        }
        return ((int) ((isNegativeVInt(firstByte) ? (i ^ -1L) : i)));

    }


}
