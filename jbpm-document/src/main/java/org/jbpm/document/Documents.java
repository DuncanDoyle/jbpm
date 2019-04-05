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

package org.jbpm.document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A collection of {@link Document Documents}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "documents-object")
public class Documents implements Serializable {

	private static final long serialVersionUID = 6962662228758156488L;
	
	@XmlElementWrapper
	@XmlAnyElement(lax=true)
	private List<Document> documents = new ArrayList<>();

	public Documents() {
	}

	public Documents(List<Document> documents) {
		this.documents = new ArrayList<Document>(documents);
	}
	
	public void addDocument(Document document) {
		documents.add(document);
	}
	
	public List<Document> getDocuments() {
	    return documents;
	}	
	
}

