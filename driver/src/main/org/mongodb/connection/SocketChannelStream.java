/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.connection;

import org.bson.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import static org.mongodb.assertions.Assertions.isTrue;

class SocketChannelStream implements Stream {
    private final SocketChannel socketChannel;
    private final ServerAddress address;
    private final SocketSettings settings;
    private final BufferProvider bufferProvider;
    private volatile boolean isClosed;

    public SocketChannelStream(final ServerAddress address, final SocketSettings settings, final BufferProvider bufferProvider) {
        this.address = address;
        this.settings = settings;
        this.bufferProvider = bufferProvider;
        try {
            socketChannel = SocketChannel.open();
            SocketStreamHelper.initialize(socketChannel.socket(), address, settings);
        } catch (IOException e) {
            close();
            throw new MongoSocketOpenException("Exception opening socket", getAddress(), e);
        }
    }

    @Override
    public void write(final List<ByteBuf> buffers) throws IOException {
        isTrue("open", !isClosed());

        int totalSize = 0;
        ByteBuffer[] byteBufferArray = new ByteBuffer[buffers.size()];
        for (int i = 0; i < buffers.size(); i++) {
            byteBufferArray[i] = buffers.get(i).asNIO();
            totalSize += byteBufferArray[i].limit();
        }

        long bytesRead = 0;
        while (bytesRead < totalSize) {
            bytesRead += socketChannel.write(byteBufferArray);
        }
    }

    @Override
    public ByteBuf read(final int numBytes) throws IOException {
        ByteBuf buffer = bufferProvider.get(numBytes);
        isTrue("open", !isClosed());

        int totalBytesRead = 0;
        while (totalBytesRead < buffer.limit()) {
            int bytesRead = socketChannel.read(buffer.asNIO());
            if (bytesRead == -1) {
                throw new MongoSocketReadException("Prematurely reached end of stream", getAddress());
            }
            totalBytesRead += bytesRead;
        }
        return buffer.flip();
    }

    @Override
    public void writeAsync(final List<ByteBuf> buffers, final AsyncCompletionHandler<Void> handler) {
        throw new UnsupportedOperationException(getClass() + " does not support asynchronous operations.");
    }

    @Override
    public void readAsync(final int numBytes, final AsyncCompletionHandler<ByteBuf> handler) {
        throw new UnsupportedOperationException(getClass() + " does not support asynchronous operations.");
    }

    @Override
    public ServerAddress getAddress() {
        return address;
    }

    /**
     * Get the settings for this socket.
     *
     * @return the settings
     */
    SocketSettings getSettings() {
        return settings;
    }

    @Override
    public void close() {
        try {
            isClosed = true;
            if (socketChannel != null) {
                socketChannel.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }
}