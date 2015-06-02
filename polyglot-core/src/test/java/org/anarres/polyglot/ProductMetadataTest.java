/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import java.io.IOException;
import org.anarres.jdiagnostics.ProductMetadata;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class ProductMetadataTest {

    private static final Logger LOG = LoggerFactory.getLogger(ProductMetadataTest.class);

    @Test
    public void testProductMetadata() throws IOException {
        ProductMetadata product = new ProductMetadata();
        LOG.info("Product is\n" + product);
    }
}
