/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
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

package org.jbpm.process.workitem.webservice;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.process.workitem.camel.CamelWorkItemHandler;
import org.jbpm.process.workitem.core.TestWorkItemManager;
import org.jbpm.services.api.service.ServiceRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.manager.RuntimeManager;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.qos.logback.core.net.server.Client;

@RunWith(MockitoJUnitRunner.class)
public class CamelWorkItemHandlerTest {

    @Mock
    ProducerTemplate producerTemplate;

    @Mock
    ConcurrentHashMap<String, Client> clients;
    
    @Mock
    Exchange outExchange;
    
    @Mock
    Message outMessage;
    
    @Mock
    CamelContext camelContext;
    
    @Mock
    RuntimeManager runtimeManager;

    @Test
    public void testExecuteGlobalCamelContext() throws Exception {
    
    	String camelRouteId = "testCamelRoute";
    	String camelRouteUri = "direct://" + camelRouteId;
    	
    	String testReponse = "testResponse";
    	
    	when(producerTemplate.send(eq(camelRouteUri), any(Exchange.class))).thenReturn(outExchange);
    	when(producerTemplate.send(argThat(not(camelRouteUri)), any(Exchange.class))).thenThrow(new IllegalArgumentException("Unexpected route id"));
    	when(producerTemplate.getCamelContext()).thenReturn(camelContext);
    	
    	when(camelContext.createProducerTemplate()).thenReturn(producerTemplate);
    	
    	when(outExchange.getOut()).thenReturn(outMessage);
    	when(outMessage.getBody()).thenReturn(testReponse);
    	
    	ServiceRegistry.get().register("GlobalCamelService", camelContext);
    	
    	
        TestWorkItemManager manager = new TestWorkItemManager();
        WorkItemImpl workItem = new WorkItemImpl();
        workItem.setParameter("camel-route-id", camelRouteId);
        workItem.setParameter("request", "someRequest");
        
        CamelWorkItemHandler handler = new CamelWorkItemHandler();
        
        handler.executeWorkItem(workItem,
                                manager);
        assertNotNull(manager.getResults());
        assertEquals(1,
                     manager.getResults().size());
        assertTrue(manager.getResults().containsKey(workItem.getId()));
        Map<String, Object> results = manager.getResults(workItem.getId());
        assertEquals(2, results.size());
        assertEquals(testReponse, results.get("response"));
    }
    
    @Test
    public void testExecuteLocalCamelContext() throws Exception {
    
    	String camelRouteId = "testCamelRoute";
    	String camelRouteUri = "direct://" + camelRouteId;
    	
    	String testReponse = "testResponse";
    	
    	String runtimeManagerId = "testRuntimeManager";
    	
    	when(runtimeManager.getIdentifier()).thenReturn(runtimeManagerId);
    	
    	when(producerTemplate.send(eq(camelRouteUri), any(Exchange.class))).thenReturn(outExchange);
    	when(producerTemplate.send(argThat(not(camelRouteUri)), any(Exchange.class))).thenThrow(new IllegalArgumentException("Unexpected route id"));
    	when(producerTemplate.getCamelContext()).thenReturn(camelContext);
    	
    	when(camelContext.createProducerTemplate()).thenReturn(producerTemplate);
    	
    	when(outExchange.getOut()).thenReturn(outMessage);
    	when(outMessage.getBody()).thenReturn(testReponse);
    	
    	//Register the RuntimeManager bound camelcontext.
    	ServiceRegistry.get().register(runtimeManagerId + "_CamelService", camelContext);
    	
    	
        TestWorkItemManager manager = new TestWorkItemManager();
        WorkItemImpl workItem = new WorkItemImpl();
        workItem.setParameter("camel-route-id", camelRouteId);
        workItem.setParameter("request", "someRequest");
        
        CamelWorkItemHandler handler = new CamelWorkItemHandler(runtimeManager);
        
        handler.executeWorkItem(workItem,
                                manager);
        assertNotNull(manager.getResults());
        assertEquals(1,
                     manager.getResults().size());
        assertTrue(manager.getResults().containsKey(workItem.getId()));
        Map<String, Object> results = manager.getResults(workItem.getId());
        assertEquals(2, results.size());
        assertEquals(testReponse, results.get("response"));
    }


}
