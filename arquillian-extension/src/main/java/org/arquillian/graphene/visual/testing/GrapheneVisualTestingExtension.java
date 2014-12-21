package org.arquillian.graphene.visual.testing;

import org.arquillian.graphene.visual.testing.api.DescriptorAndPatternsHandler;
import org.arquillian.graphene.visual.testing.configuration.GrapheneVisualTestingConfigurator;
import org.arquillian.graphene.visual.testing.impl.AfterSuiteListener;
import org.arquillian.graphene.visual.testing.impl.CrawlingDoneObserver;
import org.arquillian.graphene.visual.testing.impl.JCRDescriptorAndPatternsHandler;
import org.arquillian.graphene.visual.testing.impl.JCRSamplesAndDiffsHandler;
import org.arquillian.graphene.visual.testing.impl.AfterListener;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class GrapheneVisualTestingExtension implements LoadableExtension {

    public void register(ExtensionBuilder builder) {
        builder.observer(AfterSuiteListener.class);
        builder.observer(GrapheneVisualTestingConfigurator.class);
        builder.observer(CrawlingDoneObserver.class);
        builder.observer(JCRSamplesAndDiffsHandler.class);
        builder.observer(AfterListener.class);
        builder.service(DescriptorAndPatternsHandler.class, JCRDescriptorAndPatternsHandler.class);
        
    }
}
