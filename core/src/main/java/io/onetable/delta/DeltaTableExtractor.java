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
 
package io.onetable.delta;

import java.time.Instant;
import java.util.List;

import javax.inject.Singleton;

import org.apache.spark.sql.delta.Snapshot;

import io.onetable.model.OneTable;
import io.onetable.model.schema.OnePartitionField;
import io.onetable.model.schema.OneSchema;
import io.onetable.model.storage.DataLayoutStrategy;
import io.onetable.model.storage.TableFormat;

/** Extracts {@link OneTable} canonical representation of a table at a point in time for Delta. */
@Singleton
public class DeltaTableExtractor {
  private final DeltaSchemaExtractor schemaExtractor;

  public DeltaTableExtractor() {
    this.schemaExtractor = DeltaSchemaExtractor.getInstance();
  }

  public OneTable table(String tableName, Snapshot snapshot) {
    OneSchema schema = schemaExtractor.toOneSchema(snapshot.metadata().schema());
    List<OnePartitionField> partitionFields =
        DeltaPartitionExtractor.getInstance()
            .convertFromDeltaPartitionFormat(schema, snapshot.metadata().partitionSchema());
    DataLayoutStrategy dataLayoutStrategy =
        !partitionFields.isEmpty()
            ? DataLayoutStrategy.DIR_HIERARCHY_PARTITION_VALUES
            : DataLayoutStrategy.FLAT;
    return OneTable.builder()
        .tableFormat(TableFormat.DELTA)
        .basePath(snapshot.deltaLog().dataPath().toString())
        .name(tableName)
        .layoutStrategy(dataLayoutStrategy)
        .partitioningFields(partitionFields)
        .readSchema(schema)
        .latestCommitTime(Instant.ofEpochMilli(snapshot.timestamp()))
        .build();
  }
}