/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.arquillian.graphene.visual.testing.api;

import org.jboss.rusheye.arquillian.event.ParsingDoneEvent;

/**
 *
 * @author jhuska
 */
public interface SamplesAndDiffsHandler {
    
    void saveSamplesAndDiffs(ParsingDoneEvent parsingDoneEvent);
}
