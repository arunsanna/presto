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
package com.facebook.presto.tpch;

import com.facebook.presto.spi.ConnectorTableHandle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class TpchTableHandle
        implements ConnectorTableHandle
{
    private final String connectorId;
    private final String tableName;
    private final double scaleFactor;
    private final String schemaName;
    private final boolean useDecimal;

    @JsonCreator
    public TpchTableHandle(@JsonProperty("connectorId") String connectorId, @JsonProperty("tableName") String tableName, @JsonProperty("scaleFactor") double scaleFactor, @JsonProperty("schemaName") String schemaName, @JsonProperty("useDecimal") boolean useDecimal)
    {
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.tableName = requireNonNull(tableName, "tableName is null");
        checkArgument(scaleFactor > 0, "Scale factor must be larger than 0");
        this.scaleFactor = scaleFactor;
        this.schemaName = requireNonNull(schemaName, "schemaName is null");
        this.useDecimal = useDecimal;
    }

    @JsonProperty
    public String getConnectorId()
    {
        return connectorId;
    }

    @JsonProperty
    public String getTableName()
    {
        return tableName;
    }

    @JsonProperty
    public double getScaleFactor()
    {
        return scaleFactor;
    }

    @JsonProperty
    public String getSchemaName()
    {
        return schemaName;
    }

    @JsonProperty
    public boolean isUseDecimal()
    {
        return useDecimal;
    }

    @Override
    public String toString()
    {
        return "tpch:" + tableName + ":sf" + scaleFactor;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(connectorId, tableName, scaleFactor, schemaName, useDecimal);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TpchTableHandle other = (TpchTableHandle) obj;
        return Objects.equals(this.tableName, other.tableName) &&
                Objects.equals(this.scaleFactor, other.scaleFactor) &&
                Objects.equals(this.connectorId, other.connectorId) &&
                Objects.equals(this.schemaName, other.schemaName) &&
                Objects.equals(this.useDecimal, other.useDecimal);
    }
}
