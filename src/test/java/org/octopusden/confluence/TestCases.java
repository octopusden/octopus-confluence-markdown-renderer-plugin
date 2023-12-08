package org.octopusden.confluence;

import org.junit.Test;

public class TestCases extends BitbucketFileDownloaderTest {
    @Test
    public void testMaster() throws Exception {
        testPlugin(
                "project1",
                "repo1",
                "path/README.md",
                "master",
                "/rest/api/1.0/projects/project1/repos/repo1",
                "/path/README.md",
                "master"
        );
    }

    @Test
    public void testBranch() throws Exception {
        testPlugin(
                "project2",
                "repo2",
                "dir1/dir2/dir3/README.md",
                "project-connect-test",
                "/rest/api/1.0/projects/project2/repos/repo2",
                "/dir1/dir2/dir3/README.md",
                "project-connect-test"
        );
    }
}
