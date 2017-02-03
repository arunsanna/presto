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
package com.facebook.presto.sql.planner.iterative.rule.test;

import com.facebook.presto.metadata.FunctionKind;
import com.facebook.presto.metadata.Signature;
import com.facebook.presto.sql.planner.iterative.rule.SwapAdjacentWindowsByPartitionsOrder;
import com.facebook.presto.sql.planner.plan.WindowNode;
import com.facebook.presto.sql.tree.FunctionCall;
import com.facebook.presto.sql.tree.QualifiedName;
import com.facebook.presto.sql.tree.WindowFrame;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.util.Optional;

import static com.facebook.presto.spi.type.BigintType.BIGINT;
import static com.facebook.presto.spi.type.DoubleType.DOUBLE;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.specification;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.values;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.window;
import static com.facebook.presto.sql.tree.FrameBound.Type.CURRENT_ROW;
import static com.facebook.presto.sql.tree.FrameBound.Type.UNBOUNDED_PRECEDING;

public class TestSwapAdjacentWindowsByPartitionsOrder
{
    private final RuleTester tester = new RuleTester();

    private WindowNode.Frame frame;
    private Signature signature;

    public TestSwapAdjacentWindowsByPartitionsOrder()
    {
        frame = new WindowNode.Frame(WindowFrame.Type.RANGE, UNBOUNDED_PRECEDING,
                Optional.empty(), CURRENT_ROW, Optional.empty());
        signature = new Signature(
                "avg",
                FunctionKind.WINDOW,
                ImmutableList.of(),
                ImmutableList.of(),
                DOUBLE.getTypeSignature(),
                ImmutableList.of(DOUBLE.getTypeSignature()),
                false);
    }

    @Test
    public void doesNotFireOnPlanWithoutWindowFunctions()
            throws Exception
    {
        tester.assertThat(new SwapAdjacentWindowsByPartitionsOrder())
                .on(p -> p.values(p.symbol("a", BIGINT)))
                .doesNotFire();
    }

    @Test
    public void doesNotFireOnPlanWithSingleWindowNode()
            throws Exception
    {
        tester.assertThat(new SwapAdjacentWindowsByPartitionsOrder())
                .on(p -> p.window(new WindowNode.Specification(
                                ImmutableList.of(p.symbol("a", BIGINT)),
                                ImmutableList.of(),
                                ImmutableMap.of()),
                        ImmutableMap.of(p.symbol("a", BIGINT),
                                new WindowNode.Function(new FunctionCall(QualifiedName.of("avg"), ImmutableList.of()), signature, frame)),
                        p.values(p.symbol("a", BIGINT))))
                .doesNotFire();
    }

    @Test
    public void subsetComesFirst()
            throws Exception
    {
        tester.assertThat(new SwapAdjacentWindowsByPartitionsOrder())
                .on(p ->
                        p.window(new WindowNode.Specification(
                                        ImmutableList.of(p.symbol("a", BIGINT)),
                                        ImmutableList.of(),
                                        ImmutableMap.of()),
                                ImmutableMap.of(p.symbol("avg_1", BIGINT),
                                        new WindowNode.Function(new FunctionCall(QualifiedName.of("avg"), ImmutableList.of()), signature, frame)),
                                p.window(new WindowNode.Specification(
                                                ImmutableList.of(p.symbol("a", BIGINT), p.symbol("b", BIGINT)),
                                                ImmutableList.of(),
                                                ImmutableMap.of()),
                                        ImmutableMap.of(p.symbol("avg_2", BIGINT),
                                                new WindowNode.Function(new FunctionCall(QualifiedName.of("avg"), ImmutableList.of()), signature, frame)),
                                        p.values(p.symbol("a", BIGINT), p.symbol("b", BIGINT)))))
                .matches(window(specification(ImmutableList.of("a", "b"), ImmutableList.of(), ImmutableMap.of()),
                        ImmutableList.of(),
                        window(specification(ImmutableList.of("a"), ImmutableList.of(), ImmutableMap.of()),
                                ImmutableList.of(),
                                values(ImmutableMap.of("a", 0, "b", 1)))));
    }
}
