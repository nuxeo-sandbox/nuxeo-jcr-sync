/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.sync.jcr.service.impl;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.2
 */
// Update ACL
// It's actually super hard to synchronize permissions because
// of the inheritence in both sides, remote and local.
// So, for the scope of this POC, we just
// add the permissions if they are not already are set.
// If a permission is not in hte mapping, it is still added?
// IMPORTANT: If the user or the group does not exist in the system, the whole
// thing fails, we must give up because we can't get the Principal and can't
// that checks permission using ACL, Security policies etc. etc.
// (just CoreSession#hasPermission)
// This applies only for users, not groups, unfortunately
public class JCRAceMapping implements JCRServiceConstants {

    private static final Log log = LogFactory.getLog(JCRAceMapping.class);

    protected String connectionName;

    protected String method;

    protected Map<String, String> mapping;

    protected List<JCRUserMappingDescriptor> users;

    protected CoreSession coreSession;

    protected DocumentModel doc;

    protected Node remote;

    protected Session session;

    protected AccessControlManager acm;

    protected UserManager userManager = null;

    public JCRAceMapping(String connectionName, String method, Map<String, String> mapping,
            List<JCRUserMappingDescriptor> users) {
        super();
        this.connectionName = connectionName;
        this.method = method;
        this.mapping = mapping;
        this.users = users;
    }

    protected UserManager getUserManager() {
        if (userManager == null) {
            userManager = Framework.getService(UserManager.class);
        }

        return userManager;
    }

    public DocumentModel applyMapping(CoreSession coreSession, DocumentModel doc, Session session, Node remote)
            throws RepositoryException {

        this.coreSession = coreSession;
        this.doc = doc;
        this.session = session;
        this.acm = session.getAccessControlManager();
        this.remote = remote;

        try {
            switch (method) {
            case ACE_SYNC_METHOD_REPLACE:
                return applyWithReplaceAll();

            case ACE_SYNC_METHOD_ADD_IF_NOT_SET:
            default:
                return applyWithAddIfNotSet();
            }
        } catch (AccessDeniedException ade) {
            return doc;
        }
    }

    protected String mapUser(Principal principal) {
        String pName = principal.getName();
        Optional<JCRUserMappingDescriptor> map = this.users.stream()
                                                           .filter(u -> pName.equals(u.getRemoteUser()))
                                                           .findFirst();
        if (map.isPresent()) {
            return map.get().getLocalUser();
        }
        return pName;
    }

    protected DocumentModel applyWithReplaceAll() throws RepositoryException {

        DocumentRef docRef = doc.getRef();

        // Remove all permissions, block inheritance
        ACP nxacp = doc.getACP();

        boolean permissionChanged = nxacp.blockInheritance(ACL.LOCAL_ACL, coreSession.getPrincipal().getName());
        if (permissionChanged) {
            doc.setACP(nxacp, true);
        }

        ACL[] acls = nxacp.getACLs();
        for (ACL acl : acls) {
            if (!ACL.INHERITED_ACL.equals(acl.getName())) {
                nxacp.removeACL(acl.getName());
            }
        }
        doc.setACP(nxacp, true);

        // Now, add the remote permissions
        AccessControlPolicy[] remoteACEs = acm.getEffectivePolicies(remote.getPath());
        if (remoteACEs != null) {
            ACLImpl nuxeoAcl = new ACLImpl(SYNC_ACL);
            nxacp.addACL(nuxeoAcl);

            for (AccessControlPolicy acp : remoteACEs) {
                AccessControlEntry ace = null;
                if (acp instanceof AccessControlEntry) {
                    ace = (AccessControlEntry) acp;
                } else {
                    continue;
                }
                String principalId = mapUser(ace.getPrincipal());
                Principal localPrincipal = getUserManager().getPrincipal(principalId);
                boolean isGroup = false;
                boolean isEveryone = false;

                if (localPrincipal == null) {
                    isGroup = getUserManager().getGroup(principalId) != null;
                }

                if (localPrincipal == null && !isGroup) {
                    isEveryone = SecurityConstants.EVERYONE.equals(principalId);
                }

                if (localPrincipal == null && !isGroup && !isEveryone) {
                    // TODO: throw new NuxeoException("User/Group <" + principalId + "> not found");
                    log.warn("User/Group <" + principalId + "> not found, using Administrator");
                    principalId = "Administrator";
                }

                for (Privilege remotePerm : ace.getPrivileges()) {
                    String localPerm = mapping.get(remotePerm.getName());
                    if (localPerm == null) {
                        // No mapping, use the original as permission
                        // OR throw an error?
                        localPerm = remotePerm.getName();
                    }

                    ACE nuxeoAce = new ACE(principalId, localPerm, true);
                    nuxeoAcl.add(nuxeoAce);
                }
            }
            coreSession.setACP(docRef, nxacp, false);
        }

        return doc;
    }

    protected DocumentModel applyWithAddIfNotSet() throws RepositoryException {

        DocumentRef docRef = doc.getRef();

        AccessControlPolicy[] remoteACEs = acm.getEffectivePolicies(remote.getPath());
        ACP localAcp = doc.getACP();
        if (remoteACEs != null) {
            for (AccessControlPolicy acp : remoteACEs) {
                AccessControlEntry ace = null;
                if (acp instanceof AccessControlEntry) {
                    ace = (AccessControlEntry) acp;
                } else {
                    continue;
                }
                String principalId = mapUser(ace.getPrincipal());

                NuxeoPrincipal localPrincipal = getUserManager().getPrincipal(principalId);
                boolean isGroup = false;
                boolean isEveryone = false;

                if (localPrincipal == null) {
                    isGroup = getUserManager().getGroup(principalId) != null;
                }

                if (localPrincipal == null && !isGroup) {
                    isEveryone = SecurityConstants.EVERYONE.equals(principalId);
                }

                if (localPrincipal == null && !isGroup && !isEveryone) {
                    // TODO: throw new NuxeoException("User/Group <" + principalId + "> not found");
                    log.warn("User/Group <" + principalId + "> not found, using Administrator");
                    principalId = "Administrator";
                }

                for (Privilege remotePerm : ace.getPrivileges()) {
                    String localPerm = mapping.get(remotePerm.getName());
                    if (localPerm == null) {
                        // No mapping, use the original as permission
                        // OR throw an error?
                        localPerm = remotePerm.getName();
                        log.warn("Permission mapping for user <" + principalId + "> not found: <" + remotePerm + ">");
                    }

                    // Add permission if this user does not already have it
                    boolean needAddPermission = false;
                    if (localPrincipal != null) {
                        // This CoreSession#hasPermission checks all, including SecurityPolicies
                        // But actually this may be a problem. If hasPermission() returns false
                        // because of a security policy, we are still adding it (but it will
                        // be ignored because custom SecurityPolicy are called first.
                        // Complex problem, for sure :-)
                        if (!coreSession.hasPermission(localPrincipal, docRef, localPerm)) {
                            needAddPermission = true;
                        }
                    } else {
                        Access access = localAcp.getAccess(principalId, localPerm);
                        if (access == Access.UNKNOWN) { // or access != Access.GRANT...
                            needAddPermission = true;
                        }
                    }
                    if (needAddPermission) {
                        ACPImpl acpi = new ACPImpl();
                        ACLImpl nuxeoAcl = new ACLImpl(SYNC_ACL);
                        acpi.addACL(nuxeoAcl);
                        ACE nuxeoAce = new ACE(principalId, localPerm, true);
                        nuxeoAcl.add(nuxeoAce);
                        coreSession.setACP(docRef, acpi, false);
                    }
                }
            }
        }

        return doc;
    }

    public String getMethod() {
        return method;
    }

}
