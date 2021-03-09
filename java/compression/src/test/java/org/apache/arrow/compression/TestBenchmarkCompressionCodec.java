/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.arrow.compression;

import java.util.ArrayList;
import java.util.List;

import org.apache.arrow.memory.ArrowBuf;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.compression.CompressionCodec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestBenchmarkCompressionCodec {

  private RootAllocator allocator;

  private CompressionCodec codec;

  @Before
  public void init() {
    allocator = new RootAllocator();
    codec = new Lz4CompressionCodec();
  }

  @After
  public void terminate() {
    allocator.close();
  }

  private void compressBuffers(List<ArrowBuf> inputBuffers) {
    for (final ArrowBuf buf : inputBuffers) {
      buf.getReferenceManager().retain();
      final ArrowBuf compressed = codec.compress(allocator, buf);
      compressed.close();
    }
  }

  @Test
  public void benchmarkCompression() throws Exception {
    final int bufferLength = 100_000;
    final int numBuffers = 5;
    final int warmupRuns = 3;
    final int runs = 10;

    // prepare buffers to compress
    final List<ArrowBuf> buffers = new ArrayList<>();
    for (int i = 0; i < numBuffers; i++) {
      final ArrowBuf b = allocator.buffer(bufferLength * 4);
      for (int j = 0; j < bufferLength; j++) {
        b.writeInt(i + j);
      }
      buffers.add(b);
    }
    
    // Warmup
    for (int i = 0; i < warmupRuns; i++) {
      compressBuffers(buffers);
    }
    // Compress and time multiple times
    double totalTime = 0;
    for (int i = 0; i < runs; i++) {
      final long startTime = System.nanoTime();
      compressBuffers(buffers);
      final double timeMs = (System.nanoTime() - startTime) / 1_000_000.0;
      totalTime += timeMs;
      System.out.println("Run " + i + ": " + timeMs + "ms");
    }
    System.out.println("Average: " + (totalTime / runs) + "ms");

    // Close all buffers
    buffers.forEach(ArrowBuf::close);
  }
}
