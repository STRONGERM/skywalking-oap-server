/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.toolkit.activation.meter.util;

import org.apache.skywalking.apm.agent.core.meter.MeterId;
import org.apache.skywalking.apm.agent.core.meter.MeterTag;
import org.apache.skywalking.apm.agent.core.meter.MeterType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MeterIdConverter {

    /**
     * Convert toolkit id to agent core id
     */
    public static MeterId convert(org.apache.skywalking.apm.toolkit.meter.MeterId id) {
        final String meterName = id.getName();
        MeterType type = convertType(id);
        List<MeterTag> tags = Collections.emptyList();
        if (id.getTags() != null) {
            tags = id.getTags().stream().map(new Function<org.apache.skywalking.apm.toolkit.meter.MeterId.Tag, MeterTag>() {
                @Override
                public MeterTag apply(org.apache.skywalking.apm.toolkit.meter.MeterId.Tag t) {
                    return new MeterTag(t.getName(), t.getValue());
                }
            }).collect(new Supplier<List<MeterTag>>() {
                @Override
                public List<MeterTag> get() {
                    return new ArrayList();
                }
            }, new BiConsumer<List<MeterTag>, MeterTag>() {
                @Override
                public void accept(List<MeterTag> meterTags, MeterTag meterTag) {
                    meterTags.add(meterTag);
                }
            }, new BiConsumer<List<MeterTag>, List<MeterTag>>() {
                @Override
                public void accept(List<MeterTag> meterTags, List<MeterTag> meterTags2) {
                    meterTags.addAll(meterTags2);
                }
            });
        }

        return new MeterId(meterName, type, tags);
    }

    private static MeterType convertType(org.apache.skywalking.apm.toolkit.meter.MeterId id) {
        switch (id.getType()) {
            case GAUGE:
                return MeterType.GAUGE;
            case COUNTER:
                return MeterType.COUNTER;
            case HISTOGRAM:
                return MeterType.HISTOGRAM;
            default:
                throw new IllegalStateException("Could not found the meter type: " + id.getType());
        }
    }
}
