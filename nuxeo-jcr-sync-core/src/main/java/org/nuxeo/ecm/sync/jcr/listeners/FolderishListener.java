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
package org.nuxeo.ecm.sync.jcr.listeners;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;
import org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants;
import org.nuxeo.ecm.sync.jcr.service.impl.JCRImportService;
import org.nuxeo.runtime.api.Framework;

public class FolderishListener implements EventListener, PostCommitEventListener, JCRServiceConstants {

    static final Log log = LogFactory.getLog(FolderishListener.class);

    protected JCRRemoteService jcr;

    public FolderishListener() {
        super();
    }

    private void checkServices() {
        if (jcr == null) {
            jcr = Framework.getService(JCRRemoteService.class);
        }
    }

    @Override
    public void handleEvent(EventBundle events) {
        checkServices();
        for (Event evt : events) {
            if (evt.getContext() instanceof DocumentEventContext) {
                DocumentEventContext context = (DocumentEventContext) evt.getContext();
                if (filterDoc(context.getSourceDocument())) {
                    execute(context);
                }
            }
        }
    }

    @Override
    public void handleEvent(Event evt) {
        checkServices();
        if (evt.getContext() instanceof DocumentEventContext) {
            DocumentEventContext context = (DocumentEventContext) evt.getContext();
            if (filterDoc(context.getSourceDocument())) {
                execute(context);
            }
        }
    }

    private boolean filterDoc(DocumentModel model) {
        return model != null && model.hasFacet(SYNC_FACET) && model.hasFacet("Folderish")
                && model.getPropertyValue(XPATH_REMOTE_UID) != null && model.getPropertyValue(XPATH_STATE) != null
                && model.getPropertyValue(XPATH_STATE).equals("sync");
    }

    private void execute(DocumentEventContext context) {
        DocumentModel model = context.getSourceDocument();
        model.setPropertyValue(XPATH_STATE, "traversed");

        JCRImportService imp = new JCRImportService(context.getCoreSession(), jcr);
        imp.setState("queued");

        try {
            imp.run(model);
        } catch (RepositoryException rpe) {
            log.error("Unable to synchronize folder :: " + model, rpe);
        }
    }

}
