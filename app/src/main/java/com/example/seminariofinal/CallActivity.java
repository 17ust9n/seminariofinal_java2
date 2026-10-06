package com.example.seminariofinal;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.utils.Key;
import com.goterl.lazysodium.utils.KeyPair;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallActivity extends AppCompatActivity {

    private LinearLayout layoutInCall;
    private TextView tvIcName, tvVfName, tvVfState;
    private Button btnDeclineCall, btnAnswerCall;
    private FloatingActionButton fabAddCall, fabHangup;

    private boolean isPendingCall = false;
    private String contactName;
    private String contactPhone;
    private String contactPublicKey;

    // Elementos de seguridad efímeros
    private KeyPair ephemeralKeyPair;
    private byte[] sessionKey;
    private byte[] callNonce;

    // Helper de Audio y Rutas
    private AudioRecorderHelper audioRecorderHelper;
    private String tempWavPath;
    private String encryptedAudioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        audioRecorderHelper = new AudioRecorderHelper();
        setupAudioPaths();

        getIntentData();
        initViews();
        setupListeners();
        checkPendingCall();
        generateEphemeralKeys();
    }

    private void setupAudioPaths() {
        File dir = getExternalFilesDir(null);
        if (dir != null) {
            long timestamp = System.currentTimeMillis();
            tempWavPath = dir.getAbsolutePath() + "/call_recording_" + timestamp + ".wav";
            encryptedAudioPath = dir.getAbsolutePath() + "/call_recording_" + timestamp + ".enc";
        }
    }

    private void getIntentData() {
        if (getIntent() != null) {
            contactName = getIntent().getStringExtra("contact_name");
            contactPhone = getIntent().getStringExtra("contact_phone");
            contactPublicKey = getIntent().getStringExtra("contact_public_key");
            isPendingCall = getIntent().getBooleanExtra("is_pending_call", false);
        }
    }

    private void initViews() {
        layoutInCall = findViewById(R.id.layoutInCall);
        tvIcName = findViewById(R.id.tvIcName);
        tvVfName = findViewById(R.id.tvVfName);
        tvVfState = findViewById(R.id.tvVfState);
        btnDeclineCall = findViewById(R.id.btnDeclineCall);
        btnAnswerCall = findViewById(R.id.btnAnswerCall);
        fabAddCall = findViewById(R.id.fabAddCall);
        fabHangup = findViewById(R.id.fabHangup);

        if (contactName != null) {
            tvVfName.setText(contactName);
        }
    }

    private void setupListeners() {
        fabHangup.setOnClickListener(v -> hangup());
        fabAddCall.setOnClickListener(v -> openInviteModal());

        btnDeclineCall.setOnClickListener(v -> declineCall());
        btnAnswerCall.setOnClickListener(v -> answerCall());
    }

    private void generateEphemeralKeys() {
        new Thread(() -> {
            try {
                LazySodiumAndroid sodium = SodiumManager.getInstance();
                ephemeralKeyPair = sodium.cryptoBoxKeypair();
                callNonce = audioRecorderHelper.generateNonce();
            } catch (com.goterl.lazysodium.exceptions.SodiumException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error al generar claves efímeras: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void checkPendingCall() {
        if (isPendingCall) {
            tvIcName.setText(contactName != null ? contactName : "Llamada entrante");
            layoutInCall.setVisibility(View.VISIBLE);
        } else {
            tvVfState.setText("Llamando...");
        }
    }

    private void answerCall() {
        layoutInCall.setVisibility(View.GONE);
        tvVfState.setText("Conectando de forma segura...");

        SharedPreferences prefs = getSharedPreferences("starssenger_prefs", MODE_PRIVATE);
        String myPhone = prefs.getString("user_phone", "");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("caller_phone", contactPhone);
        body.put("receiver_phone", myPhone);
        body.put("action", "accept");

        apiService.respondCall(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {}

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        new Thread(() -> {
            try {
                if (contactPublicKey != null && !contactPublicKey.isEmpty() && ephemeralKeyPair != null) {
                    LazySodiumAndroid sodium = SodiumManager.getInstance();

                    Key remotePubKey = Key.fromHexString(contactPublicKey);
                    sessionKey = new byte[Box.BEFORENMBYTES];

                    boolean success = sodium.cryptoBoxBeforeNm(
                            sessionKey,
                            remotePubKey.getAsBytes(),
                            ephemeralKeyPair.getSecretKey().getAsBytes()
                    );

                    if (success) {
                        runOnUiThread(() -> tvVfState.setText("En llamada (E2EE 🔒)"));
                        audioRecorderHelper.startRecording(tempWavPath);
                    } else {
                        runOnUiThread(() -> tvVfState.setText("En llamada (Sin cifrar)"));
                    }
                } else {
                    runOnUiThread(() -> tvVfState.setText("En llamada (Sin cifrar)"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error en handshake", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void hangup() {
        stopAndEncryptAudio();
        cleanUpSession();
        finish();
    }

    private void declineCall() {
        SharedPreferences prefs = getSharedPreferences("starssenger_prefs", MODE_PRIVATE);
        String myPhone = prefs.getString("user_phone", "");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("caller_phone", contactPhone);
        body.put("receiver_phone", myPhone);
        body.put("action", "reject");

        apiService.respondCall(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {}

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        cleanUpSession();
        layoutInCall.setVisibility(View.GONE);
        finish();
    }

    private void stopAndEncryptAudio() {
        if (sessionKey != null && callNonce != null) {
            Key keyObject = Key.fromBytes(sessionKey);
            audioRecorderHelper.stopRecording(tempWavPath, keyObject, callNonce, encryptedAudioPath);
        } else {
            audioRecorderHelper.stopRecording(tempWavPath, null, null, null);
        }
    }

    private void cleanUpSession() {
        if (sessionKey != null) {
            java.util.Arrays.fill(sessionKey, (byte) 0);
            sessionKey = null;
        }
        if (callNonce != null) {
            java.util.Arrays.fill(callNonce, (byte) 0);
            callNonce = null;
        }
        ephemeralKeyPair = null;
    }

    private void openInviteModal() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_invite_call);

        Button btnInvite = dialog.findViewById(R.id.btnDoInvite);
        Button btnCancel = dialog.findViewById(R.id.btnCancelInvite);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnInvite.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}