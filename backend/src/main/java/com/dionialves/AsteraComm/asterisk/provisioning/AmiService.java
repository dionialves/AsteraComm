package com.dionialves.AsteraComm.asterisk.provisioning;

import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.action.CommandAction;
import org.asteriskjava.manager.response.CommandResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AmiService {

    @Value("${asterisk.hostname}")
    private String hostname;

    @Value("${asterisk.port}")
    private int port;

    @Value("${asterisk.username}")
    private String username;

    @Value("${asterisk.password}")
    private String password;

    @Async
    public void sendCommand(String command) {
        try {
            ManagerConnectionFactory factory = new ManagerConnectionFactory(hostname, port, username, password);
            ManagerConnection connection = factory.createManagerConnection();
            connection.login();
            connection.sendAction(new CommandAction(command));
            connection.logoff();
        } catch (Exception e) {
            System.err.println("AMI command failed [" + command + "]: " + e.getMessage());
        }
    }

    public CommandResponse sendCommandWithResponse(String command) {
        try {
            ManagerConnectionFactory factory = new ManagerConnectionFactory(hostname, port, username, password);
            ManagerConnection connection = factory.createManagerConnection();
            connection.login();
            CommandResponse response = (CommandResponse) connection.sendAction(new CommandAction(command));
            connection.logoff();
            return response;
        } catch (Exception e) {
            System.err.println("AMI command failed [" + command + "]: " + e.getMessage());
            return null;
        }
    }
}
