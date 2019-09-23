package org.nuxeo.ecm.sync.jcr.service.impl;

import java.util.LinkedList;
import java.util.List;

public class DefaultFieldMappings {

    static final List<JCRFieldMappingDescriptor> MAPPINGS = new LinkedList<JCRFieldMappingDescriptor>();

    static {
        MAPPINGS.add(new JCRFieldMappingDescriptor("Document Name", "dc:title", "jcr:name"));
        MAPPINGS.add(new JCRFieldMappingDescriptor("Document Description", "dc:description", "jcr:description"));
        MAPPINGS.add(new JCRFieldMappingDescriptor("Creation Date", "dc:created", "jcr:creationDate"));
    }

}
