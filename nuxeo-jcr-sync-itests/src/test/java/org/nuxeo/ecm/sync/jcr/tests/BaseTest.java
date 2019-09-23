package org.nuxeo.ecm.sync.jcr.tests;

public abstract class BaseTest {

    // ------------------------This is config, should be in another class, but, well.
    public static final String CONNECTION_NUXEO_ADD_PERMS = "remoteNuxeo";

    public static final String CONNECTION_NUXEO_REPLACE_PERMS = "remoteNuxeoReplacePermissions";
    // ------------------------

    public static final String BASE_URL = "http://localhost:8080/nuxeo";

    public static final String LOGIN = "Administrator";

    public static final String PASSWORD = "Administrator";

    static final String REST_API_URL = "http://localhost:8080/nuxeo";

    public static final String GROUP_MEMBERS = "members";

    public static final String TEST_FILE_PATH = "/folder_2/file1";

    public static final String TEST_FILE_TITLE = "Test File 1";

    public static final String TEST_FILE_BLOB_NAME = "file1";

    public static final int TEST_FILE_BLOB_SIZE = 225;

    public static final String DEFAULT_PASSWORD = "123";

    public static final String USER1 = "john";

    public static final String GROUP1 = "Finance";

    public static final String CUSTOM_PERM_NOT_MAPPED = "CanDoThis";

    public static final int COUNT_TEST_FOLDERS = 2;

    public BaseTest() {
        super();
    }
}
