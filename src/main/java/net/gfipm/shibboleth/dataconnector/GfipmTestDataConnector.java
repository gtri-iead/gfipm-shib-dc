/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.gfipm.shibboleth.dataconnector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;


/**
 * Data connector implementation that returns staticly defined attributes.
 */
public class GfipmTestDataConnector extends AbstractDataConnector {

    /** Log4j logger. */
    @NonnullAfterInit private final Logger log =  LoggerFactory.getLogger(GfipmTestDataConnector.class);

    /** Source Data. */
//    private Map<String, BaseAttribute> attributes;

    /** Path to User Attribute Files */
    @NonnullAfterInit private String pathToUserAttributeFiles;

    /** Shibboleth Attribute Definition Id to get the User Id from as opposed to just using the principal id */
    @NonnullAfterInit private String uidAttributeId;

    /**
     * Constructor.
     * 
     * @param fileAttributes attributes that configure this data connector
     */
    public GfipmTestDataConnector() {
       // String pathUserMetadata, String uidAttrName) {
       // pathToUserAttributeFiles = pathUserMetadata;
       // uidAttributeId           = uidAttrName;
    }

    /**
      * Set the attribute to use as the key when identifying an attribute file. 
      * 
      * @param pathToAttributeFiles what to set.
      */
    public void setUidAttribute(@Nullable String uidAttribute) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        uidAttributeId = StringSupport.trimOrNull(uidAttribute);
    }

    /**
      * Get the uid attribute.
      * 
      * @return the uid attribute name. 
      */
    @NonnullAfterInit public String getUidAttribute() {
        return uidAttributeId;
    }


    /**
      * Set the path of the direcotry that contains an attribute file. 
      * 
      * @param pathToAttributeFiles what to set.
      */
    public void setPathToAttributeFiles(@Nullable String pathToAttributeFiles) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        pathToUserAttributeFiles = StringSupport.trimOrNull(pathToAttributeFiles);
    }

    /**
      * Get the path of the directory that contains an attribute file. 
      * 
      * @return the Path to the Attributes File. 
      */
    @NonnullAfterInit public String getPathToAttributeFiles() {
        return pathToUserAttributeFiles;
    }

    private String getPrincipal (
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) {

        final Map<String,List<IdPAttributeValue<?>>> dependencyAttributes =
                PluginDependencySupport.getAllAttributeValues(workContext, getDependencies());


        for (final Entry<String,List<IdPAttributeValue<?>>> dependencyAttribute : dependencyAttributes.entrySet()) {
            log.debug("Adding dependent attribute '{}' with the following values to the connector context: {}",
                      dependencyAttribute.getKey(), dependencyAttribute.getValue());
              if ( dependencyAttribute.getKey() == uidAttributeId ) {
                 String principalObjString = dependencyAttribute.getValue().toString();
                 int start = principalObjString.indexOf ('=');
                 int end   = principalObjString.indexOf ('}');
                 return principalObjString.substring (start + 1, end);
              }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Map<String, IdPAttribute> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {

        Constraint.isNotNull(resolutionContext, "AttributeResolutionContext cannot be null");
        Constraint.isNotNull(workContext, "AttributeResolverWorkContext cannot be null");

        String strPrincipal = getPrincipal (resolutionContext, workContext);

        //We want to cleanup principals that are DNs
        if ( null == strPrincipal )
        {
           log.error ("Failed to identify the principal");
           throw new ResolutionException("Unique principal not identified.");
        } else if ( strPrincipal.startsWith ("/") ) {
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

        Map<String, IdPAttribute> outputAttr = new HashMap<String, IdPAttribute>(20);

        try {
           File inputFile = new File(strFileName);
           BufferedReader in = new BufferedReader(new FileReader(inputFile));
           String strNextLine = "";

           while ( (strNextLine = in.readLine()) != null ) {
             String[] tokens = strNextLine.split(" :: ");

             if (tokens.length != 2) {
               log.debug ("Delimiter error when parsing attribute line: " + strNextLine);
             }
             else {
               String strAttrName  = tokens[0];
               String strAttrValue = tokens[1];
               log.debug ("Attr  = " + strAttrName + "\nValue = " + strAttrValue + "\n");

               List<IdPAttributeValue<String>> outputValue = Lists.newArrayListWithExpectedSize(1);
               outputValue.add(new StringAttributeValue(strAttrValue));

               final IdPAttribute tempAttr = new IdPAttribute(strAttrName);
               tempAttr.setValues(outputValue);

               outputAttr.put (tempAttr.getId(), tempAttr);
             }
           }
        }
        catch (java.io.FileNotFoundException name) {
           throw new ResolutionException("File Not Found: " + strFileName);
        } catch (java.io.IOException e) {
           throw new ResolutionException("File Parsing Error, while reading " + strFileName);
        }

        return outputAttr;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == uidAttributeId) {
            throw new ComponentInitializationException(getLogPrefix() + " No uid attribute source set up.");
        }
        if (null == pathToUserAttributeFiles) {
            throw new ComponentInitializationException(getLogPrefix() + " No path to attribute files set up.");
        }
    }

}

