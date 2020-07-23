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

import de.mapoll.javaAVMTR064.beans.DeviceType;
import de.mapoll.javaAVMTR064.beans.RootType;
import de.mapoll.javaAVMTR064.beans.ServiceType;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FritzConnection {

    private static int DEFAULT_PORT = 49000;
    private static String FRITZ_IGD_DESC_FILE = "igddesc.xml";
    private static String FRITZ_TR64_DESC_FILE = "tr64desc.xml";

    private Map<String, Service> services;
    private String user = null;
    private String pwd = null;
    private HttpHost targetHost;
    private CloseableHttpClient httpClient;
    private HttpClientContext context;


    private String nonce;
    private String auth;
    private String realm;

    private String name;

    FritzConnection(String address, int port) {

        targetHost = new HttpHost(address, port);
        httpClient = HttpClients.createDefault();
        context = HttpClientContext.create();
        services = new HashMap<String, Service>();
    }

    public FritzConnection(String address) {
        this(address, DEFAULT_PORT);
    }

    public static void main(String[] args) throws IOException, JAXBException {
        FritzConnection fc = new FritzConnection("192.168.178.1", "fritz-user", "LJy9T9HrhELnjzeA");

        fc.init();
//        Map<String, Service> services = fc.getServices();
        Action getTotalBytesReceived = fc.getService("WANCommonInterfaceConfig:1").getAction("GetTotalBytesReceived");
        Response execute = getTotalBytesReceived.execute();


    }

    public FritzConnection(String address, int port, String user, String pwd) {
        this(address, port);
        this.user = user;
        this.pwd = pwd;
    }

    public FritzConnection(String address, String user, String pwd) {
        this(address);
        this.user = user;
        this.pwd = pwd;
    }

    public void init() throws IOException, JAXBException {
        if (user != null && pwd != null) {
            readTR64();
        } else {
            throw new RuntimeException("Only user and password are supported");
//            readIGDDESC();
        }

    }

    private void readTR64() throws ClientProtocolException, IOException, JAXBException {
        InputStream xml = getXMLIS("/" + FRITZ_TR64_DESC_FILE);
        JAXBContext jaxbContext = JAXBContext.newInstance(RootType.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        RootType root = (RootType) jaxbUnmarshaller.unmarshal(xml);
        DeviceType device = root.getDevice();
        name = device.getFriendlyName();
        getServicesFromDevice(device);
    }


//    private void readIGDDESC() throws ClientProtocolException, IOException, JAXBException {
//        InputStream xml = getXMLIS("/" + FRITZ_IGD_DESC_FILE);
//        JAXBContext jaxbContext = JAXBContext.newInstance(RootType2.class);
//        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//        RootType2 root = (RootType2) jaxbUnmarshaller.unmarshal(xml);
//        DeviceType device = root.getDevice();
//        name = device.getFriendlyName();
//        getServicesFromDevice(device);
//    }

    private void getServicesFromDevice(DeviceType device) throws IOException, JAXBException {
        for (ServiceType sT : device.getServiceList().getService()) {
            String[] tmp = sT.getServiceType().split(":");
            String key = tmp[tmp.length - 2] + ":" + tmp[tmp.length - 1];

            services.put(key, new Service(sT, this));
        }
        if (device.getDeviceList() != null) {
            for (DeviceType d : device.getDeviceList().getDevice()) {
                getServicesFromDevice(d);
            }
        }
    }

    private InputStream httpRequest(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        CloseableHttpResponse response = null;
        byte[] content = null;
        try {
            response = httpClient.execute(target, request, context);
            content = EntityUtils.toByteArray(response.getEntity());
        } catch (IOException e) {
            throw e;
        } finally {
            if (response != null) {
                response.close();
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException(response.getStatusLine().toString());
                }
            }

        }
        if (content != null) {
            return new ByteArrayInputStream(content);
        } else {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    protected InputStream getXMLIS(String fileName) throws IOException {
        HttpGet httpget = new HttpGet(fileName);
        return httpRequest(targetHost, httpget, context);

    }

    protected InputStream getSOAPXMLIS(String fileName, String urn, HttpEntity entity) throws IOException {
        HttpPost httppost = new HttpPost(fileName);
        httppost.addHeader("soapaction", urn);
        httppost.addHeader("charset", "utf-8");
        httppost.addHeader("content-type", "text/xml");
        httppost.setEntity(entity);
        return httpRequest(targetHost, httppost, context);
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public Service getService(String name) {
        return getServices().get(name);
    }

    public String getUser() {
        return this.user;
    }

    public String getPwd() {
        return this.pwd;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void printInfo() {
        System.out.println(name);
        System.out.println("----------------------------------");
        for (String a : services.keySet()) {
            System.out.println(a);
            Service s = services.get(a);
            for (String b : s.getActions().keySet()) {
                System.out.print("    ");
                System.out.println(b);
                System.out.print("       ");
                System.out.println(s.getActions().get(b).getArguments());
            }
        }
    }

}
