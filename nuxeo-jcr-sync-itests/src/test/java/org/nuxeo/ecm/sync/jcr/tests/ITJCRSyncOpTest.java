/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.sync.jcr.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;
import org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants;
import org.nuxeo.ecm.sync.jcr.operations.JCRSync;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.sync.jcr", "org.nuxeo.ecm.sync.jcr:OSGI-INF/jcr-repository-test-contribs.xml" })
public class ITJCRSyncOpTest extends BaseTest {

    static final Log log = LogFactory.getLog(ITJCRSyncOpTest.class);

    static final String LOCAL_USER = "localuser1";

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected AutomationService service;

    @Inject
    protected EventService eventService;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Inject
    protected UserManager userManager;

    @Inject
    protected JCRRemoteService jcr;

    protected DocumentModel src;

    @Before
    public void initRepo() throws Exception {

        // initRemoteDocuments();

        assertNotNull(jcr);

        coreSession.removeChildren(coreSession.getRootDocument().getRef());
        coreSession.save();

        src = coreSession.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = coreSession.createDocument(src);
        coreSession.save();
        src = coreSession.getDocument(src.getRef());
       
        createLocalGroup(GROUP1);
        createLocalUser(USER1);
        createLocalUser(LOCAL_USER);
        NuxeoPrincipal principal = userManager.getPrincipal(USER1);
        principal.setGroups(Arrays.asList(GROUP_MEMBERS, GROUP1));
        userManager.updateUser(principal.getModel());

    }

    protected DocumentModel syncAndCheckMetadata(String connectionName) throws Exception {

        final String path = "/src/file";
        final String remote = TEST_FILE_PATH;

        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID)
             .set("type", "File")
             .set("name", "file")
             .set("properties", "dc:title=" + TEST_FILE_TITLE);
        // ------------------------------> Add a permission for later check
        chain.add("Document.AddPermission").set("permission", SecurityConstants.READ_WRITE).set("username", LOCAL_USER);
        // ----------------------------------------------------------------
        chain.add(JCRSync.ID).set("connection", connectionName).set("remoteRef", remote);

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        coreSession.save();

        eventService.waitForAsyncCompletion();
        while (eventServiceAdmin.getEventsInQueueCount() > 0) {
            eventService.waitForAsyncCompletion();
            Thread.yield();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        doc.refresh();

        // Check values
        assertEquals(path, doc.getPathAsString());
        assertEquals(connectionName, doc.getPropertyValue(JCRServiceConstants.XPATH_CONNECTION));
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals(TEST_FILE_BLOB_NAME, blob.getFilename());
        assertEquals(TEST_FILE_BLOB_SIZE, blob.getLength());

        return doc;
    }

    @Test
    public void testJCRSyncOperationWithAddPermissions() throws Exception {

        DocumentModel doc = syncAndCheckMetadata(CONNECTION_NUXEO_ADD_PERMS);
        assertNotNull(doc);
        if (doc != null) {
            return;
        }

        // Check permissions
        // This has to be checked against what is created in BaseTest#initRemoteDocuments
        // The service should have added the permission in a specifi ACL.
        // We must have our permissions in this acl
        ACL syncAcl = doc.getACP().getACL(JCRServiceConstants.SYNC_ACL);
        assertNotNull(syncAcl);

        boolean user1CanEverything = false;
        boolean group1CanReadWrite = false;
        boolean group1HasCustomPermission = false;

        String aceUserName;
        for (ACE ace : syncAcl.getACEs()) {
            aceUserName = ace.getUsername();
            if (GROUP1.equals(aceUserName)) {
                if (SecurityConstants.READ_WRITE.equals(ace.getPermission())) {
                    group1CanReadWrite = true;
                } else if (CUSTOM_PERM_NOT_MAPPED.equals(ace.getPermission())) {
                    group1HasCustomPermission = true;
                }
            } else if (USER1.equals(aceUserName) && "Everything".equals(ace.getPermission())) {
                user1CanEverything = true;
            }
        }

        assertTrue(user1CanEverything);
        assertTrue(group1CanReadWrite);
        assertTrue(group1HasCustomPermission);

        // When the doc to sync was created, a permission was added (see syncAndCheckMetadata)
        // and it must still exist when testing the CONNECTION_NUXEO_ADD_PERMS connection
        NuxeoPrincipal principal = userManager.getPrincipal(LOCAL_USER);
        boolean localUserCanReadWrite = coreSession.hasPermission(principal, doc.getRef(),
                SecurityConstants.READ_WRITE);
        ;
        assertTrue(localUserCanReadWrite);

    }

    @Test
    public void testJCRSyncOperationWithReplacePermissions() throws Exception {

        DocumentModel doc = syncAndCheckMetadata(CONNECTION_NUXEO_REPLACE_PERMS);
        assertNotNull(doc);
        if (doc != null) {
            return;
        }

        // Check permissions
        // This has to be checked against what is created in BaseTest#initRemoteDocuments
        // The service should have added the permission in a specifi ACL.
        // We must have our permissions in this acl
        ACL acl = doc.getACP().getACL(JCRServiceConstants.SYNC_ACL);
        assertNotNull(acl);

        boolean user1CanEverything = false;
        boolean group1CanReadWrite = false;
        boolean group1HasCustomPermission = false;

        String aceUserName;
        for (ACE ace : acl.getACEs()) {
            aceUserName = ace.getUsername();
            if (GROUP1.equals(aceUserName)) {
                if (SecurityConstants.READ_WRITE.equals(ace.getPermission())) {
                    group1CanReadWrite = true;
                } else if (CUSTOM_PERM_NOT_MAPPED.equals(ace.getPermission())) {
                    group1HasCustomPermission = true;
                }
            } else if (USER1.equals(aceUserName) && "Everything".equals(ace.getPermission())) {
                user1CanEverything = true;
            }
        }

        assertTrue(user1CanEverything);
        assertTrue(group1CanReadWrite);
        assertTrue(group1HasCustomPermission);

        // When the doc to sync was created, a permission was added (see syncAndCheckMetadata)
        // and it must not exist when testing the CONNECTION_NUXEO_REPLACE_PERMS connection
        NuxeoPrincipal principal = userManager.getPrincipal(LOCAL_USER);
        boolean localUserCanReadWrite = coreSession.hasPermission(principal, doc.getRef(),
                SecurityConstants.READ_WRITE);
        ;
        assertFalse(localUserCanReadWrite);

    }

    protected void createLocalUser(String userId) {
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", userId);
        userModel.setProperty("user", "password", userId);
        userManager.createUser(userModel);
    }

    protected void createLocalGroup(String groupId) {
        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", groupId);
        userManager.createGroup(groupModel);
    }

}
