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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.sync.jcr.api.JCRServiceConstants;

@XObject("connection")
public class JCRConnectionDescriptor implements Serializable, JCRServiceConstants {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("repository")
    protected String repository = null;

    @XNode("url")
    protected String url = null;

    @XNode("@binding")
    protected String binding = null;

    @XNode("username")
    protected String username = null;

    @XNode("credentials")
    protected String credentials = null;

    @XNodeMap(value = "property", key = "@key", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties = new HashMap<>();

    @XNodeList(value = "field-mapping", componentType = JCRFieldMappingDescriptor.class, type = ArrayList.class)
    protected List<JCRFieldMappingDescriptor> fieldMapping = new ArrayList<>();

    @XNodeList(value = "user-mapping", componentType = JCRUserMappingDescriptor.class, type = ArrayList.class)
    protected List<JCRUserMappingDescriptor> userMapping = new ArrayList<>();

    @XNode("ace-mapping/method")
    protected String aceMappingMethod = ACE_SYNC_METHOD_REPLACE;

    @XNodeMap(value = "ace-mapping/remoteAce", key = "@value", type = HashMap.class, componentType = String.class)
    protected Map<String, String> aceMapping = new HashMap<>();

    @XNodeMap(value = "doctype-mapping/doctype", key = "@value", type = HashMap.class, componentType = String.class)
    protected Map<String, String> doctypeMapping = new HashMap<>();

    public JCRConnectionDescriptor() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBinding() {
        return binding;
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getAceMapping() {
        return aceMapping;
    }

    public void setAceMapping(Map<String, String> aceMapping) {
        this.aceMapping = aceMapping;
    }

    public List<JCRFieldMappingDescriptor> getFieldMapping() {
        return fieldMapping;
    }

    public String getAceMappingMethod() {
        if (StringUtils.isBlank(aceMappingMethod)) {
            aceMappingMethod = ACE_SYNC_METHOD_REPLACE;
        }

        return aceMappingMethod;
    }

    public Map<String, String> getDoctypeMapping() {
        return doctypeMapping;
    }

    public List<JCRUserMappingDescriptor> getUserMapping() {
        return userMapping;
    }

    public void setUserMapping(List<JCRUserMappingDescriptor> userMapping) {
        this.userMapping = userMapping;
    }

}