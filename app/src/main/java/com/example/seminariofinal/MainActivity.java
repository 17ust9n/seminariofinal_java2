package com.example.seminariofinal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.SodiumAndroid;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "POLLING_CALL";
    private static final long POLL_INTERVAL_MS = 2000;

    private EditText etSearch;
    private RecyclerView rvChatList;
    private FloatingActionButton fabNewChat;
    private BottomNavigationView bottomNavigation;

    private boolean isUserLoggedIn = false;
    private boolean isClientConnected = false;
    private List<Contact> contactList = new ArrayList<>();
    private ContactAdapter adapter;

    private LazySodiumAndroid lazySodium;

    // --- LLAMADAS ---
    private ApiService api;
    private AlertDialog incomingCallDialog;
    private String currentCallerPhone = null;   // llamada ya mostrada/atendida
    private final Handler callPollingHandler = new Handler(Looper.getMainLooper());
    private Runnable callPollingRunnable;
    private String myUserPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = getSharedPreferences("starssenger_prefs", Context.MODE_PRIVATE);
        isUserLoggedIn = preferences.getBoolean("is_logged_in", false);
        myUserPhone = preferences.getString("user_phone", "");
        Log.d(TAG, "Teléfono del usuario: '" + myUserPhone + "'");

        if (!isUserLoggedIn) {
            showOnboarding();
            return;
        }

        setContentView(R.layout.activity_main);

        lazySodium = new LazySodiumAndroid(new SodiumAndroid());
        api = ApiClient.getClient().create(ApiService.class);

        initViews();

        if (!isClientConnected) {
            startNet();
        } else {
            subscribeToTopics();
        }

        setupListeners();
        setupCallPolling();
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuildConvs();
        if (etSearch != null) {
            renderList(etSearch.getText().toString());
        }
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
        if (callPollingRunnable != null) {
            callPollingHandler.removeCallbacks(callPollingRunnable);
            callPollingHandler.post(callPollingRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (callPollingRunnable != null) {
            callPollingHandler.removeCallbacks(callPollingRunnable);
        }
        dismissCallDialog();
        currentCallerPhone = null;
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        rvChatList = findViewById(R.id.rvChatList);
        fabNewChat = findViewById(R.id.fabNewChat);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        fabNewChat.setImageResource(R.drawable.ic_edit_pencil);
        rvChatList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ContactAdapter(new ArrayList<>(), false, new OnContactActionListener() {
            @Override
            public void onContactClick(Contact contact) {
                hideKeyboard();
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("contact_name", contact.getName());
                intent.putExtra("contact_phone", contact.getPhone());
                intent.putExtra("is_group", contact.isGroup());
                startActivity(intent);
            }
            @Override public void onEdit(Contact contact, int position) {}
            @Override public void onDelete(Contact contact, int position) {}
        });

        rvChatList.setAdapter(adapter);
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fabNewChat.setOnClickListener(v -> openNewChat());

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                hideKeyboard();
                openProfile();
                return true;
            }
            return false;
        });
    }

    // ================= LLAMADAS =================

    private void setupCallPolling() {
        callPollingRunnable = new Runnable() {
            @Override
            public void run() {
                checkIncomingCallApi();
                callPollingHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
    }

    private void checkIncomingCallApi() {
        if (myUserPhone == null || myUserPhone.trim().isEmpty()) {
            Log.w(TAG, "myUserPhone vacío: el login no guardó 'user_phone'");
            return;
        }

        api.checkIncomingCall(myUserPhone).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isFinishing() || isDestroyed()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "Respuesta no válida, código: " + response.code());
                    return;
                }

                Map<String, Object> data = response.body();
                Log.d(TAG, "Respuesta: " + data);

                boolean hasCall = Boolean.TRUE.equals(data.get("has_call"));
                if (!hasCall) {
                    dismissCallDialog();
                    currentCallerPhone = null;
                    return;
                }

                String callerPhone = String.valueOf(data.get("caller_phone"));
                if (callerPhone.equals(currentCallerPhone)) return;

                Object nameObj = data.get("caller_name");
                String callerName = nameObj != null ? String.valueOf(nameObj) : "";
                String displayName = !callerName.isEmpty() ? callerName : callerPhone;

                currentCallerPhone = callerPhone;
                showIncomingCallDialog(displayName, callerPhone);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error al consultar llamadas: " + t.getMessage());
            }
        });
    }

    public void showIncomingCallDialog(String callerName, String callerPhone) {
        if (isFinishing() || isDestroyed()) return;
        dismissCallDialog();

        incomingCallDialog = new AlertDialog.Builder(this)
                .setTitle("Llamada entrante")
                .setMessage(callerName + " te está llamando")
                .setCancelable(false)
                .setPositiveButton("Contestar", (d, w) -> sendCallResponseApi(callerName, callerPhone, "accept"))
                .setNegativeButton("Rechazar", (d, w) -> sendCallResponseApi(callerName, callerPhone, "reject"))
                .create();
        incomingCallDialog.show();
    }

    private void dismissCallDialog() {
        if (incomingCallDialog != null && incomingCallDialog.isShowing()) {
            incomingCallDialog.dismiss();
        }
        incomingCallDialog = null;
    }

    private void sendCallResponseApi(String callerName, String callerPhone, String action) {
        Map<String, Object> body = new HashMap<>();
        body.put("caller_phone", callerPhone);
        body.put("receiver_phone", myUserPhone);
        body.put("action", action);

        api.respondCall(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "respondCall falló, código: " + response.code());
                    Toast.makeText(MainActivity.this, "No se pudo responder la llamada", Toast.LENGTH_SHORT).show();
                    return;
                }
                if ("accept".equals(action)) {
                    Intent intent = new Intent(MainActivity.this, CallActivity.class);
                    intent.putExtra("contact_phone", callerPhone);
                    intent.putExtra("contact_name", callerName);
                    intent.putExtra("is_pending_call", true);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Llamada rechazada", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error de red al responder", t);
                Toast.makeText(MainActivity.this, "Error de red al responder llamada", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void startCallApi(String targetPhone) {
        Map<String, Object> body = new HashMap<>();
        body.put("caller_phone", myUserPhone);
        body.put("receiver_phone", targetPhone);

        api.startCall(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Toast.makeText(MainActivity.this,
                        response.isSuccessful() ? "Llamando..." : "No se pudo iniciar la llamada",
                        Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de red al llamar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOnboarding() {
        startActivity(new Intent(MainActivity.this, OnboardingActivity.class));
        finish();
    }

    private void rebuildConvs() {
        SharedPreferences prefs = getSharedPreferences("starssenger_prefs", MODE_PRIVATE);
        Gson gson = new Gson();
        contactList = new ArrayList<>();

        String contactsJson = prefs.getString("contacts_list", null);
        if (contactsJson != null) {
            Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
            ArrayList<Contact> savedContacts = gson.fromJson(contactsJson, type);
            if (savedContacts != null) contactList.addAll(savedContacts);
        }

        String groupsJson = prefs.getString("groups_list", null);
        if (groupsJson != null) {
            Type type = new TypeToken<ArrayList<Group>>() {}.getType();
            ArrayList<Group> savedGroups = gson.fromJson(groupsJson, type);
            if (savedGroups != null) {
                for (Group g : savedGroups) {
                    if (g != null) {
                        String verifiedName = null;
                        if (g.getName() != null && !g.getName().trim().isEmpty()) {
                            verifiedName = g.getName();
                        } else {
                            try {
                                java.lang.reflect.Field field = g.getClass().getDeclaredField("groupName");
                                field.setAccessible(true);
                                verifiedName = (String) field.get(g);
                            } catch (Exception ignored) {}
                        }
                        if (verifiedName == null || verifiedName.trim().isEmpty()) {
                            verifiedName = "Sala de Chat #" + Math.abs(g.hashCode() % 1000);
                        }
                        Contact groupContact = new Contact();
                        groupContact.setName(verifiedName);
                        groupContact.setPhone("Sala grupal");
                        groupContact.setGroup(true);
                        contactList.add(groupContact);
                    }
                }
            }
        }
    }

    private void renderList(String query) {
        List<Contact> filteredList = new ArrayList<>();
        for (Contact c : contactList) {
            String name = c.getName() != null ? c.getName() : "";
            String phone = c.getPhone() != null ? c.getPhone() : "";
            if (name.toLowerCase().contains(query.toLowerCase()) || phone.contains(query)) {
                filteredList.add(c);
            }
        }
        if (adapter != null) adapter.updateList(filteredList);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void startNet() {}
    private void subscribeToTopics() {}

    private void openNewChat() {
        hideKeyboard();
        startActivity(new Intent(MainActivity.this, NewChatActivity.class));
    }

    private void openProfile() {
        hideKeyboard();
        startActivity(new Intent(MainActivity.this, ProfileActivity.class));
    }

    public LazySodiumAndroid getLazySodium() {
        return lazySodium;
    }
}