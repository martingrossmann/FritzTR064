/**
 * *********************************************************************************************************************
 * <p>
 * javaAVMTR064 - open source Java TR-064 API
 * ===========================================
 * <p>
 * Copyright 2015 Marin Pollmann <pollmann.m@gmail.com>
 * <p>
 * <p>
 * **********************************************************************************************************************
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * <p>
 * *********************************************************************************************************************
 */
package de.mapoll.javaAVMTR064;

import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Response {

    private SOAPMessage response;
    private Map<String, Type> stateToType;
    private Map<String, String> argumentState;
    private Map<String, String> data;
    private Map<String, String> headerData;

    public Response(SOAPMessage response, Map<String, Type> stateToType, Map<String, String> argumentState)
            throws SOAPException {
        this.response = response;
        this.stateToType = stateToType;
        this.argumentState = argumentState;
        this.data = new HashMap<>();
        this.headerData = new HashMap<>();

        this.fillData(this.data, response.getSOAPBody().getChildNodes());
        this.fillData(this.headerData, response.getSOAPHeader().getChildNodes());
    }

    private void fillData(Map<String, String> map, NodeList tmpList) {
        NodeList nodes = null;

        for (int i = 1; i < tmpList.getLength() && nodes == null; i++) {
            if (tmpList.item(i).getNodeName().equals("#text")) {
                continue;
            }
            nodes = tmpList.item(i).getChildNodes();
        }

        for (int i = 1; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName().equals("#text")) {
                continue;
            }
            map.put(nodes.item(i).getNodeName(), nodes.item(i).getTextContent());
        }
    }

    public SOAPMessage getSOAPMessage() {
        return response;
    }

    public Map<String, String> getData() {
        return data;
    }

    public Map<String, String> getHeaderData() {
        return headerData;
    }

    public String getValueAsString(String argument) throws NoSuchFieldException {
        if (!argumentState.containsKey(argument) || !data.containsKey(argument)) {
            throw new NoSuchFieldException(argument);
        }
        return data.get(argument);
    }

    public int getValueAsInteger(String argument) throws ClassCastException, NoSuchFieldException {
        if (!argumentState.containsKey(argument) || !data.containsKey(argument)) {
            throw new NoSuchFieldException(argument);
        }
        if (stateToType.get(argumentState.get(argument)) != Integer.class) {
            throw new ClassCastException(argument);
        }

        int ret = -1;

        try {
            ret = Integer.parseInt(data.get(argument));
        } catch (NumberFormatException e) {
            throw new ClassCastException(argument + " " + e.getMessage());
        }

        return ret;
    }

    public boolean getValueAsBoolean(String argument) throws ClassCastException, NoSuchFieldException {
        if (!argumentState.containsKey(argument) || !data.containsKey(argument)) {
            throw new NoSuchFieldException(argument);
        }
        if (stateToType.get(argumentState.get(argument)) != Boolean.class) {
            throw new ClassCastException(argument);
        }

        boolean ret = false;

        if (data.get(argument).equals("1") || data.get(argument).equalsIgnoreCase("true")) {
            ret = true;
        } else if (data.get(argument).equals("0") || data.get(argument).equalsIgnoreCase("false")) {
            ret = false;
        } else {
            throw new ClassCastException(argument);
        }

        return ret;
    }

    public Date getValueAsDate(String argument) throws ClassCastException, NoSuchFieldException {
        if (!argumentState.containsKey(argument) || !data.containsKey(argument)) {
            throw new NoSuchFieldException(argument);
        }
        if (stateToType.get(argumentState.get(argument)) != Date.class) {
            throw new ClassCastException(argument);
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        Date ret = null;

        try {
            ret = dateFormat.parse(data.get(argument));
        } catch (ParseException e) {
            throw new ClassCastException(argument + " " + e.getMessage());
        }

        return ret;
    }

    public UUID getValueAsUUID(String argument) throws ClassCastException, NoSuchFieldException {
        if (!argumentState.containsKey(argument) || !data.containsKey(argument)) {
            throw new NoSuchFieldException(argument);
        }
        if (stateToType.get(argumentState.get(argument)) != UUID.class) {
            throw new ClassCastException(argument);
        }
        UUID ret = null;

        try {
            ret = UUID.fromString(data.get(argument));
        } catch (IllegalArgumentException e) {
            throw new ClassCastException(argument + " " + e.getMessage());
        }

        return ret;
    }

}
