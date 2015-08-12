
package net.gfipm.shibboleth.config;

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;


/**
 * Spring namespace handler for the GFIPM BAE data connector namespace.
 */
public class GFIPMNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:global:gfipm:1.3:bae-resolver";
    
    /** {@inheritDoc} */
    public void init() {
        // Register GFIPM Data Connector Parsers.
        registerBeanDefinitionParser(GfipmTestDataConnectorParser.TYPE_NAME, new GfipmTestDataConnectorParser());
        registerBeanDefinitionParser(GfipmBAEDataConnectorParser.TYPE_NAME,  new GfipmBAEDataConnectorParser());
    }

}
