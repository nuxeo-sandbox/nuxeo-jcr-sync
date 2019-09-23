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

import static org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants.SYNC_FACET;
import static org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants.XPATH_MODIFIED;
import static org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants.XPATH_REMOTE_UID;
import static org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants.XPATH_STATE;
import static org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants.XPATH_SYNCHRONIZED;
import static org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants.XPATH_TYPE;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;

public abstract class JCROperations {

    private static final Log log = LogFactory.getLog(JCROperations.class);

    public JCROperations() {
        super();
    }

    protected DocumentModel loadDocument(CoreSession session, DocumentModel target, AtomicReference<String> remoteRef,
            AtomicBoolean idRef) {
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (target == null) {
            throw new NullPointerException("document");
        }
        DocumentModel model = target;
        if (!model.hasFacet(SYNC_FACET)) {
            model.addFacet(SYNC_FACET);
        }

        if (remoteRef.get() == null) {
            remoteRef.set((String) model.getPropertyValue(XPATH_REMOTE_UID));
            idRef.set(true);
            if (remoteRef.get() == null) {
                throw new IllegalArgumentException("UID or path required for sync");
            }
        }
        return model;
    }

    protected String validateConnection(Property connectionProperty, String connection) {
        Property connect = connectionProperty;
        if (connect.getValue() != null && connection != null) {
            if (!connect.getValue().equals(connection)) {
                throw new IllegalArgumentException("Mis-matched repository connection");
            }
        } else if (connection != null) {
            connect.setValue(connection);
        } else {
            connection = (String) connect.getValue();
        }
        return connection;
    }

    protected Session createSession(String connectionName, Property repositoryProperty, JCRRemoteService jcr) {
        Session repo = jcr.createSession(connectionName);
        return repo;
    }

    protected Node loadObject(Session repo, String remoteRef, boolean idRef) {
        if (!remoteRef.startsWith("/") && !idRef) {
            log.warn("Using ID reference for non-path like value: " + remoteRef);
            idRef = true;
        }

        Node remote = null;
        try {
            if (idRef) {
                remote = repo.getNodeByIdentifier(remoteRef);
            } else {
                remote = repo.getNode(remoteRef);
            }
        } catch (RepositoryException e) {
            // Nothing, remote stays null
        }

        if (remote == null) {
            throw new IllegalArgumentException("Remote " + (idRef ? "id" : "path") + " not found: " + remoteRef);
        }

        return remote;
    }

    protected void checkObject(Node remote, DocumentModel model) throws RepositoryException {
        // Set required identifying information
        if (model.getPropertyValue(XPATH_REMOTE_UID) == null) {
            model.setPropertyValue(XPATH_REMOTE_UID, remote.getIdentifier());
            model.setPropertyValue(XPATH_TYPE, remote.getPrimaryNodeType().getName());
        } else if (!model.getPropertyValue(XPATH_REMOTE_UID).equals(remote.getIdentifier())) {
            throw new IllegalArgumentException("Mis-matched remote document UUID: "
                    + model.getPropertyValue(XPATH_REMOTE_UID) + " != " + remote.getIdentifier());
        }
    }

    protected boolean requiresUpdate(Node remote, DocumentModel doc, boolean force) throws RepositoryException {
        // Check Last Modified
        GregorianCalendar syncRef = (GregorianCalendar) doc.getPropertyValue(XPATH_SYNCHRONIZED);
        Calendar cal = null;
        if (remote.hasProperty(javax.jcr.Property.JCR_LAST_MODIFIED)) {
            cal = remote.getProperty(javax.jcr.Property.JCR_LAST_MODIFIED).getDate();
        }

        return force || syncRef == null || cal == null || cal.after(syncRef);
    }

    protected DocumentModel updateSyncAttributes(Node remote, DocumentModel doc, String state)
            throws RepositoryException {
        if (state != null) {
            doc.setPropertyValue(XPATH_STATE, state);
        }
        doc.setPropertyValue(XPATH_SYNCHRONIZED, new Date());
        if (remote.hasProperty(javax.jcr.Property.JCR_LAST_MODIFIED)) {
            doc.setPropertyValue(XPATH_MODIFIED,
                    remote.getProperty(javax.jcr.Property.JCR_LAST_MODIFIED).getDate().getTime());
        }

        return doc;
    }

}
