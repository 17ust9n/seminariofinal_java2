package com.example.seminariofinal;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.utils.Key;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private ImageButton btnBack;
    private ImageButton btnVoiceCall, btnVideoCall;
    private FloatingActionButton btnMic;
    private TextView tvChName, tvChSub, tvChAv;
    private EditText etTxt;
    private RecyclerView rvMessages;

    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();

    private AudioRecorderHelper recorderHelper = new AudioRecorderHelper();
    private String audioFilePath;
    private boolean isRecording = false;
    private String contactPhone = "";

    // Datos de grupo
    private boolean isGroup = false;
    private String groupName = "";

    // Variables criptográficas
    private String contactPublicKeyHex = "";
    private String myPublicKeyHex = "";
    private String mySecretKeyHex = "";
    private LazySodiumAndroid sodium;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sodium = SodiumManager.getInstance();

        initViews();
        getIntentData();
        loadMyKeys();
        loadMessages();
        setupListeners();
    }

    private android.os.Handler pollHandler = new android.os.Handler();
    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            checkNewMessages();
            pollHandler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveMessages();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void loadMyKeys() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    this,
                    "starssenger_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            mySecretKeyHex = securePrefs.getString("secret_key", "");
            myPublicKeyHex = securePrefs.getString("public_key", "");

            if (mySecretKeyHex.isEmpty() || myPublicKeyHex.isEmpty()) {
                com.goterl.lazysodium.utils.KeyPair keyPair = sodium.cryptoBoxKeypair();
                myPublicKeyHex = keyPair.getPublicKey().getAsHexString();
                mySecretKeyHex = keyPair.getSecretKey().getAsHexString();

                securePrefs.edit()
                        .putString("public_key", myPublicKeyHex)
                        .putString("secret_key", mySecretKeyHex)
                        .apply();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String chatKey() {
        if (isGroup) {
            return (groupName == null || groupName.isEmpty())
                    ? ""
                    : "group_" + groupName;
        }
        return contactPhone == null ? "" : contactPhone;
    }

    private void getIntentData() {
        if (getIntent() == null) return;

        isGroup = getIntent().getBooleanExtra("is_group", false);

        if (isGroup) {
            groupName = getIntent().getStringExtra("contact_name");

            if (groupName == null || groupName.trim().isEmpty()) {
                groupName = "Grupo sin nombre";
            }

            contactPhone = getIntent().getStringExtra("contact_phone");
            if (contactPhone == null) {
                contactPhone = "Sala grupal";
            }

            setupGroupHeader();
            return;
        }

        String name = getIntent().getStringExtra("contact_name");
        contactPhone = getIntent().getStringExtra("contact_phone");
        contactPublicKeyHex = getIntent().getStringExtra("contact_public_key");

        if (name != null && !name.trim().isEmpty()) {
            tvChName.setText(name);
            String initial = name.trim().substring(0, 1).toUpperCase();
            if (tvChAv != null) {
                tvChAv.setText(initial);
            }
        } else {
            tvChName.setText("Usuario");
            if (tvChAv != null) {
                tvChAv.setText("U");
            }
        }

        if (contactPhone != null && !contactPhone.isEmpty()) {
            tvChSub.setText(contactPhone);
        }

        if (contactPublicKeyHex == null || contactPublicKeyHex.trim().isEmpty()) {
            try {
                com.goterl.lazysodium.utils.KeyPair dummyPair = sodium.cryptoBoxKeypair();
                contactPublicKeyHex = dummyPair.getPublicKey().getAsHexString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupGroupHeader() {
        tvChName.setText(groupName.isEmpty() ? "Grupo sin nombre" : groupName);

        if (tvChAv != null) {
            tvChAv.setText("👥");
            tvChAv.setBackgroundResource(R.drawable.bg_avatar_circle);
        }

        tvChSub.setVisibility(View.GONE);
        btnVoiceCall.setVisibility(View.GONE);
        btnVideoCall.setVisibility(View.GONE);

        View.OnClickListener openDetail = v -> openGroupDetail();

        if (tvChAv != null) {
            tvChAv.setOnClickListener(openDetail);
        }
        tvChName.setOnClickListener(openDetail);

        etTxt.setHint("Mensaje grupal…");
    }

    private void openGroupDetail() {
        Intent intent = new Intent(this, GroupActivity.class);
        intent.putExtra("group_name", groupName);
        startActivity(intent);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnVoiceCall = findViewById(R.id.btnVoiceCall);
        btnVideoCall = findViewById(R.id.btnVideoCall);

        tvChName = findViewById(R.id.chName);
        tvChSub = findViewById(R.id.chSub);
        tvChAv = findViewById(R.id.chAv);

        btnMic = findViewById(R.id.micBtn);
        etTxt = findViewById(R.id.txtInput);
        rvMessages = findViewById(R.id.chatRecyclerView);

        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        messageAdapter = new MessageAdapter(
                messageList,
                new MessageAdapter.OnMessageActionListener() {

                    @Override
                    public void onDeleteMessage(Message message, int position) {
                        deleteMessage(message, position);
                    }

                    @Override
                    public void onDownloadAudio(Message message) {
                        downloadAudioFile(message.getAudioPath());
                    }
                }
        );

        rvMessages.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, MainActivity.class);
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            startActivity(intent);
            finish();
        });

        btnVoiceCall.setOnClickListener(v -> startCall(false));
        btnVideoCall.setOnClickListener(v -> startCall(true));

        etTxt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }
            return false;
        });

        btnMic.setOnClickListener(v -> {
            String text = etTxt.getText().toString().trim();
            if (!text.isEmpty()) {
                send();
            } else {
                toggleRec();
            }
        });

        etTxt.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    btnMic.setImageResource(android.R.drawable.ic_menu_send);
                } else {
                    btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // =====================================================================
    // Llamadas
    // =====================================================================

    private void startCall(boolean video) {
        if (isGroup) {
            Toast.makeText(this, "Las llamadas grupales no están disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("starssenger_prefs", MODE_PRIVATE);
        String myPhone = prefs.getString("user_phone", "");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("caller_phone", myPhone);
        body.put("receiver_phone", contactPhone);

        apiService.startCall(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Intent intent = new Intent(ChatActivity.this, CallActivity.class);
                intent.putExtra("contact_name", tvChName.getText().toString());
                intent.putExtra("contact_phone", contactPhone);
                intent.putExtra("contact_public_key", contactPublicKeyHex);
                intent.putExtra("is_pending_call", false);
                intent.putExtra("is_video", video);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Error de red al iniciar la llamada", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =====================================================================
    // Persistencia de mensajes
    // =====================================================================

    private void loadMessages() {
        String key = chatKey();
        if (key.isEmpty()) return;

        SharedPreferences prefs = getSharedPreferences("starssenger_prefs", MODE_PRIVATE);
        String json = prefs.getString("chat_messages_" + key, null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Message>>() {
            }.getType();

            List<Message> saved = gson.fromJson(json, type);

            if (saved != null) {
                messageList.clear();

                for (Message msg : saved) {
                    if (msg.getType() == Message.TYPE_TEXT && isEncrypted(msg.getText())) {
                        String decryptedText = decryptText(msg.getText(), msg.isSentByMe());
                        msg.setText(decryptedText);
                    }
                    messageList.add(msg);
                }

                messageAdapter.notifyDataSetChanged();

                if (!messageList.isEmpty()) {
                    rvMessages.scrollToPosition(messageList.size() - 1);
                }
            }
        }
    }

    private void saveMessages() {
        String key = chatKey();
        if (key.isEmpty()) return;

        SharedPreferences prefs = getSharedPreferences("starssenger_prefs", MODE_PRIVATE);
        Gson gson = new Gson();

        List<Message> encryptedList = new ArrayList<>();

        for (Message msg : messageList) {
            if (msg.getType() == Message.TYPE_TEXT && msg.getText() != null) {

                String textToSave = isEncrypted(msg.getText())
                        ? msg.getText()
                        : encryptText(msg.getText());

                Message encMsg = new Message(
                        msg.getId(),
                        textToSave,
                        msg.getAudioPath(),
                        msg.getType(),
                        msg.isSentByMe()
                );

                encryptedList.add(encMsg);

            } else {
                encryptedList.add(msg);
            }
        }

        prefs.edit()
                .putString("chat_messages_" + key, gson.toJson(encryptedList))
                .apply();
    }

    // =====================================================================
    // Cifrado de mensajes de texto
    // =====================================================================

    private void checkNewMessages() {
        if (contactPhone == null || contactPhone.isEmpty()) return;
    }

    private String encryptText(String plainText) {
        if (isGroup) {
            return plainText;
        }

        if (contactPublicKeyHex == null ||
                contactPublicKeyHex.isEmpty() ||
                mySecretKeyHex.isEmpty()) {

            Toast.makeText(this, "Faltan claves criptográficas para enviar", Toast.LENGTH_SHORT).show();
            return plainText;
        }

        try {
            byte[] nonce = sodium.nonce(Box.NONCEBYTES);
            String nonceHex = sodium.toHexStr(nonce);

            Key recipientPubKey = Key.fromHexString(contactPublicKeyHex);
            Key myPrivKey = Key.fromHexString(mySecretKeyHex);

            byte[] messageBytes = plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] cipherBytes = new byte[messageBytes.length + Box.MACBYTES];

            boolean success = sodium.cryptoBoxEasy(
                    cipherBytes,
                    messageBytes,
                    messageBytes.length,
                    nonce,
                    recipientPubKey.getAsBytes(),
                    myPrivKey.getAsBytes()
            );

            if (success) {
                String cipherHex = sodium.toHexStr(cipherBytes);
                return "ENC:" + nonceHex + ":" + cipherHex;
            } else {
                return plainText;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return plainText;
        }
    }

    private String decryptText(String encryptedFormattedText, boolean sentByMe) {
        if (isGroup) {
            return encryptedFormattedText;
        }

        try {
            String[] parts = encryptedFormattedText.split(":");

            if (parts.length != 3 || !parts[0].equals("ENC")) {
                return encryptedFormattedText;
            }

            if (contactPublicKeyHex == null ||
                    contactPublicKeyHex.isEmpty() ||
                    mySecretKeyHex.isEmpty()) {
                return "[Error al descifrar mensaje]";
            }

            byte[] nonce = sodium.toBin(parts[1]);
            byte[] cipherBytes = sodium.toBin(parts[2]);

            if (cipherBytes.length < Box.MACBYTES) {
                return "[Error al descifrar mensaje]";
            }

            Key contactPubKey = Key.fromHexString(contactPublicKeyHex);
            Key myPrivKey = Key.fromHexString(mySecretKeyHex);

            byte[] decryptedBytes = new byte[cipherBytes.length - Box.MACBYTES];

            boolean success = sodium.cryptoBoxOpenEasy(
                    decryptedBytes,
                    cipherBytes,
                    cipherBytes.length,
                    nonce,
                    contactPubKey.getAsBytes(),
                    myPrivKey.getAsBytes()
            );

            if (success) {
                return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
            } else {
                return "[Error al descifrar mensaje]";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "[Error al descifrar mensaje]";
        }
    }

    private boolean isEncrypted(String text) {
        return text != null && text.startsWith("ENC:");
    }

    // =====================================================================
    // Envío y audio
    // =====================================================================

    private void send() {
        String text = etTxt.getText().toString().trim();

        if (!text.isEmpty()) {
            String textToSend = isGroup ? text : encryptText(text);

            Message message = new Message(
                    UUID.randomUUID().toString(),
                    text,
                    null,
                    Message.TYPE_TEXT,
                    true
            );

            messageAdapter.addMessage(message);
            saveMessages();
            etTxt.setText("");
            rvMessages.scrollToPosition(messageList.size() - 1);

            sendMessageToBackend(contactPhone, textToSend);
        }
    }

    private void sendMessageToBackend(String receiverPhone, String content) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Map<String, Object> body = new HashMap<>();
        body.put("receiver_phone", receiverPhone);
        body.put("content", content);

        apiService.sendMessage(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "Error al enviar: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Fallo de conexión al enviar mensaje", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageAdapter != null) {
            messageAdapter.releaseMediaPlayer();
        }
    }

    private void downloadAudioFile(String sourceFilePath) {
        if (sourceFilePath == null) return;

        File sourceFile = new File(sourceFilePath);

        if (!sourceFile.exists()) {
            Toast.makeText(this, "El archivo de audio no existe", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File downloadsDir = android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);

            File destFile = new File(
                    downloadsDir,
                    "Audio_" + System.currentTimeMillis() + ".wav"
            );

            try (FileInputStream in = new FileInputStream(sourceFile);
                 FileOutputStream out = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[1024];
                int read;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            Toast.makeText(this, "Audio guardado en Descargas", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al descargar el audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleRec() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION
            );
            return;
        }

        if (!isRecording) {
            startRecording();
        } else {
            stopRecordingAndSend();
        }
    }

    private void startRecording() {
        audioFilePath = new File(
                getFilesDir(),
                UUID.randomUUID().toString() + ".wav"
        ).getAbsolutePath();

        recorderHelper.startRecording(audioFilePath);
        isRecording = true;

        btnMic.setImageResource(android.R.drawable.ic_media_pause);
        Toast.makeText(this, "Grabando audio...", Toast.LENGTH_SHORT).show();
    }

    private void stopRecordingAndSend() {
        if (isRecording) {

            String encryptedPath = audioFilePath.replace(".wav", ".enc");

            Key key = recorderHelper.generateSecretKey();
            byte[] nonce = recorderHelper.generateNonce();

            recorderHelper.stopRecording(audioFilePath, key, nonce, encryptedPath);

            isRecording = false;
            btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);

            File encryptedFile = new File(encryptedPath);

            if (encryptedFile.exists() && encryptedFile.length() > 0) {

                Message message = new Message(
                        UUID.randomUUID().toString(),
                        null,
                        encryptedPath,
                        Message.TYPE_AUDIO,
                        true
                );

                messageAdapter.addMessage(message);
                saveMessages();
                rvMessages.scrollToPosition(messageList.size() - 1);

            } else {
                Toast.makeText(
                        this,
                        "El audio grabado está vacío o falló el cifrado",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void deleteMessage(Message message, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar mensaje")
                .setMessage("¿Estás seguro de que deseas eliminar este mensaje?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    messageList.remove(position);
                    messageAdapter.notifyItemRemoved(position);
                    saveMessages();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}