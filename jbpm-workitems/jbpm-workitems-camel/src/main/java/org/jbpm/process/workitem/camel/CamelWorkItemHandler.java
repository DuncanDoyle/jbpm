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

package org.jbpm.process.workitem.camel;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.jbpm.process.workitem.core.AbstractLogOrThrowWorkItemHandler;
import org.jbpm.services.api.service.ServiceRegistry;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelWorkItemHandler extends AbstractLogOrThrowWorkItemHandler implements Cacheable {

	private static final String GLOBAL_CAMEL_CONTEXT_SERVICE_KEY = "GlobalCamelService";
	private static final String RUNTIME_CAMEL_CONTEXT_SERVICE_POSTFIX = "_CamelService";

	private static Logger logger = LoggerFactory.getLogger(CamelWorkItemHandler.class);

	// Maintain a producerTemplate per CamelContext
	// private ConcurrentHashMap<String, ProducerTemplate> producerTemplates = new ConcurrentHashMap<String, ProducerTemplate>();

	private ProducerTemplate producerTemplate;

	/**
	 * Default Constructor. This creates a {@link ProducerTemplate} for the global {@link CamelContext}.
	 */
	public CamelWorkItemHandler() {
		CamelContext globalCamelContext = (CamelContext) ServiceRegistry.get().service(GLOBAL_CAMEL_CONTEXT_SERVICE_KEY);
		// TODO: Should we allow to set the maximumCacheSize on the producer?
		this.producerTemplate = globalCamelContext.createProducerTemplate();
	}

	/**
	 * Constructor which accepts {@link RuntimeManager}. This causes this WorkItemHanlder to create a {@link ProducerTemplate} for the
	 * runtime specific {@link CamelContext}.
	 */
	public CamelWorkItemHandler(RuntimeManager runtimeManager) {
		String runtimeCamelContextKey = runtimeManager.getIdentifier() + RUNTIME_CAMEL_CONTEXT_SERVICE_POSTFIX;
		CamelContext runtimeCamelContext = (CamelContext) ServiceRegistry.get().service(runtimeCamelContextKey);
		// TODO: Should we allow to set the maximumCacheSize on the producer?
		this.producerTemplate = runtimeCamelContext.createProducerTemplate();
	}

	public void executeWorkItem(WorkItem workItem, final WorkItemManager manager) {
		String camelRouteId = (String) workItem.getParameter("camel-route-id");
		
		// We only support direct. We don't need to support more, as direct simply gives us the entrypoint into the actual Camel Routes.
		String camelUri = "direct://" + camelRouteId;
		
		Exchange inExchange = ExchangeBuilder.anExchange(producerTemplate.getCamelContext()).withBody(workItem).build();
		Exchange outExchange = producerTemplate.send(camelUri, inExchange);
		Message out = outExchange.getOut();
		
		Map<String, Object> result = new HashMap<>();
		Object response = out.getBody();
		result.put("response", response);
		Map<String, Object> messageHeaders = out.getHeaders();
		result.put("out-headers", messageHeaders);

		manager.completeWorkItem(workItem.getId(), result);
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, cannot be aborted
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