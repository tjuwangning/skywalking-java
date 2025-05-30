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

package org.apache.skywalking.apm.plugin.spring.cloud.gateway.v412x;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

import java.lang.reflect.Method;

import static org.apache.skywalking.apm.network.trace.component.ComponentsDefine.SPRING_CLOUD_GATEWAY;

public class NettyRoutingFilterV412Interceptor implements InstanceMethodsAroundInterceptor {

    private static final String NETTY_ROUTING_FILTER_TRACED_ATTR = NettyRoutingFilterV412Interceptor.class.getName() + ".isTraced";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        ServerWebExchange exchange = (ServerWebExchange) allArguments[0];
        if (isTraced(exchange)) {
            return;
        }

        setTracedStatus(exchange);

        EnhancedInstance enhancedInstance = getInstance(allArguments[0]);

        AbstractSpan span = ContextManager.createLocalSpan("SpringCloudGateway/RoutingFilter");
        if (enhancedInstance != null && enhancedInstance.getSkyWalkingDynamicField() != null) {
            ContextManager.continued((ContextSnapshot) enhancedInstance.getSkyWalkingDynamicField());
        }
        span.setComponent(SPRING_CLOUD_GATEWAY);
    }

    private static void setTracedStatus(ServerWebExchange exchange) {
        exchange.getAttributes().put(NETTY_ROUTING_FILTER_TRACED_ATTR, true);
    }

    private static boolean isTraced(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(NETTY_ROUTING_FILTER_TRACED_ATTR, false);
    }

    private EnhancedInstance getInstance(Object o) {
        EnhancedInstance instance = null;
        if (o instanceof EnhancedInstance) {
            instance = (EnhancedInstance) o;
        } else if (o instanceof ServerWebExchangeDecorator) {
            ServerWebExchange delegate = ((ServerWebExchangeDecorator) o).getDelegate();
            return getInstance(delegate);
        }
        return instance;
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        if (ContextManager.isActive()) {
            // if HttpClientFinalizerSendInterceptor does not invoke, we will stop the span there to avoid context leak.
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}
