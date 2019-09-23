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
 *     Damon Brown
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.sync.jcr.api;

/**
 *
 * @since 10.2
 */
public interface JCRServiceConstants {

    String XPATH_REMOTE_UID = "jcrsync:uid";

    String XPATH_CONNECTION = "jcrsync:connection";

    String XPATH_REPOSITORY = "jcrsync:repository";

    String XPATH_TYPE = "jcrsync:type";

    String XPATH_STATE = "jcrsync:state";

    String XPATH_SYNCHRONIZED = "jcrsync:synchronized";

    String XPATH_MODIFIED = "jcrsync:modified";

    String XPATH_URI = "jcrsync:uri";

    String SYNC_ACL = "JcrSync";

    String SYNC_FACET = "JCRSync";

    String ACE_SYNC_METHOD_REPLACE = "replaceAll";

    String ACE_SYNC_METHOD_ADD_IF_NOT_SET = "addIfNotSet";

}
