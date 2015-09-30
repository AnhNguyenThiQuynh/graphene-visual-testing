/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.arquillian.graphene.visual.testing.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.graphene.visual.testing.api.MaskFromREST;
import org.arquillian.graphene.visual.testing.api.MaskHandler;
import org.arquillian.graphene.visual.testing.api.event.CrawlMaskToSuiteEvent;
import org.arquillian.graphene.visual.testing.api.event.CrawlMaskToJCREvent;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jboss.rusheye.RushEye;
import javax.inject.Inject;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import org.arquillian.graphene.visual.testing.api.event.DeleteMaskFromSuiteEvent;

/**
 *
 * @author spriadka
 */
public class JCRMaskHandler implements MaskHandler{
   
    @Inject
    private Event<CrawlMaskToJCREvent> crawlingMasksDoneEvent;
    
    
    @Override
    public void deleteMasks(@Observes DeleteMaskFromSuiteEvent deleteMaskEvent) {
        File descriptor = deleteMaskEvent.getSuiteDescriptor();
        Long maskID = deleteMaskEvent.getMaskID();
        try{
            Document suiteXml = new SAXReader().read(descriptor);
            deleteMaskFromConfiguration(maskID, suiteXml);
            deleteMaskFromTest(maskID, suiteXml);
            writeDocumentToFile(null, suiteXml);
        }
        catch (Exception e){
            
        }
    }

    @Override
    public void uploadMasks(@Observes CrawlMaskToSuiteEvent crawlMaskEvent) {
        System.out.println("OBSERVED SUCCESSFULLY");
        File descriptor = crawlMaskEvent.getSuiteDescriptor();
        try {
            Document suiteXml = new SAXReader().read(descriptor);
            addMasksToConfiguration(crawlMaskEvent.getMasks(), suiteXml);
            addMasksToTest(crawlMaskEvent.getMasks(), suiteXml);
            String suiteName = crawlMaskEvent.getMasks().get(0).getName().split(":")[0];
            writeDocumentToFile(suiteName,suiteXml);
            
        } catch (DocumentException ex) {
            Logger.getLogger(JCRMaskHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void addMasksToConfiguration(List<MaskFromREST> masks, Document doc){
        Namespace ns = Namespace.get(RushEye.NAMESPACE_VISUAL_SUITE);
        String xPath = "/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"visual-suite\"]/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"global-configuration\"]";
        Element configurationNode = (Element) doc.selectSingleNode(xPath);
        for (MaskFromREST mask : masks){
            Element maskElement = configurationNode.addElement(QName.get("mask",ns));
            maskElement.addAttribute("id", mask.getId().toString());
            maskElement.addAttribute("source", mask.getSourceUrl());
            maskElement.addAttribute("type", mask.getMaskType().value());
        }
    }
    
    private void deleteMaskFromConfiguration(Long maskID, Document doc){
        Namespace ns = Namespace.get(RushEye.NAMESPACE_VISUAL_SUITE);
        String xPath = "/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"visual-suite\"]/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"global-configuration\"/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"mask\" and @id=\"" + maskID + "]]";
        Element maskElement = (Element)doc.selectSingleNode(xPath);
        doc.remove(maskElement);
    }
    
    private void deleteMaskFromTest(Long maskID, Document doc){
        Namespace ns = Namespace.get(RushEye.NAMESPACE_VISUAL_SUITE);
        String xPath = "/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"visual-suite\"]/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"test\"/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"mask\" and @id=\"" + maskID + "]]";
        Element maskElement = (Element)doc.selectSingleNode(xPath);
        doc.remove(maskElement);
    }
    
    private void addMasksToTest(List<MaskFromREST> masks, Document doc){
        Namespace ns = Namespace.get(RushEye.NAMESPACE_VISUAL_SUITE);
        for (MaskFromREST mask : masks){
            String testName = mask.getName().split(":")[1].replaceAll("/", ".");
            testName = testName.substring(0, testName.lastIndexOf(".png"));
            String xPath = "/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"visual-suite\"]/*[namespace-uri()=\"" + ns.getURI() + "\" and name()=\"test\" and @name=\"" + testName + "\"]";
            Element testElement = (Element)doc.selectSingleNode(xPath);
            Element maskElement = testElement.addElement(QName.get("mask",ns ));
            maskElement.addAttribute("id", mask.getId().toString());
            maskElement.addAttribute("source", mask.getSourceUrl());
            maskElement.addAttribute("type", mask.getMaskType().value());
        }
    }
    
    private void writeDocumentToFile(String suiteName, Document doc){
        File toWrite = new File("suite.xml");
        try {
            XMLWriter writer = new XMLWriter(new FileOutputStream(toWrite),OutputFormat.createPrettyPrint());
            System.out.println(doc.getText());
            writer.write(doc);
            crawlingMasksDoneEvent.fire(new CrawlMaskToJCREvent(toWrite,suiteName));
        } catch (IOException ex) {
            Logger.getLogger(JCRMaskHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    
}