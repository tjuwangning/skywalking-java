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

package test.apache.skywalking.apm.testcase.spring3.service;

import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import test.apache.skywalking.apm.testcase.spring3.component.TestComponentBean;
import test.apache.skywalking.apm.testcase.spring3.dao.TestRepositoryBean;

@Service
public class TestServiceBean {
    @Autowired
    private TestComponentBean componentBean;

    @Autowired
    private TestRepositoryBean repositoryBean;

    public void doSomeBusiness(String name) {
        componentBean.componentMethod(name);
        repositoryBean.doSomeStuff(name);
    }

    // Test the class is enhanced by both SkyWalking and Spring AOP
    @Retryable(value = Exception.class, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Trace
    private void doRetry() {
    }
}
