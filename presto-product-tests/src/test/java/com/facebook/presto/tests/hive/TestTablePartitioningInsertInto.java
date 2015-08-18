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
package com.facebook.presto.tests.hive;

import com.facebook.presto.jdbc.PrestoResultSet;
import com.facebook.presto.tests.queryinfo.QueryInfoClient;
import com.google.inject.Inject;
import com.teradata.tempto.ProductTest;
import com.teradata.tempto.Requirement;
import com.teradata.tempto.RequirementsProvider;
import com.teradata.tempto.configuration.Configuration;
import com.teradata.tempto.context.ThreadLocalTestContextHolder;
import com.teradata.tempto.fulfillment.table.MutableTableRequirement;
import com.teradata.tempto.fulfillment.table.MutableTablesState;
import com.teradata.tempto.fulfillment.table.hive.HiveTableDefinition;
import com.teradata.tempto.query.QueryResult;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static com.facebook.presto.tests.TestGroups.HIVE_CONNECTOR;
import static com.facebook.presto.tests.TestGroups.SMOKE;
import static com.teradata.tempto.Requirements.compose;
import static com.teradata.tempto.fulfillment.table.hive.InlineDataSource.createResourceDataSource;
import static com.teradata.tempto.query.QueryExecutor.query;
import static com.teradata.tempto.query.QueryType.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;

public class TestTablePartitioningInsertInto
        extends ProductTest
        implements RequirementsProvider
{
    @Inject
    private MutableTablesState mutableTablesState;

    private static final String PARTITIONED_NATION_NAME = "partitioned_nation_read_test";
    private static final String TARGET_NATION_NAME = "target_nation_test";

    private static final int NUMBER_OF_LINES_PER_SPLIT = 5;
    private static final String DATA_REVISION = "1";
    private static final HiveTableDefinition PARTITIONED_NATION =
            HiveTableDefinition.builder()
                    .setName(PARTITIONED_NATION_NAME)
                    .setCreateTableDDLTemplate("" +
                            "CREATE EXTERNAL TABLE %NAME%(" +
                            "   p_nationkey     INT," +
                            "   p_name          STRING," +
                            "   p_comment       STRING) " +
                            "PARTITIONED BY (p_regionkey INT)" +
                            "ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' ")
                    .addPartition("p_regionkey=1", createResourceDataSource(PARTITIONED_NATION_NAME, DATA_REVISION, partitionDataFile(1)))
                    .addPartition("p_regionkey=2", createResourceDataSource(PARTITIONED_NATION_NAME, DATA_REVISION, partitionDataFile(2)))
                    .addPartition("p_regionkey=3", createResourceDataSource(PARTITIONED_NATION_NAME, DATA_REVISION, partitionDataFile(3)))
                    .build();

    private static final HiveTableDefinition TARGET_NATION =
            HiveTableDefinition.builder()
                    .setName(TARGET_NATION_NAME)
                    .setCreateTableDDLTemplate("" +
                            "CREATE EXTERNAL TABLE %NAME%(" +
                            "   p_nationkey     INT," +
                            "   p_name          STRING," +
                            "   p_comment       STRING," +
                            "   p_regionkey     INT) " +
                            "ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' " +
                            "LOCATION '%LOCATION%'")
                    .setNoData()
                    .build();

    private static String partitionDataFile(int region)
    {
        return "com/facebook/presto/tests/hive/data/partitioned_nation/nation_region_" + region + ".textfile";
    }

    @Override
    public Requirement getRequirements(Configuration configuration)
    {
        return compose(
                MutableTableRequirement.builder(PARTITIONED_NATION).build(),
                MutableTableRequirement.builder(TARGET_NATION).build());
    }

    @Test(groups = {HIVE_CONNECTOR, SMOKE})
    public void selectFromPartitionedNation()
            throws Exception
    {
        // read all data
        testQuerySplitsNumber("INSERT INTO %s SELECT * FROM %s WHERE p_nationkey < 40", NUMBER_OF_LINES_PER_SPLIT * 3);

        // read no partitions
        testQuerySplitsNumber("INSERT INTO %s SELECT * FROM %s WHERE p_regionkey = 42", 0);

        // read one partition
        testQuerySplitsNumber("INSERT INTO %s SELECT * FROM %s WHERE p_regionkey = 2 AND p_nationkey < 40", NUMBER_OF_LINES_PER_SPLIT * 1);
        // read two partitions
        testQuerySplitsNumber("INSERT INTO %s SELECT * FROM %s WHERE p_regionkey = 2 AND p_nationkey < 40 or p_regionkey = 3", NUMBER_OF_LINES_PER_SPLIT * 2);
        // read all (three) partitions
        testQuerySplitsNumber("INSERT INTO %s SELECT * FROM %s WHERE p_regionkey = 2 OR p_nationkey < 40", NUMBER_OF_LINES_PER_SPLIT * 3);

        // range read two partitions
        testQuerySplitsNumber("INSERT INTO %s SELECT * FROM %s WHERE p_regionkey <= 2", NUMBER_OF_LINES_PER_SPLIT * 2);
        testQuerySplitsNumber("INSERT INTO %s SELECT * FROM %s WHERE p_regionkey <= 1 OR p_regionkey >= 3", NUMBER_OF_LINES_PER_SPLIT * 2);
    }

    private void testQuerySplitsNumber(String query, int expectedProcessedLines)
            throws Exception
    {
        String partitionedNation = mutableTablesState.get(PARTITIONED_NATION_NAME).getNameInDatabase();
        String targetNation = mutableTablesState.get(TARGET_NATION_NAME).getNameInDatabase();
        QueryResult queryResult = query(String.format(query,
                targetNation,
                partitionedNation), UPDATE);

        long processedLinesCount = getProcessedLinesCount(queryResult);
        assertThat(processedLinesCount).isEqualTo(expectedProcessedLines);
    }

    private long getProcessedLinesCount(QueryResult queryResult)
            throws SQLException
    {
        PrestoResultSet prestoResultSet = queryResult.getJdbcResultSet().get().unwrap(PrestoResultSet.class);
        QueryInfoClient queryInfoClient = ThreadLocalTestContextHolder.testContext().getDependency(QueryInfoClient.class);
        return queryInfoClient.getQueryStats(prestoResultSet.getQueryId()).get().getRawInputPositions();
    }
}
