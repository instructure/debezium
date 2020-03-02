/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.postgresql;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.connector.AbstractSourceInfoStructMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.HashSet;

public class PostgresSourceInfoStructMaker extends AbstractSourceInfoStructMaker<SourceInfo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresSourceInfoStructMaker.class);

    private final Schema schema;

    public PostgresSourceInfoStructMaker(String connector, String version, CommonConnectorConfig connectorConfig) {
        super(connector, version, connectorConfig);
        schema = commonSchemaBuilder()
                .name("io.debezium.connector.postgresql.Source")
                .field(SourceInfo.SCHEMA_NAME_KEY, Schema.STRING_SCHEMA)
                .field(SourceInfo.TABLE_NAME_KEY, Schema.STRING_SCHEMA)
                .field(SourceInfo.TXID_KEY, Schema.OPTIONAL_INT64_SCHEMA)
                .field(SourceInfo.LSN_KEY, Schema.OPTIONAL_INT64_SCHEMA)
                .field(SourceInfo.XMIN_KEY, Schema.OPTIONAL_INT64_SCHEMA)
                .field(SourceInfo.COMPLETED_TABLES, Schema.OPTIONAL_STRING_SCHEMA)
                .build();
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public Struct struct(SourceInfo sourceInfo) {
        assert sourceInfo.database() != null
                && sourceInfo.schemaName() != null
                && sourceInfo.tableName() != null;

        Struct result = super.commonStruct(sourceInfo);
        result.put(SourceInfo.SCHEMA_NAME_KEY, sourceInfo.schemaName());
        result.put(SourceInfo.TABLE_NAME_KEY, sourceInfo.tableName());
        if (sourceInfo.txId() != null) {
            result.put(SourceInfo.TXID_KEY, sourceInfo.txId());
        }
        if (sourceInfo.lsn() != null) {
            result.put(SourceInfo.LSN_KEY, sourceInfo.lsn());
        }
        if (sourceInfo.xmin() != null) {
            result.put(SourceInfo.XMIN_KEY, sourceInfo.xmin());
        }
        String serializedCompletedTables = serializeCompletedTables(sourceInfo.completedTables());
        if (serializedCompletedTables != null) {
            result.put(SourceInfo.COMPLETED_TABLES, serializedCompletedTables);
        }
        return result;
    }

    private String serializeCompletedTables(HashSet<String> completedTables) {
        if (completedTables == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(completedTables);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            LOGGER.error("Failed to serialize completed Tables");
            return null;
        }
    }
}
