package com.example.local;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;



public class ClientMQTT {
    private static final String TAG = "ClientMQTT";
    String clientId = "urn:lo:nsid:localsensor:ONEPLUS2";
    String username = "json+device";
    private MqttAndroidClient client = null;
    //String password = "6341164c7db740e1a7362cdae8b544f1"; //clé du compte de riadh
    String password = "0715af77390c4945ba50e7264c4f9425"; //clé de mon compte
    String topic = "dev/data";
    String subscriptionTopic = "fifo/test";



    public void connect(String address, String port, Context context) {
        client = new MqttAndroidClient(context,
                "tcp://" + address + ":" + port, clientId);
        try {
            final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setMaxInflight(10);
            mqttConnectOptions.setUserName(username);
            mqttConnectOptions.setPassword(password.toCharArray());
            mqttConnectOptions.setKeepAliveInterval(30);
            IMqttToken token = client.connect(mqttConnectOptions); // on tente de se connecter
            //sendMsg("test");
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                                // Nous sommes connecté
                                System.out.println("On est connecté !");
                                //souscrire();
                                if (!client.isConnected()) {
                                    System.out.println("Je suis là");
                                }
                                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                                disconnectedBufferOptions.setBufferEnabled(true);
                                disconnectedBufferOptions.setBufferSize(100);
                                disconnectedBufferOptions.setPersistBuffer(false);
                                disconnectedBufferOptions.setDeleteOldestMessages(false);
                                client.setBufferOpts(disconnectedBufferOptions);
                                }
                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                     Throwable exception) {
                    // Erreur de connexion : temps de connexion trop long ou problème de pare-feu
                    System.err.println("Echec de connection !");
                    }
                });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        //client.setCallback(new MqttCallbackHandler()); // ligne à commenter pour le moment
        }


    public void disconnect() {
        if (client == null) {
            return;
        }
        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener()
            {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                        // Nous nous sommes correctement déconnecté
                    System.out.println("On est déconnecté !");
                        }
                @Override
                public void onFailure(IMqttToken asyncActionToken,
                Throwable exception) {
                        // Quelque chose c'est mal passé, mais on est probablement déconnecté malgré tout
                    System.err.println("Echec de déconnection !");
                        }
                });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void souscrire()
    {
        try
        {
            client.subscribe(subscriptionTopic, 0, null, new IMqttActionListener()
            {
                @Override
                public void onSuccess(IMqttToken asyncActionToken)
                {
                    Log.w(TAG,"souscrire : onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception)
                {
                    Log.w(TAG, "souscrire : onFailure");
                }
            });

        }
        catch (MqttException ex)
        {
            Log.e(TAG, "souscrire : exception");
            ex.printStackTrace();
        }
    }


    public void sendMsg(String msg) {
        String payload = msg;
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            //MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, encodedPayload, 0, false, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("Publier : onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    System.out.println("Publier : onFailure");
                }
            });
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }


    public void publishTextMessage(
            String messageText)
    {
        byte[] bytesMessage;
        try {
            bytesMessage =
                    messageText.getBytes("UTF-8");
            MqttMessage message;
            message = new MqttMessage(bytesMessage);
            client.publish(topic, message);
        } catch (MqttException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }





}
