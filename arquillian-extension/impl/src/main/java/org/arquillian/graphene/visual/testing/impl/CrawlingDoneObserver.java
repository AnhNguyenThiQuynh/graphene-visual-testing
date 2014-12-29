package org.arquillian.graphene.visual.testing.impl;

import org.arquillian.graphene.visual.testing.api.DescriptorAndPatternsHandler;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.rusheye.arquillian.event.CrawlingDoneEvent;

/**
 *
 * @author jhuska
 */
public class CrawlingDoneObserver {

    @Inject
    private Instance<ServiceLoader> serviceLoader;
    
    public void saveDescriptorAndPatterns(@Observes CrawlingDoneEvent event) {
        DescriptorAndPatternsHandler handler = serviceLoader.get().onlyOne(DescriptorAndPatternsHandler.class);
        boolean success = handler.saveDescriptorAndPatterns();
        if(success) {
            System.out.println("Descriptor and Patterns saved!");
        }
    }
}
