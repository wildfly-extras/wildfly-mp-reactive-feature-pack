package org.wildfly.test.microprofile.reactive.provision.xml.verifier;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class VerifyProvisionXmlFeaturePackVersionsTestCase {

    private static final String FEATURE_PACK_NAME = "wildfly-microprofile-reactive-feature-pack";
    private static final int EXPECTED_README_COUNT = 2;
    private static final int EXPECTED_PROVISION_XML_COUNT = 5;
    private int readMeCount = 0;
    private int provisionXmlCount = 0;


    List<String> errors = new ArrayList<>();

    String projectVersion;
    Path checkoutFolder;

    @Before
    public void before() {
        projectVersion = System.getProperty("test.project.version");
        Assert.assertNotNull(projectVersion);

        checkoutFolder = Paths.get("").toAbsolutePath();

        while (checkoutFolder != null && checkoutFolder.getFileName().toString().equals("provision-xml-verifier")) {
            checkoutFolder = checkoutFolder.getParent();
        }
        Assert.assertNotNull(checkoutFolder);
    }


    @Test
    public void verifyReadMe() throws Exception {
        Path path = checkoutFolder.resolve("README.md");
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            if (line.contains(projectVersion)) {
                readMeCount++;
            }
        }
        Assert.assertEquals(EXPECTED_README_COUNT, readMeCount);
    }

    @Test
    public void verifyProvisionXmls() throws Exception {

        Files.walkFileTree(checkoutFolder, new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals("provision.xml")) {
                    checkProvisionXmlFile(file, projectVersion);
                }
                return super.visitFile(file, attrs);
            }
        });

        Assert.assertEquals(errors.toString(), 0, errors.size());
        Assert.assertEquals("Wrong provision.xml count", EXPECTED_PROVISION_XML_COUNT, provisionXmlCount);
    }

    private void checkProvisionXmlFile(Path path, String projectVersion) throws IOException {
        provisionXmlCount++;
        List<String> lines = Files.readAllLines(path);
        boolean foundFeaturePack = false;
        for (String line : lines) {
            if (line.contains(FEATURE_PACK_NAME)) {
                foundFeaturePack = true;
                if (!line.contains(projectVersion)) {
                    errors.add(path.toAbsolutePath() + " does not contain the correct version for " + FEATURE_PACK_NAME);
                }
                break;
            }
        }
        if (!foundFeaturePack) {
            errors.add(path.toAbsolutePath() + " does not contain an entry for " + FEATURE_PACK_NAME);
        }
    }
}
