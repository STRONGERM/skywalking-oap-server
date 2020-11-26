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

package org.apache.skywalking.apm.plugin.spring.mvc.commons;

import org.apache.skywalking.apm.agent.core.context.jdk8.Optional;
import org.apache.skywalking.apm.agent.core.util.jdk8.Function;

import java.lang.reflect.Method;

/**
 * Tools for parsing path from annotation
 */
public class ParsePathUtil {

    public static String recursiveParseMethodAnnotaion(Method method, Function<Method, String> parseFunc) {
        String result = parseFunc.apply(method);
        if (result == null) {
            Class<?> declaringClass = method.getDeclaringClass();
//            result = recursiveMatches(declaringClass, method.getName(), method.getParameters(), parseFunc);
        }
        return Optional.ofNullable(result).orElse("");
    }

   /* private static String recursiveMatches(Class claz, String methodName, Parameter[] parameters,
        Function<Method, String> parseFunc) {
        Class<?>[] interfaces = claz.getInterfaces();
        for (Class<?> implInterface : interfaces) {
            String path = recursiveMatches(implInterface, methodName, parameters, parseFunc);
            if (path != null) {
                return path;
            }
            Method[] declaredMethods = implInterface.getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                if (Objects.equals(declaredMethod.getName(), methodName) && parameterEquals(parameters, declaredMethod.getParameters())) {
                    return parseFunc.apply(declaredMethod);
                }
            }
        }
        return null;
    }*/

    /*private static boolean parameterEquals(Parameter[] p1, Parameter[] p2) {
        if (p1.length != p1.length) {
            return false;
        }
        for (int i = 0; i < p1.length; i++) {
            if (!Objects.equals(p1[i].getType().getName(), p2[i].getType().getName())) {
                return false;
            }
        }
        return true;
    }*/
}
