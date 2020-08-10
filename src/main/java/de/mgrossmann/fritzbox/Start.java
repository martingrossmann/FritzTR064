package de.mgrossmann.fritzbox;

import de.mapoll.javaAVMTR064.Action;
import de.mapoll.javaAVMTR064.FritzConnection;
import de.mapoll.javaAVMTR064.Response;
import de.mapoll.javaAVMTR064.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Properties;

public class Start {

    private String ip;
    private String user;
    private String password;
    private FritzConnection fritzConnection;

    public static void main(String[] args) throws IOException, JAXBException {
        Start start = new Start();
        start.getCounter();
    }

    public Start() throws IOException, JAXBException {
        this.init();
        fritzConnection = new FritzConnection(ip, user, password);
        fritzConnection.init();
    }

    private void init() {
        Properties prop = new Properties();
        try {
            prop.load(this.getClass().getClassLoader().getResourceAsStream("env.properties"));
            this.ip = prop.getProperty("fritz.ip");
            this.user = prop.getProperty("fritz.user");
            this.password = prop.getProperty("fritz.pass");
        } catch (IOException e) {
            throw new RuntimeException("Cannot load properties", e);
        }
    }

    public void getCounter() throws IOException {
        Service service = this.fritzConnection.getService("WANCommonInterfaceConfig:1");
        Action getTotalBytesReceived = service.getAction("GetTotalBytesReceived");
        Response execute = getTotalBytesReceived.execute();
        System.out.println(execute.getData().toString());
    }

}
