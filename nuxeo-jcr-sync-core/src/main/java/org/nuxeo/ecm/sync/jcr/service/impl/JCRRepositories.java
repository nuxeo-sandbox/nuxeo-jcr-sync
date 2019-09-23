/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Damon Brown
 */
package org.nuxeo.ecm.sync.jcr.service.impl;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Instantiations;
import org.nuxeo.ecm.core.io.registry.reflect.Priorities;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Synchronize individual documents
 */
@Setup(mode = Instantiations.SINGLETON, priority = Priorities.REFERENCE)
public class JCRRepositories extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "jcrRepos";

    public JCRRepositories() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel doc) throws IOException {
        JCRRemoteService jcr = Framework.getService(JCRRemoteService.class);
        jg.writeFieldName(NAME);
        jg.writeObject(jcr.getConnectionNames());
    }

}
