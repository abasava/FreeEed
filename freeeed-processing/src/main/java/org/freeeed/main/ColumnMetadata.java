/*
 *
 * Copyright SHMsoft, Inc. 
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
/*    
    Copyright 2012 Mark Kerzner

*/ 
package org.freeeed.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.tika.metadata.Metadata;
import org.freeeed.services.Project;

public class ColumnMetadata {

    private ArrayList<String> headers = new ArrayList<>();
    private ArrayList<String> values = new ArrayList<>();
    public static final String metadataNamesFile = "config/standard-metadata-names.properties";
    private FreeEedConfiguration metadataNames;
    private String fieldSeparator;
    // allMetadata controls whether all or only standard mapped metadata is delivered
    private boolean allMetadata = false;
    private int standardHeaderSize = 0;

    /**
     * @return the fieldSeparator
     */
    public String getFieldSeparator() {
        return fieldSeparator;
    }

    /**
     * @param fieldSeparator the fieldSeparator to set
     */
    public void setFieldSeparator(String fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }
    /**
     * Aliases give all name by which are metadata goes
     */
    private HashMap<String, String> aliases = new HashMap<>();

    public ColumnMetadata() {
        init();
    }

    private void init() {
        try {
            metadataNames = new FreeEedConfiguration(metadataNamesFile);
        } catch (ConfigurationException e) {
            System.out.println("Error: file " + metadataNamesFile + " could not be read");
            e.printStackTrace(System.out);
        }
        Iterator numberKeys = metadataNames.getKeys();
        ArrayList<String> stringKeys = new ArrayList<>();
        while (numberKeys.hasNext()) {
            stringKeys.add((String) numberKeys.next());
        }
        Collections.sort(stringKeys);
        for (String key : stringKeys) {
            String[] aka = metadataNames.getStringArray(key);
            String realName = aka[0];
            addMetadataValue(realName, "");
            // skip the  first one, which is the alias of itself
            for (int i = 1; i < aka.length; ++i) {
                String alias = aka[i];
                aliases.put(alias, realName);
            }
        }
        standardHeaderSize = headers.size();
    }

    public void reinit() {
        for (int i = 0; i < values.size(); ++i) {
            values.set(i, "");
        }
    }

    public void addMetadataValue(String header, String value) {
        // if we have this header, put the value in the right place
        if (headers.contains(header)) {
            int index = headers.indexOf(header);
            values.set(index, value);

        } else { // if we don't have such a header, add it
            headers.add(header);
            values.add(value);
        }
        // additionally, map every alias to the real name
        if (aliases.containsKey(header)) {
            String realName = aliases.get(header);
            addMetadataValue(realName, value);
        }
    }

    public void addMetadata(Metadata metadata) {
        String[] names = metadata.names();
        for (String name : names) {
            // exclude the text from metadadata - depending on the project's settings
            boolean exclude = name.equalsIgnoreCase(DocumentMetadataKeys.DOCUMENT_TEXT)
                    && !Project.getProject().isTextInMetadata();
            if (exclude) {
                continue;
            }
            addMetadataValue(name, metadata.get(name));
        }
    }

    public String delimiterSeparatedValues() {
        StringBuilder builder = new StringBuilder();
        int headerCount = 0;
        int valuesAddedCount = 0;
        for (String value : values) {
            if (!allMetadata) {
                ++headerCount;
                if (headerCount > standardHeaderSize) {
                    continue;
                }
            }
            
            if (valuesAddedCount > 0) {
                builder.append(fieldSeparator);
            }
            
            builder.append(sanitize(value));
            
            valuesAddedCount++;
        }
        return builder.toString();
    }

    /**
     * Why would this function be needed for anything but tab delimiter?
     *
     * @return
     */
    public String delimiterSeparatedHeaders() {
        StringBuilder builder = new StringBuilder();
        int headerCount = 0;
        int valuesAddedCount = 0;
        for (String header : headers) {
            if (!allMetadata) {
                ++headerCount;
                if (headerCount > standardHeaderSize) {
                    continue;
                }
            }
            
            if (valuesAddedCount > 0) {
                builder.append(fieldSeparator);
            }
            
            builder.append(sanitize(header));
            
            valuesAddedCount++;
        }
        return builder.toString();
    }

    private String sanitize(String str) {
        // replace all non-ascii with underscore
        String ascii = str.replaceAll("[^\\p{ASCII}]", "_");
        // replace all newlines with a space (we want everything on one line)
        ascii = ascii.replace("\n", " ");
        ascii = ascii.replace("\r", " ");
        // replace all occurences of fieldSeparator with a space
        ascii = ascii.replace(fieldSeparator, " ");
        return ascii;
    }

    /**
     * @return the allMetadata
     */
    public boolean isAllMetadata() {
        return allMetadata;
    }

    /**
     * @param allMetadata the allMetadata to set
     */
    public void setAllMetadata(String allMetadataStr) {
        this.allMetadata = "ALL".equalsIgnoreCase(allMetadataStr);
    }
}
