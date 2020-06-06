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
import org.junit.Test;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class VerifyProvisionXmlFeaturePackVersionsTestCase {

    private static final String FEATURE_PACK_NAME = "wildfly-microprofile-reactive-feature-pack";
    private static final int EXPECTED_PROVISION_XML_COUNT = 3;
    private int provisionXmlCount = 0;


    List<String> errors = new ArrayList<>();

    @Test
    public void verifyProvisionXmls() throws Exception {

        String projectVersion = System.getProperty("test.project.version");
        Assert.assertNotNull(projectVersion);

        Path checkoutFolder = Paths.get("").toAbsolutePath();

        while (checkoutFolder != null && checkoutFolder.getFileName().toString().equals("provision-xml-verifier")) {
            checkoutFolder = checkoutFolder.getParent();
        }
        Assert.assertNotNull(checkoutFolder);

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
