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
package org.nuxeo.ecm.sync.jcr.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.sync.jcr.api.JCRRemoteService;
import org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants;
import org.nuxeo.ecm.sync.jcr.service.impl.JCRAceMapping;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test service and EPs.
 *
 * @author dbrown@nuxeo.com
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.sync.jcr", "org.nuxeo.ecm.sync.jcr:OSGI-INF/jcr-repository-test-contribs.xml" })
public class ITServiceConfigurationTest extends BaseTest {

    @Inject
    protected JCRRemoteService jcr;

    @Test
    public void testRepositoriesConfig() throws Exception {

        // Connections
        Collection<String> connectionNames = jcr.getConnectionNames();

        assertTrue(connectionNames.contains(BaseTest.CONNECTION_NUXEO_ADD_PERMS));
        assertTrue(connectionNames.contains(BaseTest.CONNECTION_NUXEO_REPLACE_PERMS));

        // ACE mapping
        JCRAceMapping aceMapping = jcr.getAceMappings(BaseTest.CONNECTION_NUXEO_ADD_PERMS);
        assertNotNull(aceMapping);
        assertEquals(JCRServiceConstants.ACE_SYNC_METHOD_ADD_IF_NOT_SET, aceMapping.getMethod());

        aceMapping = jcr.getAceMappings(BaseTest.CONNECTION_NUXEO_REPLACE_PERMS);
        assertNotNull(aceMapping);
        assertEquals(JCRServiceConstants.ACE_SYNC_METHOD_REPLACE, aceMapping.getMethod());

        // Field mapping
        // We are testing the configuration. See jcr-repository-test-contribs.xml
        // 3 mappings in total. 1 for all, 1 for File, 1 for Picture
        // Plus the default mapping that we don't override in tis test
        assertEquals(4, jcr.getFieldMapping(BaseTest.CONNECTION_NUXEO_ADD_PERMS, null).size());
        assertEquals(4, jcr.getFieldMapping(BaseTest.CONNECTION_NUXEO_ADD_PERMS, "Document").size());
        assertEquals(4, jcr.getFieldMapping(BaseTest.CONNECTION_NUXEO_ADD_PERMS, "Folder").size());
        assertEquals(4, jcr.getFieldMapping(BaseTest.CONNECTION_NUXEO_ADD_PERMS, "File").size());
        assertEquals(4, jcr.getFieldMapping(BaseTest.CONNECTION_NUXEO_ADD_PERMS, "Picture").size());
    }

}
