/*
 * Copyright (c) 2023 Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.server.aspect;

import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.error.Throws;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
@Aspect
@SuppressWarnings("unused")
public class ConductorErrorInterceptorAspect {

    @Pointcut("@annotation(io.appform.conductor.model.error.Throws)")
    public void monitoredFunctionCalled() {
        //Empty as required
    }

    @Pointcut("execution(* *(..))")
    public void anyFunctionCalled() {
        //Empty as required
    }

    @Around("monitoredFunctionCalled() && anyFunctionCalled()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        }
        catch (Throwable t) {
            if ((t instanceof ConductorException)) {
                throw t;
            }
            final var callSignature = joinPoint.getSignature();
            final var methodSignature = (MethodSignature) callSignature;
            final var annotation = methodSignature.getMethod().getAnnotation(Throws.class);
            final var fixedParams = Arrays.stream(Objects.requireNonNullElse(annotation.fixedParams(),
                                                                             new Throws.Param[0]))
                    .collect(Collectors.toUnmodifiableMap(Throws.Param::name, Throws.Param::value));
            final var params = new HashMap<String, Object>(fixedParams);
            IntStream.range(0, methodSignature.getMethod().getParameterCount())
                    .forEach(i -> {
                        final var runtimeParam = methodSignature.getMethod().getParameters()[i]
                                .getAnnotation(Throws.RuntimeParam.class);
                        if (runtimeParam != null) {
                            params.put(runtimeParam.value(), Objects.toString(joinPoint.getArgs()[i]));
                        }
                    });
            throw new ConductorException(annotation.value(), params, t);
        }
    }
}
