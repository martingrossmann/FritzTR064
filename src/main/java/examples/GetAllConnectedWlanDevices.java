/***********************************************************************************************************************
 *
 * javaAVMTR064 - open source Java TR-064 API
 *===========================================
 *
 * Copyright 2015 Marin Pollmann <pollmann.m@gmail.com>
 *
 *
 ***********************************************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************/

import java.io.IOException;
import java.util.HashMap;

import javax.xml.bind.JAXBException;


import org.apache.http.client.ClientProtocolException;

import de.mapoll.javaAVMTR064.Action;
import de.mapoll.javaAVMTR064.FritzConnection;
import de.mapoll.javaAVMTR064.Response;
import de.mapoll.javaAVMTR064.Service;

public class GetAllConnectedWlanDevices {
    static String ip = "192.168.178.1";
    static String user = "fritz-user";
    static String password = "LJy9T9HrhELnjzeA";


    public static void main(String[] args) throws IOException, JAXBException {
        //Create a new FritzConnection with username and password
        FritzConnection fc = new FritzConnection(ip, user, password);
        try {
            //The connection has to be initiated. This will load the tr64desc.xml respectively igddesc.xml
            //and all the defined Services and Actions.
            fc.init();
        } catch (ClientProtocolException e2) {
            //Any HTTP related error.
            e2.printStackTrace();
        } catch (IOException e2) {
            //Any Network related error.
            e2.printStackTrace();
        } catch (JAXBException e2) {
            //Any xml violation.
            e2.printStackTrace();
        }

        Action getTotalBytesReceived = fc.getService("WANCommonInterfaceConfig:1").getAction("GetTotalBytesReceived");
        Response execute = getTotalBytesReceived.execute();
        System.out.println(execute.getData().toString());

        for (int i = 1; i <= 3; i++) {
            //Get the Service. In this case WLANConfiguration:X
            Service service = fc.getService("WLANConfiguration:" + i);
            //Get the Action. in this case GetTotalAssociations
            Action action = service.getAction("GetTotalAssociations");
            Response response1 = null;
            try {
                //Execute the action without any In-Parameter.
                response1 = action.execute();
            } catch (UnsupportedOperationException | IOException e1) {

                e1.printStackTrace();
            }
            int deviceCount = -1;
            try {
                //Get the value from the field NewTotalAssociations as an integer. Values can have the Types: String, Integer, Boolean, DateTime and UUID
                deviceCount = response1.getValueAsInteger("NewTotalAssociations");
            } catch (ClassCastException | NoSuchFieldException e) {
                e.printStackTrace();
            }
            System.out.println("WLAN " + i + ":" + deviceCount);
            for (int j = 0; j < deviceCount; j++) {
                //Create a map for the arguments of an action. You have to do this, if the action has IN-Parameters.
                HashMap<String, Object> arguments = new HashMap<String, Object>();
                //Set the argument NewAssociatedDeviceIndex to an integer value.
                arguments.put("NewAssociatedDeviceIndex", j);
                try {
                    Response response2 = fc.getService("WLANConfiguration:" + i).getAction("GetGenericAssociatedDeviceInfo").execute(arguments);
                    System.out.println("    " + response2.getData());
                } catch (UnsupportedOperationException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
