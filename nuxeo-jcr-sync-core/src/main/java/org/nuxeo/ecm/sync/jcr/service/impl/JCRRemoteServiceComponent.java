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
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.sync.jcr.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.commons.JcrUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class JCRRemoteServiceComponent extends DefaultComponent implements JCRRemoteService {

    private static final Log log = LogFactory.getLog(JCRRemoteServiceComponent.class);

    public static final String EP_CONNECTION = "connection";

    // Name of connection, map of remoteDocType/localDocType for this connection
    protected Map<String, Map<String, String>> doctypeMapping = null;

    // Name of connection, Map of mapping name/values for this connection
    protected Map<String, Map<String, JCRFieldMappingDescriptor>> fieldMapping = null;

    // Name of connection, ace-mapping for this connection
    protected Map<String, JCRAceMapping> aceMapping = null;

    protected Map<String, JCRConnectionDescriptor> connections = null;

    public JCRRemoteServiceComponent() {
        super();
    }

    @Override
    public void activate(ComponentContext context) {
        doctypeMapping = new HashMap<>();
        fieldMapping = new HashMap<>();
        aceMapping = new HashMap<>();
        connections = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        doctypeMapping = null;
        fieldMapping = null;
        aceMapping = null;
        connections = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (EP_CONNECTION.equals(extensionPoint)) {
            JCRConnectionDescriptor desc = (JCRConnectionDescriptor) contribution;
            String name = desc.getName();

            log.debug("Registering connection: " + name + ", repository: " + desc.getRepository());

            if (!desc.isEnabled()) {
                connections.remove(name);
                log.info("Connection configured to not be enabled: " + name);
                return;
            }

            doctypeMapping.put(name, desc.getDoctypeMapping());

            JCRAceMapping aceMappingMap = new JCRAceMapping(name, desc.getAceMappingMethod(), desc.getAceMapping(),
                    desc.getUserMapping());
            aceMapping.put(name, aceMappingMap);

            List<JCRFieldMappingDescriptor> loadedFieldMapping = desc.getFieldMapping();
            Map<String, JCRFieldMappingDescriptor> fieldMappingMap = new HashMap<>();
            // Load default mappings (could be overridden)
            DefaultFieldMappings.MAPPINGS.forEach(field -> fieldMappingMap.put(field.getName(), field));
            // Load custom mappings
            loadedFieldMapping.forEach(oneDesc -> {
                fieldMappingMap.put(oneDesc.getName(), oneDesc);
            });
            fieldMapping.put(name, fieldMappingMap);

            connections.put(name, desc);
        }
    }

    @Override
    public List<JCRFieldMappingDescriptor> getFieldMapping(String connectionName, String doctype) {
        Map<String, JCRFieldMappingDescriptor> fieldMappingMap = fieldMapping.get(connectionName);
        return fieldMappingMap.values().stream().filter(m -> m.matches(doctype)).collect(Collectors.toList());
    }

    @Override
    public JCRAceMapping getAceMappings(String connectionName) {
        return aceMapping.get(connectionName);
    }

    @Override
    public Collection<String> getConnectionNames() {
        return Collections.unmodifiableSet(connections.keySet());
    }

    @Override
    public Map<String, String> getDoctypeMapping(String connectionName) {
        return doctypeMapping.get(connectionName);
    }

    @Override
    public Session createSession(String connectionName) {
        JCRConnectionDescriptor desc = connections.get(connectionName);
        if (desc == null) {
            throw new IllegalArgumentException("No such connection: " + connectionName);
        }
        if (desc.getUrl() == null) {
            throw new IllegalArgumentException("No URL provided for connection: " + connectionName);
        }

        Credentials creds = new GuestCredentials();
        if (desc.getUsername() == null) {
            if (desc.getCredentials() != null) {
                creds = new TokenCredentials(desc.getCredentials());
            }
        } else if (desc.getUsername() != null && desc.getCredentials() != null) {
            creds = new SimpleCredentials(desc.getUsername(), desc.getCredentials().toCharArray());
        }

        try {
            Repository repository = JcrUtils.getRepository(desc.getUrl());
            Session session = repository.login(creds, desc.getRepository());
            return session;
        } catch (RepositoryException rpe) {
            throw new NuxeoException("Error connecting JCR Repository", rpe,
                    Status.SERVICE_UNAVAILABLE.getStatusCode());
        }
    }

    @Override
    public JCRConnectionDescriptor getConnectionDescriptor(String connectionName) {
        return connections.get(connectionName);
    }

}
