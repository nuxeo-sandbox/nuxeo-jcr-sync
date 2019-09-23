# Nuxeo JCR Synchronization

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-jcr-sync-master)](https://qa.nuxeo.org/jenkins/view/Sandbox/job/Sandbox/job/sandbox_nuxeo-jcr-sync-master/)

Transparent JCR synchronization by path. Current Nuxeo server is the _local_ server and it can fetch and synchronize with a remote repository, via JCR.

## Overview

The plugin provides a **configurable service** (with mappings for fields, doc types, and permissions) and some **operations**, and the typical way it works is the following:

1. Synchronize a remote folder with a local one (using the path to this remote folder). This imports the children, not yet fully synchronized
2. The plugin installs listeners: When a document is created in the context of a JCR Import, the listeners then automatically synchronize the local document with the remote one, applying:
  * A field mapping
  * A permission mapping
3. At any time later a document can be synchronized with its remote "sibling", fetching changes applied to the remote
  * **IMPORTANT**: The local document fetches changes in the remote, it does not push any metadata, it is not a bidirectional synchronization.

The plugin contributes WebUI action buttons allowing a user to perform the initial importation, and — when needed — the individual updates.

### Technical Overview

When importing a folder, the plugin adds a [`JCRSync`](nuxeo-jcr-sync-core/src/main/resources/OSGI-INF/CoreExtensions.xml) facet to the parent and to every imported child. This facet comes with a [`jcrsync`](nuxeo-jcr-sync-core/src/main/resources/schema/jcrsync.xsd) schema that gives information about the remote document linked to the local one (remote UID, URI, ...).

## Configuration / XML Contribution

### Overview

The `connection` point of the `org.nuxeo.ecm.sync.jcr.service.JCRRemoteServiceComponent` component allows for configuring:

* Information about the remote server (URL, login, ...)
  * Any JCR [Session Parameter](https://chemistry.apache.org/java/javadoc/org/apache/chemistry/openjcr/commons/SessionParameter.html) may be set via the `property` element
* Mapping for document types:
  * Map the remote doc type to a local one
  * When no mapping is provided, the plugin uses `File` or `Folder`
* Mapping for fields:
  * Which local field (`xpath`) to use to get the value stored in the remote one (`property`)
  * Can be made either specific to a document type, or available for every imported document
  * Each field mapping has a unique `name` property
    * The plugin provides [default mapping](nuxeo-jcr-sync-core/src/main/java/org/nuxeo/ecm/sync/jcr/service/impl/DefaultFieldMappings.java) (hard-coded) that can be overridden if needed
* Mapping for permissions:
  * Set the synchronization method:
    * `addIfNotSet` will add the permission, without changing the existing one.
    * `replaceAll` will replace all permissions with the new one(s)
  * And the mapping: Name of the remote permission -> value in Nuxeo
  * **WARNING**: In current implementation, if a user referenced in the remote document does not exist in current repository, the synchronization will fail.

**it is possible to define as many as remote repository as needed, each of them with their own mappings**. The `name` property of the extension point is used as unique identifier for each connection.

### Synchronization

1. When a `Folderish` document is created with a JCR synchronization state of `sync`:
* The built-in listeners will perform an `JCRImport` of the immediate folder's children with a state of `queued` for each imported document.

2. When any JCR-enabled document is created with a JCR synchronization state of `queued`:
* The built-in listeners will perform a `JCRSync` of the document with a state of `sync`.
* If the created document is `Folderish`, the recursion will continue (see #1).

### Configuration Example

```
<extension target="org.nuxeo.ecm.sync.jcr.service.JCRRemoteServiceComponent"
           point="connection">
  <connection name="remoteNuxeo" enabled="true" binding="browser">
    <repository>default</repository>
    <url>http://localhost:8080/nuxeo/json/jcr</url>
    <username>Administrator</username>
    <credentials>Administrator</credentials>

    <!-- Example of a list of Document Types mapping -->
    <doctype-mapping>
      <!--  Example when mapping between 2 Nuxeo repositories -->
      <doctype value="File">File</doctype>
      <doctype value="Note">Note</doctype>
      <doctype value="Picture">Picture</doctype>
      <doctype value="CustomContract">Contract</doctype>
      <doctype value="Claim">Claim</doctype>
      
      <!--  Example with custom doc types remote/local -->
      <doctype value="basecontract">Contract</doctype>
      <doctype value="pdfdoc">File</doctype>
      <doctype value="claim_image">Picture</doctype>
    </doctype-mapping>

    <!-- Example of a list of field mapping -->
    <field-mapping name="Copy Description for Everything" xpath="dc:description" property="dc:description" />
    <field-mapping name="Copy coverage for files" xpath="dc:coverage" property="dc:coverage"
      doctype="File" />
    <field-mapping name="Update value for picture" xpath="c:c" property="prop_c" doctype="Picture" />

    <!-- Example of user mapping -->
    <user-mapping local="members" remote="GROUP_EVERYONE" type="group" />

    <!-- Example of a list of ACE mapping -->
    <ace-mapping>
      <method>addIfNotSet</method>
      <remoteAce value="jcr:read">Read</remoteAce>
      <remoteAce value="jcr:write">ReadWrite</remoteAce>
      <remoteAce value="jcr:all">Everything</remoteAce>
      <remoteAce value="Everything">Everything</remoteAce>
    </ace-mapping>

  </connection>

  <!-- Here, another connection -->
  <connection name="remoteRepository" enabled="true" binding="browser">
    <property key="org.apache.chemistry.openjcr.binding.auth.http.oauth.bearer">abc1234</property>
    . . . etc . . .
  </connection>
  
</extension>
```

> Any [Apache Chemistry](https://chemistry.apache.org/) [session parameter](https://chemistry.apache.org/java/javadoc/org/apache/chemistry/openjcr/commons/SessionParameter.html) value can be set via the `property` element.

## Operations

* `Repository.JCRConnections`: Return the list of connections set up in the XML configuration
* `Repository.JCRImport`: Import folder-based items from remote repositories
* `Document.JCRSync`: Synchronize individual pieces of content


## Build and Install (and Test)

Build with maven (at least 3.3)

```
mvn clean install
```

> Package built here: `nuxeo-jcr-sync-package/target`

> Install with `nuxeoctl mp-install <package>`


### Testing/Unit-Testing

For unit test with maven, the plugin deploys a Nuxeo distribution, starts it on port 8080, performs the tests, then stop the distribution. This means **the unit tests will fail if you already have a Nuxeo (or any application for that mater) running on localhost:8080**.

To debug the unit tests from an IDE (Eclipse, ...); just start a nuxeo and make sur the test contribution can reach it (URL, login, password)

### Known Limitations

* No fine-tuning of the mappings, no callbacks to adapt it dynamically (maybe based on the value of fields for example)
* Not using the recent (9.10) Streams, so it will probably not ingest thousands of documents per second (anyway, the rate of ingestion also depends on the speed at which the remote application can find and send the documents)

## Support

**These features are sand-boxed and not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).

