
package net.gfipm.shibboleth.config;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for the GFIPM BAE data connector namespace.
 */
public class GFIPMNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:global:gfipm:1.1:bae-resolver";
    
    /** {@inheritDoc} */
    public void init() {
        // Register GFIPM Data Connector Parsers.
        registerBeanDefinitionParser(GfipmTestDataConnectorBeanDefinitionParser.TYPE_NAME,
                new GfipmTestDataConnectorBeanDefinitionParser());
        registerBeanDefinitionParser(GfipmBAEDataConnectorBeanDefinitionParser.TYPE_NAME,
                new GfipmBAEDataConnectorBeanDefinitionParser());
    }

}
