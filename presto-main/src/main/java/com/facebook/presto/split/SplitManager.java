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
package com.facebook.presto.split;

import com.facebook.presto.Session;
import com.facebook.presto.metadata.TableLayoutHandle;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplitSource;
import com.facebook.presto.spi.connector.ConnectorSplitManager;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class SplitManager
{
    private final ConcurrentMap<String, ConnectorSplitManager> splitManagers = new ConcurrentHashMap<>();

    public void addConnectorSplitManager(String connectorId, ConnectorSplitManager connectorSplitManager)
    {
        checkState(splitManagers.putIfAbsent(connectorId, connectorSplitManager) == null, "SplitManager for connector '%s' is already registered", connectorId);
    }

    public SplitSource getSplits(Session session, TableLayoutHandle layout, List<ColumnHandle> columns)
    {
        String connectorId = layout.getConnectorId();
        requireNonNull(columns, "columns is null");
        ConnectorSplitManager splitManager = getConnectorSplitManager(connectorId);

        // assumes connectorId and catalog are the same
        ConnectorSession connectorSession = session.toConnectorSession(connectorId);

        ConnectorSplitSource source = splitManager.getSplits(layout.getTransactionHandle(), connectorSession, layout.getConnectorHandle(), columns);

        return new ConnectorAwareSplitSource(connectorId, layout.getTransactionHandle(), source);
    }

    private ConnectorSplitManager getConnectorSplitManager(String connectorId)
    {
        ConnectorSplitManager result = splitManagers.get(connectorId);
        checkArgument(result != null, "No split manager for connector '%s'", connectorId);
        return result;
    }
}
