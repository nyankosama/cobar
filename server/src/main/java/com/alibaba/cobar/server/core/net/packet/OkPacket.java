/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cobar.server.core.net.packet;

import java.nio.ByteBuffer;

import com.alibaba.cobar.server.core.net.FrontendConnection;
import com.alibaba.cobar.server.core.net.protocol.MySQLMessage;
import com.alibaba.cobar.server.util.ByteBufferUtil;

/**
 * From server to client in response to command, if no error and no result set.
 * 
 * <pre>
 * Bytes                       Name
 * -----                       ----
 * 1                           field_count, always = 0
 * 1-9 (Length Coded Binary)   affected_rows
 * 1-9 (Length Coded Binary)   insert_id
 * 2                           server_status
 * 2                           warning_count
 * n   (until end of packet)   message fix:(Length Coded String)
 * 
 * @see http://forge.mysql.com/wiki/MySQL_Internals_ClientServer_Protocol#OK_Packet
 * </pre>
 * 
 * @author xianmao.hexm 2010-7-16 上午10:33:50
 */
public class OkPacket extends AbstractPacket {

    public static final byte FIELD_COUNT = 0x00;
    public static final byte[] OK = new byte[] { 7, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0 };

    public byte fieldCount = FIELD_COUNT;
    public long affectedRows;
    public long insertId;
    public int serverStatus;
    public int warningCount;
    public byte[] message;

    public void read(BinaryPacket bin) {
        packetLength = bin.packetLength;
        packetId = bin.packetId;
        MySQLMessage mm = new MySQLMessage(bin.data);
        fieldCount = mm.read();
        affectedRows = mm.readLength();
        insertId = mm.readLength();
        serverStatus = mm.readUB2();
        warningCount = mm.readUB2();
        if (mm.hasRemaining()) {
            this.message = mm.readBytesWithLength();
        }
    }

    public void read(byte[] data) {
        MySQLMessage mm = new MySQLMessage(data);
        packetLength = mm.readUB3();
        packetId = mm.read();
        fieldCount = mm.read();
        affectedRows = mm.readLength();
        insertId = mm.readLength();
        serverStatus = mm.readUB2();
        warningCount = mm.readUB2();
        if (mm.hasRemaining()) {
            this.message = mm.readBytesWithLength();
        }
    }

    public void write(FrontendConnection c) {
        packetLength = calcPacketLength();
        ByteBuffer buffer = c.allocate();
        ByteBufferUtil.writeUB3(buffer, packetLength);
        buffer.put(packetId);
        buffer.put(fieldCount);
        ByteBufferUtil.writeLength(buffer, affectedRows);
        ByteBufferUtil.writeLength(buffer, insertId);
        ByteBufferUtil.writeUB2(buffer, serverStatus);
        ByteBufferUtil.writeUB2(buffer, warningCount);
        if (message != null) {
            ByteBufferUtil.writeWithLength(buffer, message);
        }
        c.write(buffer);
    }

    @Override
    public int calcPacketLength() {
        int i = 1;
        i += ByteBufferUtil.getLength(affectedRows);
        i += ByteBufferUtil.getLength(insertId);
        i += 4;
        if (message != null) {
            i += ByteBufferUtil.getLength(message);
        }
        return i;
    }

    @Override
    protected String getPacketInfo() {
        return "OK Packet";
    }

}