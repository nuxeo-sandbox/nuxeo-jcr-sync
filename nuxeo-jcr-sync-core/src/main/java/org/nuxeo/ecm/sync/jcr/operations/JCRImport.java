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
package org.nuxeo.ecm.sync.jcr.operations;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;
import org.nuxeo.ecm.sync.jcr.service.impl.JCRImportService;

/**
 * Synchronize individual documents
 */
@Operation(id = JCRImport.ID, category = Constants.CAT_FETCH, label = "JCR Structure Import", description = "Import JCR content with a remote repository.")
public class JCRImport {

    public static final String ID = "Repository.JCRImport";

    static final Log log = LogFactory.getLog(JCRImport.class);

    @Context
    protected CoreSession session;

    @Context
    protected JCRRemoteService jcr;

    @Param(name = "connection", required = false)
    protected String connection;

    @Param(name = "remoteRef", required = false)
    protected String remoteRef;

    @Param(name = "idRef", required = false, values = "false")
    protected boolean idRef = false;

    @Param(name = "force", required = false, values = "false")
    protected boolean force = false;

    @Param(name = "state", required = false)
    protected String state;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel target) {

        JCRImportService jcrImport = new JCRImportService(session, jcr);

        jcrImport.setConnectionName(connection);
        jcrImport.setRemoteRef(remoteRef);
        jcrImport.setIsIdRef(idRef);
        jcrImport.setForce(force);
        jcrImport.setState(state);

        try {
            DocumentModel result = jcrImport.run(target);
            return result;
        } catch (RepositoryException rpe) {
            throw new NuxeoException("Unable to synchronize folder", rpe, Status.SERVICE_UNAVAILABLE.getStatusCode());
        }
    }

}
