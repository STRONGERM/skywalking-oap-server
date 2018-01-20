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

package org.apache.skywalking.apm.collector.ui.graphql.base;

import org.junit.Assert;

/**
 * @author peng-yongsheng
 */
@GraphQLQueryType
public class IntegerInputQueryType {

    public Integer test(Integer arg1, Integer arg2, Integer arg3) {
        Assert.assertEquals(new Integer(1), arg1);
        Assert.assertEquals(new Integer(1), arg2);
        Assert.assertEquals(new Integer(1), arg3);
        return 0;
    }
}
