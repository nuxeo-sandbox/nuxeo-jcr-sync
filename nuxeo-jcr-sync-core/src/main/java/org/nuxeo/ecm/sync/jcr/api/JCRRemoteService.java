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
package org.nuxeo.ecm.sync.jcr.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.nuxeo.ecm.sync.jcr.service.impl.JCRAceMapping;
import org.nuxeo.ecm.sync.jcr.service.impl.JCRConnectionDescriptor;
import org.nuxeo.ecm.sync.jcr.service.impl.JCRFieldMappingDescriptor;

/**
 * This service connects to a distant repository available via JCR, and loads/provides mapping configuration
 *
 * @since 10.2
 */
public interface JCRRemoteService {

    /**
     * Creates and returns the OpenCmis Session given the connection name, and the corresponding parameters set in the
     * XML configuration
     *
     * @param name of the {@link JCRConnectionDescriptor}
     * @return the created session
     * @since 10.2
     */
    Session createSession(String connectionName);

    /**
     * Returns the Map of remoteDoctype/localDoctype for the given connectionName
     *
     * @param connectionName name of the connection to use
     * @return a map of remoteDoctype/localDoctype
     * @since 10.2
     */
    public Map<String, String> getDoctypeMapping(String connectionName);

    /**
     * Returns the list of {@link JCRFieldMappingDescriptor} for the given connectionName
     *
     * @param connectionName name of the connection to use
     * @param doctype the doc type. Passing null will return all the mapping
     * @return a list of {@link JCRFieldMappingDescriptor}
     * @since 10.2
     */
    List<JCRFieldMappingDescriptor> getFieldMapping(String connectionName, String doctype);

    /**
     * Returns ACEs mapping for the given connectionName, with the mapping method
     *
     * @param connectionName name of the connection to use
     * @return ACEs mapping for the given connectionName. Contains elements whose key is the remote ACE, value is the
     *         local ACE to apply
     * @since 10.2
     */
    JCRAceMapping getAceMappings(String connectionName);

    /**
     * Return the names of all the connections that have been loaded
     *
     * @return the names of all the connections that have been loaded
     * @since 10.2
     */
    Collection<String> getConnectionNames();

    /**
     * Return the descriptor of a specific connection
     *
     * @param connectionName name of a connection
     * @return the descriptor for this connection
     * @since 10.2
     */
    JCRConnectionDescriptor getConnectionDescriptor(String connectionName);

}