package com.example.local;

import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MapLocationActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationManager locationManager;
    Criteria criteres;
    String fournisseur;
    MqttAndroidClient clientM;
    MqttAndroidClient clientS;
    String serverUri = "liveobjects.orange-business.com";
    String portTTN = "1883";
    String clientId = "urn:lo:nsid:localsensor:" + getDeviceName();
    String clientIdS = "urn:lo:nsid:localsensor:S" + getDeviceName();
    String username = "json+device";
    String usernameS = "application";
    String password = "6341164c7db740e1a7362cdae8b544f1";
    String passwordS = "3bfe078dadd842fca538a66ad8967c00";
    String subscriptionTopic = "fifo/YoussefFifo";
    String publishTopic = "dev/data";
    double latitude; //latitude qu'on reçoit des capteurs
    double longitude; //lagitude qu'on reçcoit des capteurs
    double Lat; //latitude du piéton
    double Lng; //longitude du piéton
    String ACTION_FILTER = "com.example.local";
    final int MSG_ENTRER = 1;
    final int MSG_SORTIR = 2;
    boolean enter = false;
    String Phone_Name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Map Location Activity");
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFrag != null;
        mapFrag.getMapAsync(this);
        connect(serverUri, portTTN, getApplicationContext()); //On se connecte à LiveObject
        connectFiFo(serverUri, portTTN, getApplicationContext()); //On se connecte à la FIFO
        //l'enregistrement de notre BrodcastReceiver
        //registerReceiver(new ProximityReceiver(), new IntentFilter(ACTION_FILTER));

    }

    public String getDeviceName() {
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        return myDevice.getName();
    }

    public void connect(String address, String port, Context context) {
        clientM = new MqttAndroidClient(context,
                "tcp://" + address + ":" + port, clientId);

        try {
            final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setMaxInflight(10);
            mqttConnectOptions.setUserName(username);
            mqttConnectOptions.setPassword(password.toCharArray());
            mqttConnectOptions.setKeepAliveInterval(30);
            IMqttToken token = clientM.connect(mqttConnectOptions); // on tente de se connecter
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Nous sommes connecté
                    System.out.println("On est connecté !");
                    if (!clientM.isConnected()) {
                        System.out.println("On est pas connecté");
                    }
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    clientM.setBufferOpts(disconnectedBufferOptions);
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
    }

    public void connectFiFo (String address, String port, Context context) {
        try {
            clientS = new MqttAndroidClient(context,
                    "tcp://" + address + ":" + port, clientIdS);
            final MqttConnectOptions mqttConnectOptionsS = new MqttConnectOptions();
            mqttConnectOptionsS.setAutomaticReconnect(true);
            mqttConnectOptionsS.setCleanSession(true);
            mqttConnectOptionsS.setMaxInflight(10);
            mqttConnectOptionsS.setUserName(usernameS);
            mqttConnectOptionsS.setPassword(passwordS.toCharArray());
            mqttConnectOptionsS.setKeepAliveInterval(30);
            IMqttToken tokenS = clientS.connect(mqttConnectOptionsS);
            tokenS.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Nous sommes connecté
                    System.out.println("On est connecté à la FIFO!");
                    souscrire();
                    clientS.setCallback(new MqttCallbackExtended() {
                        @Override
                        public void connectComplete(boolean b, String s) {
                            Log.d("SUBSCRIPTION", "connectComplete");
                        }

                        @Override
                        public void connectionLost(Throwable throwable) {
                            Log.d("SUBSCRIPTION", "connectionLost");
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage mqttMessage) {
                            Log.d("SUBSCRIPTION", "messageArrived : " + mqttMessage.toString());
                            String loca = mqttMessage.toString();
                            int deb1 = loca.indexOf("{\"lat\"");
                            int fin1 = loca.indexOf(",\"lon\"", deb1);
                            String s1 = loca.substring(deb1 + 1, fin1);
                            latitude = Double.parseDouble(s1.split(":")[1]);
                            int deb2 = loca.indexOf("\"lon\"");
                            int fin2 = loca.indexOf(",\"provider\"", deb2);
                            String s2 = loca.substring(deb2 + 1, fin2);
                            longitude = Double.parseDouble(s2.split(":")[1]);
                            int deb3= loca.indexOf("localsensor");
                            int fin3 = loca.indexOf(",\"timestamp\"", deb3);
                            String name = loca.substring(deb3 , fin3 - 1);
                            Phone_Name = name.split(":")[1];
                            System.out.println(Phone_Name);
                            if (!getDeviceName().equals(Phone_Name)) {
                                new Thread(r).start();
                            }
                            // On ajoute une alerte de proximité si on s'approche d'un emplacement précis
                            /*Intent i = new Intent(ACTION_FILTER);
                            PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), -1, i, 0);
                            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            locationManager.addProximityAlert(Lat, Lng, 1f, -1, pi); // A changer par la loca du capteur de la carte LTE-SIM
                            //locationManager.addProximityAlert(Lat, Lng, 10f, -1, pi); //m le rayon du cercle de proximité
                            */

                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                            Log.d("SUBSCRIPTION", "deliveryComplete");
                        }
                    });
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    System.out.println("Failure");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    Runnable r = new Runnable() {
        public void run() {
            float[] results = new float[1];
            //Location.distanceBetween(Lat, Lng, 43.60564984592378, 1.4598712182136313, results);
            Location.distanceBetween(Lat, Lng, latitude, longitude, results);
            System.out.println("La distance est:" +(double) results[0]);
            if (results[0] < 10f) {
                System.out.println("True");
                enter = true;
                String messageString = "" + results[0];
                Message msg = mHandler.obtainMessage(
                        MSG_ENTRER, (Object) messageString);
                mHandler.sendMessage(msg);
            }
            else {
                System.out.println("false");
                if (enter) { //càd que y avait déja une voiture qui constituait un danger
                    String messageString = "" + results[0];
                    Message msg = mHandler.obtainMessage(
                            MSG_SORTIR, (Object) messageString);
                    mHandler.sendMessage(msg);
                    enter = false;
                }
            }
        }
    };


    final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_ENTRER) {
                System.out.println("true2");
                AlertDialog.Builder builder = new AlertDialog.Builder(MapLocationActivity.this);
                builder.setTitle("Risque de proximité");
                builder.setMessage("Fais attention, la voiture de " + Phone_Name + " se rapproche dangereusement de toi. Elle est à " + (String) msg.obj + "m de toi.");
                builder.setPositiveButton("OK, j'ai compris", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Le danger a été reconnu", Toast.LENGTH_SHORT).show();
                    }
                });
                AlertDialog myPopup = builder.create();
                myPopup.show();
            }
            if (msg.what == MSG_SORTIR) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapLocationActivity.this);
                builder.setTitle("Pas d'inquiétude");
                builder.setMessage("Plus de véhicule à proximité");
                builder.setPositiveButton("OK, j'ai compris", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Danger écarté", Toast.LENGTH_SHORT).show();
                    }
                });
                AlertDialog Popup = builder.create();
                Popup.show();
            }
        }
    };


    public void sendMsg(JSONObject msg) {
        JSONObject payload = msg;
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.toString().getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(encodedPayload);
            clientM.publish(publishTopic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void souscrire() {
        try {
            clientS.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("souscrire : onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    System.out.println("souscrire : onFailure");
                }
            });
        } catch (MqttException ex) {
            System.out.println("souscrire : exception");
            ex.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //stop location updates when Activity is no longer active
        /*if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }*/
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            final FusedLocationProviderClient client =
                    LocationServices.getFusedLocationProviderClient(this);

            // Get the last known location
            client.getLastLocation()
                    .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            System.out.println("Localisation trouvé");
                        }
                    });
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            criteres = new Criteria();
            // la précision  : (ACCURACY_FINE pour une haute précision ou ACCURACY_COARSE pour une moins bonne précision)
            criteres.setAccuracy(Criteria.ACCURACY_FINE);
            // l'altitude
            criteres.setAltitudeRequired(true);
            // la direction
            criteres.setBearingRequired(true);
            // la vitesse
            criteres.setSpeedRequired(true);
            // la consommation d'énergie demandée
            criteres.setCostAllowed(true);
            criteres.setPowerRequirement(Criteria.POWER_HIGH);
            fournisseur = locationManager.getBestProvider(criteres, true);
            Intent intent = new Intent(this, ProximityReceiver.class);
            PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            locationManager.requestLocationUpdates(fournisseur, 10000, 0, new LocationListener() { // le temps en ms et la distance en m
                @Override
                public void onLocationChanged(Location location) {
                    mLastLocation = location;
                    if (mCurrLocationMarker != null) {
                        mCurrLocationMarker.remove();
                    }
                    //Place current location marker
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("Current Position");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                    mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

                    //move map camera
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                    Lat = location.getLatitude();
                    Lng = location.getLongitude();
                    JSONObject ob1 = new JSONObject();
                    JSONObject ob2 = new JSONObject();
                    try {
                        ob1.put("lat", Double.valueOf(Lat));
                        ob1.put("lon", Double.valueOf(Lng));
                        ob1.put("provider", "Appli mobile");
                        ob2.put("location", ob1);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendMsg(ob2);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }

    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapLocationActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, int[] grantResults) {
        if  (requestCode == MY_PERMISSIONS_REQUEST_LOCATION ) {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);

                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }

            // other 'case' lines to check for otherc
            // permissions this app might request
        }


}

