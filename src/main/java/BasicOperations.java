/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.examples;

import alluxio.AlluxioURI;
import alluxio.client.ReadType;
import alluxio.client.WriteType;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.exception.AlluxioException;
import alluxio.util.CommonUtils;
import alluxio.util.FormatUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Callable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Example to show the basic operations of Alluxio.
 */
@ThreadSafe
public class BasicOperations implements Callable<Boolean> {
  private static final Logger LOG = LoggerFactory.getLogger(BasicOperations.class);

  private final AlluxioURI srcFilePath;
  private final AlluxioURI destFilePath;
  private final OpenFileOptions mReadOptions;
  private final CreateFileOptions mWriteOptions;

  /**
   * @param srcFilePath the path for the files
   * @param destFilePath the path for the files
   * @param readType the {@link ReadType}
   * @param writeType the {@link WriteType}
   */
  public BasicOperations(AlluxioURI srcFilePath, AlluxioURI destFilePath, ReadType readType, WriteType writeType) {
    this.srcFilePath = srcFilePath;
    this.destFilePath = destFilePath;
    mReadOptions = OpenFileOptions.defaults().setReadType(readType);
    mWriteOptions = CreateFileOptions.defaults().setWriteType(writeType);
  }

  public BasicOperations(AlluxioURI srcFilePath, ReadType readType, WriteType writeType) {
    this.srcFilePath = srcFilePath;
    this.destFilePath = null;
    mReadOptions = OpenFileOptions.defaults().setReadType(readType);
    mWriteOptions = CreateFileOptions.defaults().setWriteType(writeType);
  }

  @Override
  public Boolean call() throws Exception {
    FileSystem fs = FileSystem.Factory.get();
    writeFile(fs);
    return readFile(fs);
  }

  private void writeFile(FileSystem fileSystem)
    throws IOException, AlluxioException {

    Path path = Paths.get(srcFilePath.toString()); // assumption is that file is found on local storage
    byte[] data = Files.readAllBytes(path); // reads all the data in-memory

    ByteBuffer buf = ByteBuffer.allocate(data.length);
    buf.order(ByteOrder.nativeOrder());
    buf.put(data);

    LOG.debug("Writing data...");
    System.out.println("Writing data...");
    long startTimeMs = CommonUtils.getCurrentMs();
    FileOutStream os = fileSystem.createFile(destFilePath, mWriteOptions);
    os.write(buf.array());
    os.close();

    LOG.info(FormatUtils.formatTimeTakenMs(startTimeMs, "writeFile to file " + destFilePath));
    System.out.println(FormatUtils.formatTimeTakenMs(startTimeMs, "writeFile to file " + destFilePath));
  }

  public boolean readFile(FileSystem fileSystem)
      throws IOException, AlluxioException {
    boolean pass = true;
    LOG.debug("Reading data...");
    System.out.println("Reading data... from: " + srcFilePath.toString());
    final long startTimeMs = CommonUtils.getCurrentMs();
    FileInStream is = fileSystem.openFile(srcFilePath, mReadOptions);
    ByteBuffer buf = ByteBuffer.allocate((int) is.remaining());
    is.read(buf.array());
    buf.order(ByteOrder.nativeOrder());
  
    // Problem with the HDF5 library loading mechanism - non-standard
    // H5File ss = hdf5_getters.hdf5_open_readonly(mFilePath.toString()) ;
    System.out.println("Reading data..." + buf.array().length);
    is.close();

    LOG.info(FormatUtils.formatTimeTakenMs(startTimeMs, "readFile file " + srcFilePath));
    System.out.println(FormatUtils.formatTimeTakenMs(startTimeMs, "readFile file " + srcFilePath));
    return pass;
  }
}
