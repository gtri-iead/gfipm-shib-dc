/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.gfipm.shibboleth.config;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.BaseDataConnectorBeanDefinitionParser;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;
//import net.gfipm.shibboleth.dataconnector.GfipmBAEDataConnector;
import net.gfipm.shibboleth.dataconnector.BAEAttributeNameMap;


/**
 * Spring Bean Definition Parser for static data connector.
 */
public class GfipmBAEDataConnectorBeanDefinitionParser extends BaseDataConnectorBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(GFIPMNamespaceHandler.NAMESPACE, "BAE");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(GfipmBAEDataConnectorBeanDefinitionParser.class);

    /** Local name of attribute. */
    public static final QName ATTRIBUTE_ELEMENT_NAME = new QName(GFIPMNamespaceHandler.NAMESPACE,
            "Attribute");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return GfipmBAEDataConnectorFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {
        super.doParse(pluginId, pluginConfig, pluginConfigChildren, pluginBuilder, parserContext);

        List<BAEAttributeNameMap> attributes = parseAttributes(pluginConfigChildren.get(ATTRIBUTE_ELEMENT_NAME));
        log.debug("Setting the following attributes for plugin {}: {}", pluginId, attributes);
        pluginBuilder.addPropertyValue("baeAttributes", attributes);

        String baeURL      = pluginConfig.getAttributeNS(null, "baeURL");
        String baeEntityId = pluginConfig.getAttributeNS(null, "baeEntityId");
        String myEntityId  = pluginConfig.getAttributeNS(null, "myEntityId");
        String subjectId   = pluginConfig.getAttributeNS(null, "subjectId");

        pluginBuilder.addPropertyValue("baeURL", baeURL);
        pluginBuilder.addPropertyValue("baeEntityId", baeEntityId);
        pluginBuilder.addPropertyValue("myEntityId",  myEntityId);
        pluginBuilder.addPropertyValue("subjectId",   subjectId);

        int searchTimeLimit = 5000;
        if (pluginConfig.hasAttributeNS(null, "searchTimeLimit")) {
            searchTimeLimit = Integer.parseInt(pluginConfig.getAttributeNS(null, "searchTimeLimit"));
        }
        log.debug("Data connector {} search timeout: {}ms", pluginId, searchTimeLimit);
        pluginBuilder.addPropertyValue("searchTimeLimit", searchTimeLimit);

        RuntimeBeanReference trustCredential = processCredential(pluginConfigChildren.get(new QName(
                GFIPMNamespaceHandler.NAMESPACE, "TrustCredential")), parserContext);
        log.debug("Data connector {} using provided trust material", pluginId);
        pluginBuilder.addPropertyValue("trustCredential", trustCredential);

        RuntimeBeanReference myCredential = processCredential(pluginConfigChildren.get(new QName(
                GFIPMNamespaceHandler.NAMESPACE, "AuthenticationCredential")), parserContext);
        log.debug("Data connector {} using provided client authentication material", pluginId);
        pluginBuilder.addPropertyValue("myCredential", myCredential);
    }


    /**
     * Parse attribute requirements
     *
     * @param elements DOM elements of type <code>Attribute</code>
     *
     * @return the attributes
     */
    protected List<BAEAttributeNameMap> parseAttributes(List<Element> elements) {
        if (elements == null || elements.size() == 0) {
            return null;
        }
        List<BAEAttributeNameMap> mapAttributes = new Vector<BAEAttributeNameMap>();
        for (Element ele : elements) {
            BAEAttributeNameMap mapAttribute = new BAEAttributeNameMap(); 
            mapAttribute.QueryName  = DatatypeHelper.safeTrimOrNullString(ele.getAttributeNS(null, "QueryName"));
            mapAttribute.ReturnName = DatatypeHelper.safeTrimOrNullString(ele.getAttributeNS(null, "ReturnName"));
            log.debug("BAE Attribute " + mapAttribute.QueryName + " will be returned as local attribute " + mapAttribute.ReturnName);
            mapAttributes.add(mapAttribute);
        }
        return mapAttributes;
    }

    /**
     * Processes a credential element.
     * 
     * @param credentials list containing the element to process.
     * @param parserContext current parser context
     * 
     * @return the bean definition for the credential
     */
    protected RuntimeBeanReference processCredential(List<Element> credentials, ParserContext parserContext) {
        if (credentials == null) {
            return null;
        }
        log.debug("Data connector processng a credential");
        Element credentialElem = credentials.get(0);
        return SpringConfigurationUtils.parseCustomElement(credentialElem, parserContext);
    }


}
