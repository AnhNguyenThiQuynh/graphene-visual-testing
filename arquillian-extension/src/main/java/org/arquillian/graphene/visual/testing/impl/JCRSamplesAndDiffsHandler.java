package org.arquillian.graphene.visual.testing.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.arquillian.extension.recorder.screenshooter.ScreenshooterConfiguration;
import org.arquillian.graphene.visual.testing.api.SamplesAndDiffsHandler;
import org.arquillian.graphene.visual.testing.configuration.GrapheneVisualTestingConfiguration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.rusheye.arquillian.configuration.RusheyeConfiguration;
import org.jboss.rusheye.arquillian.event.ParsingDoneEvent;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.SAXException;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.entity.StringEntity;
import org.jboss.rusheye.suite.ResultConclusion;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author jhuska
 */
public class JCRSamplesAndDiffsHandler implements SamplesAndDiffsHandler {

    private static final Logger LOGGER = Logger.getLogger(JCRSamplesAndDiffsHandler.class.getName());

    @Inject
    private Instance<RusheyeConfiguration> rusheyeConf;

    @Inject
    private Instance<GrapheneVisualTestingConfiguration> grapheneVisualTestingConf;

    @Inject
    private Instance<ScreenshooterConfiguration> screenshooterConf;

    @Override
    public void saveSamplesAndDiffs(@Observes ParsingDoneEvent parsingDoneEvent) {
        Date timestamp = new Date();
        String timestampWithoutWhiteSpaces = timestamp.toString().replaceAll("\\s+", "_").replaceAll(":", "_");
        String suiteName = grapheneVisualTestingConf.get().getTestSuiteName();
        GrapheneVisualTestingConfiguration gVC = grapheneVisualTestingConf.get();
        CloseableHttpClient httpclient = RestUtils.getHTTPClient(gVC.getJcrContextRootURL(), gVC.getJcrUserName(), gVC.getJcrPassword());
        File resultDescriptor = new File(rusheyeConf.get().getWorkingDirectory()
                + File.separator
                + rusheyeConf.get().getResultOutputFile());

        //UPLOADING RESULT DESCRIPTOR
        HttpPost postResultDescriptor = new HttpPost(gVC.getJcrContextRootURL() + "/upload/" + suiteName + "/runs/"
                + timestampWithoutWhiteSpaces + "/result.xml");
        FileEntity descriptorEntity = new FileEntity(resultDescriptor, ContentType.APPLICATION_XML);
        postResultDescriptor.setEntity(descriptorEntity);
        RestUtils.executePost(postResultDescriptor, httpclient,
                String.format("Suite result descriptor for %s uploaded!", suiteName),
                String.format("Error while uploading test suite result descriptor for test suite: %s", suiteName));

        //CREATE TEST SUITE RUN IN DATABASE
        HttpPost postCreateSuiteRun = new HttpPost(gVC.getManagerContextRootURL() + "graphene-visual-testing-webapp/rest/runs");
        postCreateSuiteRun.setHeader("Content-Type", "application/json");
        StringEntity suiteRunEntity = new StringEntity(
                "{\"timestamp\":\"" + timestamp.getTime() + "\",\"projectRevision\":\"ffff1111\","
                + "\"numberOfFailedFunctionalTests\":\"-1\","
                + "\"numberOfFailedComparisons\":\"-1\","
                + "\"testSuite\":{\"name\":\"" + suiteName + "\"}}", ContentType.APPLICATION_JSON);
        postCreateSuiteRun.setEntity(suiteRunEntity);
        String testSuiteRunID = RestUtils.executePost(postCreateSuiteRun, httpclient,
                String.format("SuiteRun in database for %s created!", suiteName),
                String.format("Error while SuiteRun name in database for test suite: %s", suiteName));

        //UPLOADING DIFFS AND SAMPLES IF ANY
        Map<String, String> patternsNamesAndCorrespondingDiffs = getDiffNames();
        if (!patternsNamesAndCorrespondingDiffs.isEmpty()) {
            uploadSamples(patternsNamesAndCorrespondingDiffs, timestampWithoutWhiteSpaces, testSuiteRunID);
            uploadDiffs(patternsNamesAndCorrespondingDiffs, timestampWithoutWhiteSpaces);
        }
    }

    private void uploadSamples(Map<String, String> patternsNamesAndCorrespondingDiffs, String timestamp, String testSuiteRunID) {
        final File screenshotsDir = screenshooterConf.get().getRootDir();
        final String suiteName = grapheneVisualTestingConf.get().getTestSuiteName();

        NodeList testNodes = getDOMFromSuiteXML().getElementsByTagName("test");
        for (Map.Entry<String, String> entry : patternsNamesAndCorrespondingDiffs.entrySet()) {
            for (int i = 0; i < testNodes.getLength(); i++) {
                if (testNodes.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(entry.getKey())) {
                    String patternSource = testNodes.item(i).getChildNodes().item(1).getAttributes()
                            .getNamedItem("source").getNodeValue();
                    //UPLOAD SAMPLE
                    File sampleToUpload = new File(screenshotsDir + File.separator + patternSource);
                    String url = grapheneVisualTestingConf.get().getJcrContextRootURL() + "/upload/" + suiteName + "/runs/"
                            + timestamp + "/samples/" + patternSource;
                    HttpPost postResultDescriptor = new HttpPost(url);
                    FileEntity sampleEntity = new FileEntity(sampleToUpload);
                    postResultDescriptor.setEntity(sampleEntity);
                    GrapheneVisualTestingConfiguration gVC = grapheneVisualTestingConf.get();
                    RestUtils.executePost(postResultDescriptor, RestUtils.getHTTPClient(gVC.getJcrContextRootURL(),
                            gVC.getJcrUserName(), gVC.getJcrPassword()),
                            String.format("Sample for %s uploaded!", suiteName),
                            String.format("Error while uploading sample for test suite: %s", suiteName));

                    //CREATE SAMPLE IN DATABASE
                    HttpPost postCreateSample = new HttpPost(gVC.getManagerContextRootURL() + "graphene-visual-testing-webapp/rest/samples");
                    postCreateSample.setHeader("Content-Type", "application/json");
                    StringEntity suiteRunEntity = new StringEntity(
                            "{}", ContentType.APPLICATION_JSON);
                    postCreateSample.setEntity(suiteRunEntity);
                    String testSuiteRunID = RestUtils.executePost(postCreateSample, httpclient,
                            String.format("SuiteRun in database for %s created!", suiteName),
                            String.format("Error while SuiteRun name in database for test suite: %s", suiteName));
                }
            }
        }
    }

    private void uploadDiffs(Map<String, String> patternsNamesAndCorrespondingDiffs, String timestampWithoutWhiteSpaces) {
        String suiteName = grapheneVisualTestingConf.get().getTestSuiteName();
        for (final String diffName : patternsNamesAndCorrespondingDiffs.keySet()) {
            File diffsDir = new File(rusheyeConf.get().getWorkingDirectory()
                    + File.separator
                    + rusheyeConf.get().getDiffsDir());
            File[] diffsWithSearchedName = diffsDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(diffName);
                }
            });
            if (diffsWithSearchedName.length > 1) {
                throw new RuntimeException("There are two or more diffs which names contains: " + diffName);
            } else if (diffsWithSearchedName.length == 0) {
                throw new RuntimeException("Diff with filename: " + diffName + " was not found!");
            } else {
                //UPLOADING DIFF
                File diffToUpload = diffsWithSearchedName[0];
                GrapheneVisualTestingConfiguration gVC = grapheneVisualTestingConf.get();
                HttpPost postResultDescriptor = new HttpPost(gVC.getJcrContextRootURL() + "/upload/" + suiteName + "/runs/"
                        + timestampWithoutWhiteSpaces + "/diffs/" + diffToUpload.getName());
                FileEntity diffEntity = new FileEntity(diffToUpload);
                postResultDescriptor.setEntity(diffEntity);
                RestUtils.executePost(postResultDescriptor, RestUtils.getHTTPClient(gVC.getJcrContextRootURL(),
                        gVC.getJcrUserName(), gVC.getJcrPassword()),
                        String.format("Diff for %s uploaded!", suiteName),
                        String.format("Error while uploading diff for test suite: %s", suiteName));
            }
        }
    }

    private Map<String, String> getDiffNames() {
        Map<String, String> result = new HashMap<>();
        NodeList testNodes = getDOMFromResultXML().getElementsByTagName("test");
        for (int i = 0; i < testNodes.getLength(); i++) {
            Node patternTag = testNodes.item(i).getChildNodes().item(1);
            String resultAttribute = patternTag.getAttributes().
                    getNamedItem("result").getNodeValue();
            if (ResultConclusion.valueOf(resultAttribute).equals(ResultConclusion.DIFFER)) {
                String outputAttrValue = patternTag.getAttributes().getNamedItem("output").getNodeValue();
                String nameAttrValue = patternTag.getAttributes().getNamedItem("name").getNodeValue();
                result.put(nameAttrValue, outputAttrValue);
            }
        }
        return result;
    }

    private Document getDOMFromResultXML() {
        String resultFilePath = rusheyeConf.get().getWorkingDirectory()
                + File.separator + rusheyeConf.get().getResultOutputFile();
        return getDOMFromXMLFile(new File(resultFilePath));
    }

    private Document getDOMFromSuiteXML() {
        String suiteFilePath = JCRDescriptorAndPatternsHandler.PATTERNS_DEFAULT_DIR
                + File.separator + rusheyeConf.get().getSuiteDescriptor().getName();
        return getDOMFromXMLFile(new File(suiteFilePath));
    }

    private Document getDOMFromXMLFile(File xmlFile) {
        Document result = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            db = dbf.newDocumentBuilder();
            result = db.parse(xmlFile);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(JCRSamplesAndDiffsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Instance<RusheyeConfiguration> getRusheyeConf() {
        return rusheyeConf;
    }

    public void setRusheyeConf(Instance<RusheyeConfiguration> rusheyeConf) {
        this.rusheyeConf = rusheyeConf;
    }

    public Instance<GrapheneVisualTestingConfiguration> getGrapheneVisualTestingConf() {
        return grapheneVisualTestingConf;
    }

    public void setGrapheneVisualTestingConf(Instance<GrapheneVisualTestingConfiguration> grapheneVisualTestingConf) {
        this.grapheneVisualTestingConf = grapheneVisualTestingConf;
    }
}
