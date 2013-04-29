/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.gfipm.shibboleth.dataconnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.BaseDataConnector;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

/**
 * Data connector implementation that returns staticly defined attributes.
 */
public class GfipmTestDataConnector extends BaseDataConnector {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(GfipmTestDataConnector.class.getName());

    /** Source Data. */
//    private Map<String, BaseAttribute> attributes;

    /** Path to User Attribute Files */
    private String pathToUserAttributeFiles;

    /** Shibboleth Attribute Definition Id to get the User Id from as opposed to just using the principal id */
    private String uidAttributeId;

    /**
     * Constructor.
     * 
     * @param fileAttributes attributes that configure this data connector
     */
    public GfipmTestDataConnector(String pathUserMetadata, String uidAttrName) {
        pathToUserAttributeFiles = pathUserMetadata;
        uidAttributeId           = uidAttrName;
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        log.debug("Resolving connector: (" + getId() + ") for principal: ("
                + resolutionContext.getAttributeRequestContext().getPrincipalName() + ")");

        Map<String, BaseAttribute> attribute = new HashMap<String, BaseAttribute>();
        //String strPrincipal    = resolutionContext.getAttributeRequestContext().getPrincipalName();

        Collection<Object> sourceIdValues = getValuesFromAllDependencies(resolutionContext, uidAttributeId);
        if (sourceIdValues == null || sourceIdValues.isEmpty()) {
            log.debug("Source attribute " + uidAttributeId + " for connector " + getId() +" provide no values");
            return Collections.EMPTY_MAP;
        }

        if (sourceIdValues.size() > 1) {
            log.warn("Source attribute " + uidAttributeId + " for connector " + getId() +" has more than one value.");
        }
        String strPrincipal = sourceIdValues.iterator().next().toString();

        //We want to cleanup principals that are DNs
        if ( strPrincipal.startsWith ("/") )
        {
           int start = strPrincipal.indexOf ('=');
           int end   = strPrincipal.indexOf ('/', start);
           strPrincipal = strPrincipal.substring (start + 1, end); // ;/CN=LinuxrefUser1/ST=GA/C=US/O=Georgia Tech.xml
        }
       
        // Now as a safety precaution in case the above fails to make a username safe
        strPrincipal.replace ('/', '-');
        strPrincipal.replace ('\\', '-');
        strPrincipal.replace ('=', '_');

        String strFileName = pathToUserAttributeFiles + strPrincipal + ".attr";

        log.debug ("Trying to load attribute file: " + strFileName + "\n");

	try
	{
	   File inputFile = new File(strFileName);
	   BufferedReader in = new BufferedReader(new FileReader(inputFile));
	   String strNextLine = "";

       while ( (strNextLine = in.readLine()) != null )
	   {
           String[] tokens = strNextLine.split(" :: ");
           
           if (tokens.length != 2)
           {
               log.debug ("Delimiter error when parsing attribute line: " + strNextLine);
           }
           else
           {
               String strAttrName  = tokens[0];
               String strAttrValue = tokens[1];
               log.debug ("Attr  = " + strAttrName + "\nValue = " + strAttrValue + "\n");
//               System.out.println ("Attr  = " + strAttrName + "\nValue = " + strAttrValue + "\n");
               BasicAttribute<String> baXmlAttr = new BasicAttribute<String>();
               baXmlAttr.getValues().add(strAttrValue);
               
               attribute.put (strAttrName, baXmlAttr);
           }
	   }
	}
	catch (java.io.FileNotFoundException name)
	{
	   throw new AttributeResolutionException("File Not Found: " + strFileName);
	}
	catch (java.io.IOException e) 
        {
           throw new AttributeResolutionException("File Parsing Error, while reading " + strFileName);
	}

//        log.debug ("GFIPMAssertion-1.0 (before encoding) = " + strXmlAttribute);

//        BasicAttribute<byte[]> baXmlAttr = new BasicAttribute<byte[]>();

//        baXmlAttr.getValues().add(strXmlAttribute.getBytes());

//	Map<String, BaseAttribute> attribute = new HashMap<String, BaseAttribute>();

//        attribute.put ("GFIPMAssertion-1.0", baXmlAttr);        

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // Do nothing
    }
}
