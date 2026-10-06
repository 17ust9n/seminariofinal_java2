package com.example.seminariofinal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.utils.KeyPair;

public class ProfileActivity extends AppCompatActivity {

    private MaterialToolbar toolbarProfile;
    private ShapeableImageView imgProfilePicture;
    private TextView tvUserName;
    private TextView tvUserEmail;

    private Button btnEditProfile;
    private Button btnCallHistory;
    private Button btnSettings;
    private Button btnTerms;
    private Button btnAbout;
    private Button btnLogout;

    private BottomNavigationView bottomNavigation;

    private LazySodiumAndroid sodium;
    private String myPublicKeyHex = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sodium = SodiumManager.getInstance();

        initViews();
        loadUserData();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void initViews() {
        toolbarProfile = findViewById(R.id.toolbarProfile);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnCallHistory = findViewById(R.id.btnCallHistory);
        btnSettings = findViewById(R.id.btnSettings);
        btnTerms = findViewById(R.id.btnTerms);
        btnAbout = findViewById(R.id.btnAbout);
        btnLogout = findViewById(R.id.btnLogout);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    private void loadUserData() {
        SharedPreferences preferences =
                getSharedPreferences("starssenger_prefs", Context.MODE_PRIVATE);

        String userName = preferences.getString("user_name", "");
        String userPhone = preferences.getString("user_phone", "Sin número");

        int secLevel = preferences.getInt("security_level", 0);
        String secLabel = (secLevel == 1)
                ? "Modo Blindado 🛡️"
                : "Modo Normal ⚡";

        tvUserName.setText(
                userName.isEmpty() ? userPhone : userName
        );

        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences securePrefs =
                    EncryptedSharedPreferences.create(
                            this,
                            "starssenger_secure_prefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );

            myPublicKeyHex =
                    securePrefs.getString("public_key", null);

            String secretKeyHex =
                    securePrefs.getString("secret_key", null);

            /*
             * Si todavía no existe la identidad criptográfica,
             * generamos el par de claves con Lazysodium.
             */
            if (myPublicKeyHex == null || secretKeyHex == null) {

                KeyPair keyPair = sodium.cryptoBoxKeypair();

                myPublicKeyHex =
                        keyPair.getPublicKey().getAsHexString();

                secretKeyHex =
                        keyPair.getSecretKey().getAsHexString();

                securePrefs.edit()
                        .putString("public_key", myPublicKeyHex)
                        .putString("secret_key", secretKeyHex)
                        .apply();
            }

            String shortKey;

            if (myPublicKeyHex.length() > 16) {
                shortKey =
                        myPublicKeyHex.substring(0, 8)
                                + "..."
                                + myPublicKeyHex.substring(
                                myPublicKeyHex.length() - 8
                        );
            } else {
                shortKey = myPublicKeyHex;
            }

            String subtext =
                    userPhone
                            + "\n"
                            + secLabel
                            + "\nClave Pública (Toca para copiar):\n"
                            + shortKey;

            tvUserEmail.setText(subtext);

        } catch (Exception e) {
            e.printStackTrace();

            tvUserEmail.setText(
                    userPhone
                            + "\n"
                            + secLabel
                            + "\nError al cargar identidad Libsodium"
            );
        }
    }

    private void setupListeners() {

        tvUserEmail.setOnClickListener(v -> {

            if (myPublicKeyHex != null
                    && !myPublicKeyHex.isEmpty()) {

                ClipboardManager clipboard =
                        (ClipboardManager)
                                getSystemService(Context.CLIPBOARD_SERVICE);

                ClipData clip =
                        ClipData.newPlainText(
                                "Clave Pública Libsodium",
                                myPublicKeyHex
                        );

                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(
                            this,
                            "¡Clave pública copiada al portapapeles!",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            EditProfileActivity.class
                    );
            startActivity(intent);
        });

        btnCallHistory.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            CallHistoryActivity.class
                    );
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            SettingsActivity.class
                    );
            startActivity(intent);
        });

        btnTerms.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            TermsActivity.class
                    );
            startActivity(intent);
        });

        btnAbout.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            AboutActivity.class
                    );
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logoutUser());

        bottomNavigation.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {

                Intent intent =
                        new Intent(
                                ProfileActivity.this,
                                MainActivity.class
                        );

                intent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                startActivity(intent);
                finish();

                return true;

            } else if (itemId == R.id.nav_profile) {

                return true;
            }

            return false;
        });
    }

    private void logoutUser() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "starssenger_prefs",
                        Context.MODE_PRIVATE
                );

        preferences.edit().clear().apply();

        try {

            MasterKey masterKey =
                    new MasterKey.Builder(this)
                            .setKeyScheme(
                                    MasterKey.KeyScheme.AES256_GCM
                            )
                            .build();

            SharedPreferences securePrefs =
                    EncryptedSharedPreferences.create(
                            this,
                            "starssenger_secure_prefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );

            securePrefs.edit().clear().apply();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent =
                new Intent(
                        ProfileActivity.this,
                        OnboardingActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}