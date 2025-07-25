/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.util;

import org.apache.hudi.common.table.view.FileSystemViewStorageConfig;
import org.apache.hudi.common.util.StringUtils;
import org.apache.hudi.configuration.FlinkOptions;
import org.apache.hudi.configuration.HadoopConfigurations;
import org.apache.hudi.exception.HoodieIOException;
import org.apache.hudi.hadoop.fs.HadoopFSUtils;
import org.apache.hudi.storage.StoragePath;

import org.apache.flink.configuration.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;

import static org.apache.hudi.common.table.HoodieTableMetaClient.AUXILIARYFOLDER_NAME;

/**
 * Helper class to read/write {@link FileSystemViewStorageConfig}.
 */
public class ViewStorageProperties {
  private static final Logger LOG = LoggerFactory.getLogger(ViewStorageProperties.class);

  private static final String FILE_NAME = "view_storage_conf";

  /**
   * Initialize the {@link #FILE_NAME} meta file.
   */
  public static void createProperties(
      String basePath,
      FileSystemViewStorageConfig config,
      Configuration flinkConf) throws IOException {
    Path propertyPath = getPropertiesFilePath(basePath, flinkConf.get(FlinkOptions.WRITE_CLIENT_ID));
    FileSystem fs = HadoopFSUtils.getFs(basePath, HadoopConfigurations.getHadoopConf(flinkConf));
    fs.delete(propertyPath, false);
    try (OutputStream outputStream = fs.create(propertyPath)) {
      config.getProps().store(outputStream,
          "Filesystem view storage properties saved on " + new Date(System.currentTimeMillis()));
    }
  }

  /**
   * Read the {@link FileSystemViewStorageConfig} with given table base path.
   */
  public static FileSystemViewStorageConfig loadFromProperties(String basePath, Configuration conf) {
    Path propertyPath = getPropertiesFilePath(basePath, conf.get(FlinkOptions.WRITE_CLIENT_ID));
    LOG.info("Loading filesystem view storage properties from " + propertyPath);
    FileSystem fs = HadoopFSUtils.getFs(basePath, HadoopConfigurations.getHadoopConf(conf));
    Properties props = new Properties();
    try {
      try (InputStream inputStream = fs.open(propertyPath)) {
        props.load(inputStream);
      }
      return FileSystemViewStorageConfig.newBuilder().fromProperties(props).build();
    } catch (IOException e) {
      throw new HoodieIOException("Could not load filesystem view storage properties from " + propertyPath, e);
    }
  }

  private static Path getPropertiesFilePath(String basePath, String uniqueId) {
    String auxPath = basePath + StoragePath.SEPARATOR + AUXILIARYFOLDER_NAME;
    String fileName = StringUtils.isNullOrEmpty(uniqueId) ? FILE_NAME : FILE_NAME + "_" + uniqueId;
    return new Path(auxPath, fileName);
  }
}
