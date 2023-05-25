package ro.pub.cs.systems.eim.practicaltest02;

import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;

public class CommunicationThread extends Thread {

    private final ServerThread serverThread;
    private final Socket socket;

    // Constructor of the thread, which takes a ServerThread and a Socket as parameters
    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    // run() method: The run method is the entry point for the thread when it starts executing.
    // It's responsible for reading data from the client, interacting with the server,
    // and sending a response back to the client.
    @Override
    public void run() {
        // It first checks whether the socket is null, and if so, it logs an error and returns.
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            // Create BufferedReader and PrintWriter instances for reading from and writing to the socket
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type!");

            // Read the city and informationType values sent by the client
            String city = bufferedReader.readLine();
            if (city == null || city.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                return;
            }

            // It checks whether the serverThread has already received the weather forecast information for the given city.
            HashMap<String, WeatherForecastInformation> data = serverThread.getData();
            WeatherForecastInformation weatherForecastInformation;
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00+00:00"); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);
            String nowAsISO = df.format(new Date());
            if (data.containsKey(nowAsISO)) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                weatherForecastInformation = data.get(nowAsISO);
            } else {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                String pageSourceCode = "";

                // make the HTTP request to the web service
                HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS);
                HttpResponse httpGetResponse = httpClient.execute(httpGet);
                HttpEntity httpGetEntity = httpGetResponse.getEntity();
                if (httpGetEntity != null) {
                    pageSourceCode = EntityUtils.toString(httpGetEntity);
                }
                if (pageSourceCode == null) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                    return;
                } else Log.i(Constants.TAG, pageSourceCode);

                // Parse the page source code into a JSONObject and extract the needed information
                JSONObject content = new JSONObject(pageSourceCode);
                JSONObject main = content;

                JSONObject time = main.getJSONObject("time");
                String updated = time.getString("updatedISO");

                JSONObject bpi = main.getJSONObject("bpi");
                JSONObject usd = bpi.getJSONObject("USD");
                String rateUSD = usd.getString("rate");

                JSONObject eur = bpi.getJSONObject("EUR");
                String rateEUR = eur.getString("rate");


//                String temperature = main.getString(Constants.TEMP);
//                String pressure = main.getString(Constants.PRESSURE);
//                String humidity = main.getString(Constants.HUMIDITY);
//                JSONObject wind = content.getJSONObject(Constants.WIND);
//                String windSpeed = wind.getString(Constants.SPEED);

                // Create a WeatherForecastInformation object with the information extracted from the JSONObject
                weatherForecastInformation = new WeatherForecastInformation(updated, rateUSD, rateEUR);

                // Cache the information for the given city
                serverThread.setData(updated, weatherForecastInformation);
            }

            if (weatherForecastInformation == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Weather Forecast Information is null!");
                return;
            }

            // Send the information back to the client
//            TimeZone tz = TimeZone.getTimeZone("UTC");
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00+00:00"); // Quoted "Z" to indicate UTC, no timezone offset
//            df.setTimeZone(tz);
//            String nowAsISO = df.format(new Date());
            String result;
            switch (city) {
                case "EUR":
                    result = weatherForecastInformation.getRateEUR() + " - EUR - ";
                    break;
                case "USD":
                    result = weatherForecastInformation.getRateUSD() + " - USD - ";
                    break;
                default:
                    result = "[COMMUNICATION THREAD] Wrong information type (EUR, USD)!";
            }

            // Send the result back to the client
            printWriter.println(result);
            printWriter.flush();
        } catch (IOException | JSONException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } finally {
            try {
                socket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

}

