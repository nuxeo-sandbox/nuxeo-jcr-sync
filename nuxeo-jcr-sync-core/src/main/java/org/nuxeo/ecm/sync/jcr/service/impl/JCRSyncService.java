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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;
import org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants;
import org.nuxeo.runtime.api.Framework;

public class JCRSyncService extends JCROperations implements JCRServiceConstants {

    private static final Log log = LogFactory.getLog(JCRSyncService.class);

    protected CoreSession coreSession;

    protected JCRRemoteService jcr;

    protected String connectionName;

    protected String remoteRef;

    protected boolean isIdRef = false;

    protected boolean force = false;

    protected String state;

    protected boolean isContent = true;

    protected String contentXPath = "file:content";

    protected UserManager userManager = null;

    public JCRSyncService(CoreSession session, JCRRemoteService jcr) {
        super();
        coreSession = session;
        this.jcr = jcr;
    }

    public DocumentModel run(DocumentModel target) throws RepositoryException {

        // Get document, check facet
        AtomicReference<String> atomicRemoteRef = new AtomicReference<>(remoteRef);
        AtomicBoolean idRef = new AtomicBoolean(isIdRef);
        DocumentModel model = loadDocument(coreSession, target, atomicRemoteRef, idRef);

        // Validate repository
        Property connectionProperty = model.getProperty(XPATH_CONNECTION);
        connectionName = validateConnection(connectionProperty, connectionName);

        // Obtain Session from JCR component
        Property repositoryProperty = model.getProperty(XPATH_REPOSITORY);
        Session repo = createSession(connectionName, repositoryProperty, jcr);

        // Retrieve object
        Node remote = loadObject(repo, atomicRemoteRef.get(), idRef.get());
        checkObject(remote, model);

        // Update document
        if (requiresUpdate(remote, model, force)) {
            // -------------------------------------> Update fields
            List<JCRFieldMappingDescriptor> descs = jcr.getFieldMapping(connectionName,
                    model.getDocumentType().getName());
            for (JCRFieldMappingDescriptor desc : descs) {
                if (!remote.hasProperty(desc.getProperty())) {
                    continue;
                }
                javax.jcr.Property val = remote.getProperty(desc.getProperty());
                Property dp = model.getProperty(desc.getXpath());
                if (val != null) {
                    dp.setValue(val);
                } else {
                    dp.remove();
                }
            }

            log.warn("Sync: " + isContent + ", " + remote.isNodeType(NodeType.NT_FILE) + ", "
                    + remote.hasNode(Node.JCR_CONTENT));

            if (isContent && remote.isNodeType(NodeType.NT_FILE) && remote.hasNode(Node.JCR_CONTENT)) {
                try {
                    Node file = remote.getNode(Node.JCR_CONTENT);
                    if (file.hasProperty(javax.jcr.Property.JCR_DATA)) {
                        Binary bin = file.getProperty(javax.jcr.Property.JCR_DATA).getBinary();
                        if (bin != null) {
                            Blob blb = Blobs.createBlob(bin.getStream());
                            blb.setFilename(remote.getName());
                            if (file.hasProperty(javax.jcr.Property.JCR_MIMETYPE)) {
                                blb.setMimeType(file.getProperty(javax.jcr.Property.JCR_MIMETYPE).getString());
                            }
                            DocumentHelper.addBlob(model.getProperty(contentXPath), blb);
                            model.setPropertyValue(XPATH_URI, remote.getPath());
                        }
                    }
                } catch (IOException iox) {
                    log.warn("Unable to copy remote content", iox);
                }
            }

            // -------------------------------------> Update permissions
            JCRAceMapping aceMapping = jcr.getAceMappings(connectionName);
            // Assume it is not null
            model = aceMapping.applyMapping(coreSession, model, repo, remote);

        }

        // Set sync attributes
        updateSyncAttributes(remote, model, state);

        // Save and return
        model = coreSession.saveDocument(model);
        return model;
    }

    protected UserManager getUserManager() {
        if (userManager == null) {
            userManager = Framework.getService(UserManager.class);
        }

        return userManager;
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

    public boolean isContent() {
        return isContent;
    }

    public void setIsContent(boolean isContent) {
        this.isContent = isContent;
    }

    public String getContentXPath() {
        return contentXPath;
    }

    public void setContentXPath(String contentXPath) {
        this.contentXPath = contentXPath;
    }

}
