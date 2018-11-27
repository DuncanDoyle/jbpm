/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.jbpm.process.workitem.camel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.util.URISupport;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.services.api.service.ServiceRegistry;
import org.kie.api.executor.Command;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.WorkItem;
import org.kie.internal.runtime.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CamelCommand implements Command,
                                          Cacheable {

	private static final String GLOBAL_CAMEL_CONTEXT_SERVICE_KEY = "GlobalCamelService";
	private static final String RUNTIME_CAMEL_CONTEXT_SERVICE_POSTFIX = "_CamelService";
	
    private static final Logger logger = LoggerFactory.getLogger(CamelCommand.class);

	// Maintain a producerTemplate per CamelContext
	private volatile static ProducerTemplate producerTemplate;


	public CamelCommand() {
		CamelContext globalCamelContext = (CamelContext) ServiceRegistry.get().service(GLOBAL_CAMEL_CONTEXT_SERVICE_KEY);
		// TODO: Should we allow to set the maximumCacheSize on the producer?
		this.producerTemplate = globalCamelContext.createProducerTemplate();
	}
	
	public CamelCommand(RuntimeManager runtimeManager) {
		String runtimeCamelContextKey = runtimeManager.getIdentifier() + RUNTIME_CAMEL_CONTEXT_SERVICE_POSTFIX;
		CamelContext runtimeCamelContext = (CamelContext) ServiceRegistry.get().service(runtimeCamelContextKey);
		// TODO: Should we allow to set the maximumCacheSize on the producer?
		this.producerTemplate = runtimeCamelContext.createProducerTemplate();
	}
	
	
	
    @Override
    public ExecutionResults execute(CommandContext ctx) throws Exception {
        
    	WorkItem workItem = (WorkItem) ctx.getData("workItem");
    	
    	String camelRouteId = (String) workItem.getParameter("camel-route-id");
		
		// We only support direct. We don't need to support more, as direct simply gives us the entrypoint into the actual Camel Routes.
		String camelUri = "direct://" + camelRouteId;
		
		Exchange inExchange = ExchangeBuilder.anExchange(producerTemplate.getCamelContext()).withBody(workItem).build();
		Exchange outExchange = producerTemplate.send(camelUri, inExchange);
		Message out = outExchange.getOut();
		
		ExecutionResults results = new ExecutionResults();
		Object response = out.getBody();
		results.setData("response", response);
		Map<String, Object> messageHeaders = out.getHeaders();
		results.setData("out-headers", messageHeaders);
    	
        return results;
    }

    
	@Override
	public void close() {
		try {
			this.producerTemplate.stop();
		} catch (Exception e) {
			logger.warn("Error encountered while closing the Camel Producer Template.", e);
			// Not much we can do here, so swallowing exception.
		}
	}

}