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

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;
import org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants;

public class JCRImportService extends JCROperations implements JCRServiceConstants {

    private static final Log log = LogFactory.getLog(JCRImportService.class);

    protected CoreSession coreSession;

    protected JCRRemoteService jcr;

    protected String connectionName;

    protected String remoteRef;

    protected boolean isIdRef = false;

    protected boolean force = false;

    protected String state;

    public JCRImportService(CoreSession coreSession, JCRRemoteService jcr) {
        super();
        this.coreSession = coreSession;
        this.jcr = jcr;
    }

    public DocumentModel run(DocumentModel target) throws RepositoryException {

        if (!target.isFolder()) {
            throw new IllegalArgumentException("Cannot import non-folderish documents");
        }

        if (coreSession == null) {
            coreSession = target.getCoreSession();
            if (coreSession == null) {
                throw new NuxeoException("No CoreSession available");
            }
        }

        // Get document, check facet
        AtomicReference<String> atomicRemoteRef = new AtomicReference<>(remoteRef);
        AtomicBoolean idRef = new AtomicBoolean(isIdRef);
        DocumentModel model = loadDocument(coreSession, target, atomicRemoteRef, idRef);

        // Validate repository
        Property p = model.getProperty(XPATH_CONNECTION);
        connectionName = validateConnection(p, connectionName);

        // Obtain Session from JCR component
        Property repositoryProperty = model.getProperty(XPATH_REPOSITORY);
        Session repo = createSession(connectionName, repositoryProperty, jcr);

        // Retrieve object
        Node remote = loadObject(repo, atomicRemoteRef.get(), idRef.get());
        checkObject(remote, model);

        // Import children of current path
        if (remote.hasNodes()) {
            NodeIterator obj = remote.getNodes();
            while (obj.hasNext()) {
                importObject(model, obj.nextNode());
            }
        } else {
            log.warn("Remote object has no children: " + remote.getPath());
            // throw new IllegalArgumentException("Cannot import non-folder documents");
        }

        // Save and return
        model = coreSession.saveDocument(model);
        return model;
    }

    private void importObject(DocumentModel model, Node obj) throws RepositoryException {

        if (StringUtils.isBlank(connectionName)) {
            throw new IllegalArgumentException("connectioName was not initialized");
        }

        Map<String, String> doctypeMapping = jcr.getDoctypeMapping(connectionName);

        String docType = "File";
        NodeType remoteDocType = obj.getPrimaryNodeType();
        String rdocType = remoteDocType.getName();
        switch (rdocType) {
        case "nt:file":
        case "nt:linkedFile":
        case "nt:resource":
            docType = StringUtils.defaultIfBlank(doctypeMapping.get(rdocType), "File");
            break;

        case "nt:folder":
            docType = StringUtils.defaultIfBlank(doctypeMapping.get(rdocType), "Folder");
            break;

        default:
            log.warn("Unregistered JCR type: " + rdocType + ", using: " + docType);
            break;
        }

        try {
            DocumentModel child = coreSession.createDocumentModel(model.getPathAsString(), obj.getName(), docType);
            child.addFacet(SYNC_FACET);
            child.setPropertyValue("dc:title", obj.getName());
            child.setPropertyValue(XPATH_REMOTE_UID, obj.getIdentifier());
            child.setPropertyValue(XPATH_TYPE, rdocType);

            child.setPropertyValue(XPATH_CONNECTION, connectionName);
            child.setPropertyValue(XPATH_REPOSITORY, model.getPropertyValue(XPATH_REPOSITORY));
            child.setPropertyValue(XPATH_STATE, state);

            child = coreSession.getOrCreateDocument(child);
        } catch (Exception ex) {
            log.error("Error creating document", ex);
            throw new RuntimeException(ex);
        }
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setConnectionName(String connectioName) {
        connectionName = connectioName;
    }

    public void setRemoteRef(String remoteRef) {
        this.remoteRef = remoteRef;
    }

    public void setIsIdRef(boolean isIdRef) {
        this.isIdRef = isIdRef;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

}
