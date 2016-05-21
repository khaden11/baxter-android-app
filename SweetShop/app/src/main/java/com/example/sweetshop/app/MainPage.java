package com.example.sweetshop.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class MainPage extends Activity implements View.OnClickListener{

    public Socket s, hostsocket;
    public DataInputStream dis;
    public String serverCommand, customerOrder;
    public int blueInt, newBlue, redInt, newRed, greenInt, newGreen;

    public TextView blueAmount, redAmount, greenAmount;

    private Intent recognizerIntent;
    public SpeechRecognizer speech;

    private static final int REQUEST_CODE = 1234;

    private static final String TAG = MainPage.class.getName();

    //wakelock to keep screen on
    protected PowerManager.WakeLock mWakeLock;

    //speach recognizer for callbacks
    private SpeechRecognizer mSpeechRecognizer;

    //handler to post changes to progress bar
    private Handler mHandler = new Handler();

    //intent for speech recogniztion
    Intent mSpeechIntent;

    public String ipUserInput;

    //legel commands
    private static final String[] VALID_COMMANDS = {
            "Best Website for Android",
            "what day is it",
            "who are you",
            "exit"
    };
    private static final int VALID_COMMANDS_SIZE = VALID_COMMANDS.length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main_page);

        ipUserInput = getInput();
    }

    public synchronized String getInput()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(MainPage.this);
                //customize alert dialog to allow desired input
                // Setting Dialog Title
                alert.setTitle("IP ADDRESS");

                // Setting Dialog Message
                alert.setMessage("ENTER IP DISPLAYED IN THE TERMINAL NOW");
                final EditText input = new EditText(MainPage.this);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        ipUserInput = input.getText().toString();
                        //notify();
                        new ConnectToServer().execute("");
                    }
                });
                alert.show();
            }
        });

        return ipUserInput;
    }

    private class ConnectToServer extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                s = new Socket(ipUserInput, 8000);

                //read input stream
                DataInputStream dis2 = new DataInputStream(s.getInputStream());
                InputStreamReader disR2 = new InputStreamReader(dis2);
                BufferedReader br = new BufferedReader(disR2);//create a BufferReader object for input

                serverCommand = br.readLine();
                /*while (!serverCommand.equals("Get choice")) {
                    serverCommand = br.readLine();
                }*/

                dis2.close();
                s.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return serverCommand;
        }

        @Override
        protected void onPostExecute(String result) {
            setContentView(R.layout.choose_sweets_page);

            blueAmount = (TextView) findViewById(R.id.blue_amount);
            redAmount = (TextView) findViewById(R.id.red_amount);
            greenAmount = (TextView) findViewById(R.id.green_amount);

            //speech.startListening(recognizerIntent);
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainPage.this);
            SpeechListener mRecognitionListener = new SpeechListener();
            mSpeechRecognizer.setRecognitionListener(mRecognitionListener);
            mSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            mSpeechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getApplicationContext().getPackageName());

            // Given an hint to the recognizer about what the user is going to say
            /*mSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);*/
            mSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");


            // Specify how many results you want to receive. The results will be sorted
            // where the first result is the one with higher confidence.
            mSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);


            //mSpeechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            mSpeechRecognizer.startListening(mSpeechIntent);
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    @Override
    protected void onPause() {
        //kill the voice recognizer
        if(mSpeechRecognizer != null){
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
        super.onPause();
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    private class SendOrderToServer extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                Log.e("sending order", "sending order");
                //DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                //dos.writeUTF(customerOrder);

                //PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
                //out.println(customerOrder);
                //InetAddress address = InetAddress.getLocalHost();
                s = new Socket(ipUserInput, 8000);

                OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());

                osw.write(customerOrder);
                osw.flush();



                s.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Sent message";
        }

        @Override
        protected void onPostExecute(String result) {
            setContentView(R.layout.activity_main_page);

            try {
                Thread.sleep(4);
                new ConnectToServer().execute("");
            }
            catch (InterruptedException e) {}

        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_blue_sweet:
                blueInt = Integer.parseInt(blueAmount.getText().toString());
                newBlue = blueInt + 1;
                if (newBlue == 4) {
                    newBlue = 3;
                }
                blueAmount.setText(String.valueOf(newBlue));
                break;
            case R.id.subtract_blue_sweet:
                blueInt = Integer.parseInt(blueAmount.getText().toString());
                newBlue = blueInt - 1;
                if (newBlue == -1) {
                    newBlue = 0;
                }
                blueAmount.setText(String.valueOf(newBlue));
                break;
            case R.id.add_red_sweet:
                redInt = Integer.parseInt(redAmount.getText().toString());
                newRed = redInt + 1;
                if (newRed == 4) {
                    newRed = 3;
                }
                redAmount.setText(String.valueOf(newRed));
                break;
            case R.id.subtract_red_sweet:
                redInt = Integer.parseInt(redAmount.getText().toString());
                newRed = redInt - 1;
                if (newRed == -1) {
                    newRed = 0;
                }
                redAmount.setText(String.valueOf(newRed));
                break;
            case R.id.add_green_sweet:
                greenInt = Integer.parseInt(greenAmount.getText().toString());
                newGreen = greenInt + 1;
                if (newGreen == 4) {
                    newGreen = 3;
                }
                greenAmount.setText(String.valueOf(newGreen));
                break;
            case R.id.subtract_green_sweet:
                greenInt = Integer.parseInt(greenAmount.getText().toString());
                newGreen = greenInt - 1;
                if (newGreen == -1) {
                    newGreen = 0;
                }
                greenAmount.setText(String.valueOf(newGreen));
                break;
            case R.id.order_sweets:
                blueInt = Integer.parseInt(blueAmount.getText().toString());
                redInt = Integer.parseInt(redAmount.getText().toString());
                greenInt = Integer.parseInt(greenAmount.getText().toString());
                customerOrder = String.valueOf(blueInt)+" blue sweets, "+String.valueOf(redInt)+" red sweets, "+
                                String.valueOf(greenInt)+" green sweets";
                SendOrderToServer orderTask = new SendOrderToServer();
                orderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case R.id.voice_command:
                mSpeechRecognizer.cancel();
                mSpeechRecognizer.startListening(mSpeechIntent);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches_text = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.e("RESULTS: ", matches_text.get(0));

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        /*mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainPage.this);
        SpeechListener mRecognitionListener = new SpeechListener();
        mSpeechRecognizer.setRecognitionListener(mRecognitionListener);
        mSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getApplicationContext().getPackageName());

        // Given an hint to the recognizer about what the user is going to say
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");


        // Specify how many results you want to receive. The results will be sorted
        // where the first result is the one with higher confidence.
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);


        //mSpeechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        mSpeechRecognizer.startListening(mSpeechIntent);*/
        super.onStart();
    }

    class SpeechListener implements RecognitionListener {
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "buffer recieved ");
        }
        public void onError(int error) {
            Log.d("ERROR NUMBER: ", String.valueOf(error));
            //if critical error then exit
            if(error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
                Log.d(TAG, "client error");
            }
            //else ask to repeats
            else{
                Log.d(TAG, "other error");
                //mSpeechRecognizer.startListening(mSpeechIntent);
            }
        }
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent");
        }
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "partial results");
        }
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "on ready for speech");
        }
        public void onResults(Bundle results) {
            Log.d(TAG, "on results");
            ArrayList<String> matches = null;
            if(results != null){
                matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null){
                    Log.d(TAG, "results are " + matches.toString());
                    final ArrayList<String> matchesStrings = matches;
                    //processCommand(matchesStrings);

                    //mSpeechRecognizer.startListening(mSpeechIntent);
                    //finish();
                    analyseVoiceCommand(matches.get(0));
                }
            }
            Log.e("Finished speech", " FINITO");

        }
        public void onRmsChanged(float rmsdB) {
            //   Log.d(TAG, "rms changed");
        }
        public void onBeginningOfSpeech() {
            Log.d(TAG, "speach begining");
        }
        public void onEndOfSpeech() {
            Log.d(TAG, "speach done");
        }

    };

    public void analyseVoiceCommand(String cmd) {
        Toast.makeText(this, cmd,
                Toast.LENGTH_LONG).show();
        String[] words = cmd.split(" " +
                "");
        String blueNum = blueAmount.getText().toString(), greenNum = greenAmount.getText().toString(), redNum = redAmount.getText().toString();
        List<String> list = Arrays.asList(words);
        if (list.contains("blue")) {
            int blueIndex = list.indexOf("blue");
            blueNum = list.get(blueIndex - 1);
        }
        if (list.contains("Blue")) {
            int greenIndex = list.indexOf("Blue");
            greenNum = list.get(greenIndex - 1);
        }
        if (list.contains("green")) {
            int greenIndex = list.indexOf("green");
            greenNum = list.get(greenIndex - 1);
        }
        if (list.contains("Green")) {
            int greenIndex = list.indexOf("Green");
            greenNum = list.get(greenIndex - 1);
        }
        if (list.contains("red")) {
            int redIndex = list.indexOf("red");
            redNum = list.get(redIndex - 1);
        }
        if (list.contains("Red")) {
            int redIndex = list.indexOf("Red");
            redNum = list.get(redIndex - 1);
        }
        Log.d("AMOUNTS SAID: ", blueNum+" "+greenNum+" "+redNum);

        blueAmount.setText(getNum(blueNum));
        greenAmount.setText(getNum(greenNum));
        redAmount.setText(getNum(redNum));
    }

    public String getNum(String num) {
        if (num.equals("one") || num.equals("1")) {
            return "1";
        }
        else if (num.equals("two") || num.equals("to") || num.equals("too") || num.equals("2") || num.contains("to")) {
            return "2";
        }
        else if (num.equals("three") || num.equals("3") || num.equals("free")) {
            return "3";
        }
        else {
            return "0";
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        speech.destroy();
    }
}
