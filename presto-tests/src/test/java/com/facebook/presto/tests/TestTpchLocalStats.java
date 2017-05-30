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
package com.facebook.presto.tests;

import com.facebook.presto.Session;
import com.facebook.presto.testing.LocalQueryRunner;
import com.facebook.presto.tests.statistics.StatisticsAssertion;
import com.facebook.presto.tpch.ColumnNaming;
import com.facebook.presto.tpch.TpchConnectorFactory;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import static com.facebook.presto.SystemSessionProperties.PUSH_PARTIAL_AGGREGATION_THROUGH_JOIN;
import static com.facebook.presto.testing.TestingSession.testSessionBuilder;
import static com.facebook.presto.tests.statistics.MetricComparisonStrategies.defaultTolerance;
import static com.facebook.presto.tests.statistics.MetricComparisonStrategies.isEqual;
import static com.facebook.presto.tests.statistics.MetricComparisonStrategies.noError;
import static com.facebook.presto.tests.statistics.Metrics.OUTPUT_ROW_COUNT;
import static com.facebook.presto.tests.statistics.Metrics.distinctValuesCount;
import static com.facebook.presto.tests.statistics.Metrics.highValue;
import static com.facebook.presto.tests.statistics.Metrics.lowValue;
import static com.facebook.presto.tests.statistics.Metrics.nullsFraction;
import static com.facebook.presto.tpch.TpchMetadata.TINY_SCHEMA_NAME;

public class TestTpchLocalStats
{
    private final StatisticsAssertion statisticsAssertion;

    public TestTpchLocalStats()
            throws Exception
    {
        Session defaultSession = testSessionBuilder()
                .setCatalog("tpch")
                .setSchema(TINY_SCHEMA_NAME)
                .setSystemProperty(PUSH_PARTIAL_AGGREGATION_THROUGH_JOIN, "true")
                .build();

        LocalQueryRunner runner = new LocalQueryRunner(defaultSession);
        runner.createCatalog("tpch", new TpchConnectorFactory(1),
                ImmutableMap.of("tpch.column-naming", ColumnNaming.STANDARD.name()
                ));
        statisticsAssertion = new StatisticsAssertion(runner);
    }

    @Test
    void testTableScanStats()
    {
        statisticsAssertion.check("SELECT * FROM nation",
                checks -> checks
                        .estimate(OUTPUT_ROW_COUNT, defaultTolerance())
                        .estimate(nullsFraction("n_nationkey"), noError())
                        .estimate(distinctValuesCount("n_nationkey"), noError())
                        .estimate(lowValue("n_nationkey"), isEqual())
                        .estimate(highValue("n_nationkey"), isEqual())
                        .estimate(nullsFraction("n_regionkey"), noError())
                        .estimate(lowValue("n_regionkey"), isEqual())
                        .estimate(highValue("n_regionkey"), isEqual())
                        .estimate(distinctValuesCount("n_name"), noError())
                        .estimate(nullsFraction("n_name"), noError())
                        .noEstimate(lowValue("n_regionkey"))
                        .noEstimate(highValue("n_regionkey"))
        );
    }
}
