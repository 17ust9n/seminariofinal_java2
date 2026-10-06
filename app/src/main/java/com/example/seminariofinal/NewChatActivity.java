package com.example.seminariofinal;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class NewChatActivity extends AppCompatActivity {

    private MaterialToolbar toolbarNewChat;
    private EditText etNcSearch;
    private FloatingActionButton btnAddContact, btnImportContacts, btnNewGroup;
    private RecyclerView rvNcList;
    private LinearLayout llGroupsSection, llGroupsContainer;

    private List<Contact> contactList = new ArrayList<>();
    private ContactAdapter adapter;
    private final Gson gson = new Gson();

    // Referencia al diálogo activo de llamada entrante
    private AlertDialog incomingCallDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        initViews();
        setupListeners();
        loadContacts();
        renderNewChat("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderGroups();
    }

    private SharedPreferences getGetPreferencesShared() {
        return getSharedPreferences("starssenger_prefs", MODE_PRIVATE);
    }

    private Boolean isValidSodiumKey(String key) {
        return key != null && key.length() == 64 && key.matches("^[0-9a-fA-F]+$");
    }

    private Dialog createStyledDialog(int layoutResId) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(layoutResId);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    private void saveContact(String name, String phone, String pubKey) {
        Contact newContact = new Contact(name, phone, pubKey);
        contactList.add(newContact);
        saveContactsToPrefs();
        renderNewChat(etNcSearch.getText().toString());
        Toast.makeText(this, "Contacto guardado exitosamente 💾", Toast.LENGTH_SHORT).show();
    }

    /**
     * Muestra el diálogo de llamada entrante con opciones de contestar o rechazar.
     */
    public void showIncomingCallDialog(String callerName) {
        if (isFinishing() || isDestroyed()) return;

        // Si ya hay una llamada en pantalla, se cierra la previa
        if (incomingCallDialog != null && incomingCallDialog.isShowing()) {
            incomingCallDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Llamada entrante")
                .setMessage(callerName + " te está llamando")
                .setCancelable(false) // Impide cerrar al tocar fuera
                .setPositiveButton("Contestar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        acceptCall(callerName);
                    }
                })
                .setNegativeButton("Rechazar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        rejectCall(callerName);
                    }
                });

        incomingCallDialog = builder.create();
        incomingCallDialog.show();
    }

    private void acceptCall(String callerName) {
        Toast.makeText(this, "Llamada aceptada de " + callerName, Toast.LENGTH_SHORT).show();
        // TODO: Iniciar la Activity de llamada o conectar stream WebRTC
    }

    private void rejectCall(String callerName) {
        Toast.makeText(this, "Llamada rechazada", Toast.LENGTH_SHORT).show();
        // TODO: Enviar señal de rechazo al backend/socket
    }

    private void initViews() {
        toolbarNewChat = findViewById(R.id.toolbarNewChat);
        toolbarNewChat.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbarNewChat.setNavigationIconTint(ContextCompat.getColor(this, R.color.green_accent));

        etNcSearch = findViewById(R.id.etNcSearch);
        btnAddContact = findViewById(R.id.btnAddContact);
        btnImportContacts = findViewById(R.id.btnImportContacts);
        btnNewGroup = findViewById(R.id.btnNewGroup);
        rvNcList = findViewById(R.id.rvNcList);
        llGroupsSection = findViewById(R.id.llGroupsSection);
        llGroupsContainer = findViewById(R.id.llGroupsContainer);

        rvNcList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ContactAdapter(new ArrayList<>(), true, new OnContactActionListener() {
            @Override
            public void onContactClick(Contact contact) {
                hideKeyboard();
                Intent intent = new Intent(NewChatActivity.this, ChatActivity.class);
                intent.putExtra("contact_name", contact.getName());
                intent.putExtra("contact_phone", contact.getPhone());
                intent.putExtra("contact_public_key", contact.getPublicKey());
                intent.putExtra("is_group", contact.isGroup());
                startActivity(intent);
            }

            @Override
            public void onEdit(Contact contact, int position) {
                hideKeyboard();
                openEditContactModal(contact);
            }

            @Override
            public void onDelete(Contact contact, int position) {
                hideKeyboard();
                confirmDeleteContact(contact);
            }
        });
        rvNcList.setAdapter(adapter);
    }

    private void setupListeners() {
        toolbarNewChat.setNavigationOnClickListener(v -> {
            hideKeyboard();
            finish();
        });

        etNcSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderNewChat(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAddContact.setOnClickListener(v -> {
            hideKeyboard();
            openAddContactModal();
        });

        btnImportContacts.setOnClickListener(v -> {
            hideKeyboard();
            importToNew();
        });

        btnNewGroup.setOnClickListener(v -> {
            hideKeyboard();
            openNewGroupModal();
        });
    }

    private void loadContacts() {
        SharedPreferences prefs = getGetPreferencesShared();
        String json = prefs.getString("contacts_list", null);

        if (json != null) {
            Type type = new TypeToken<List<Contact>>() {}.getType();
            List<Contact> loadedList = gson.fromJson(json, type);
            contactList = (loadedList != null) ? loadedList : new ArrayList<>();
        } else {
            contactList = new ArrayList<>();
        }
    }

    private void renderNewChat(String query) {
        String cleanQuery = (query == null) ? "" : query.trim().toLowerCase();
        List<Contact> filteredList = new ArrayList<>();

        for (Contact c : contactList) {
            boolean matchesName = c.getName() != null && c.getName().toLowerCase().contains(cleanQuery);
            boolean matchesPhone = c.getPhone() != null && c.getPhone().contains(cleanQuery);

            if (matchesName || matchesPhone) {
                filteredList.add(c);
            }
        }

        if (adapter != null) {
            adapter.updateList(filteredList);
        }
    }

    private void saveContactsToPrefs() {
        SharedPreferences prefs = getGetPreferencesShared();
        String updatedJson = gson.toJson(contactList);
        prefs.edit().putString("contacts_list", updatedJson).apply();
    }

    private void importToNew() {
        Toast.makeText(this, "Accediendo a la agenda...", Toast.LENGTH_SHORT).show();
    }

    private void openAddContactModal() {
        Dialog dialog = createStyledDialog(R.layout.dialog_add_contact);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        if (tvTitle != null) {
            tvTitle.setText("Nuevo contacto +");
            tvTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        EditText etCName = dialog.findViewById(R.id.etCName);
        EditText etCNum = dialog.findViewById(R.id.etCNum);
        EditText etCPublicKey = dialog.findViewById(R.id.etCPublicKey);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveContact);
        Button btnCancel = dialog.findViewById(R.id.btnCancelContact);
        TextView tvCErr = dialog.findViewById(R.id.tvCErr);

        if (btnSave != null) {
            btnSave.setIconResource(R.drawable.ic_save);
            btnSave.setIconTint(null);
            btnSave.setIconPadding(12);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etCName.getText().toString().trim();
            String num = etCNum.getText().toString().trim();
            String pubKey = etCPublicKey != null ? etCPublicKey.getText().toString().trim() : "";

            if (name.isEmpty() || num.isEmpty()) {
                tvCErr.setText("Completa nombre y número de teléfono");
                tvCErr.setVisibility(View.VISIBLE);
            } else if (phoneAlreadyExists(num, null)) {
                tvCErr.setText("Ya existe un contacto con ese número de teléfono");
                tvCErr.setVisibility(View.VISIBLE);
            } else if (!pubKey.isEmpty() && !isValidSodiumKey(pubKey)) {
                tvCErr.setText("La clave pública Libsodium debe tener 64 caracteres Hex");
                tvCErr.setVisibility(View.VISIBLE);
            } else {
                saveContact(name, num, pubKey);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void setupMemberCheckbox(CheckBox cb, LinearLayout actionsLayout) {
        if (actionsLayout != null) {
            for (int i = 0; i < actionsLayout.getChildCount(); i++) {
                View child = actionsLayout.getChildAt(i);
                if (child != cb) child.setVisibility(View.GONE);
            }
            boolean cbInside = actionsLayout.findViewById(R.id.cbMember) != null;
            actionsLayout.setVisibility(cbInside ? View.VISIBLE : View.GONE);
        }

        if (cb != null) {
            cb.setVisibility(View.VISIBLE);

            int green = ContextCompat.getColor(this, R.color.green_accent);
            int gray = Color.parseColor("#8aa0b3");
            ColorStateList tint = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{-android.R.attr.state_checked}
                    },
                    new int[]{green, gray}
            );
            cb.setButtonTintList(tint);
        }
    }

    private String cleanPhone(String p) {
        return p == null ? "" : p.replaceAll("\\D", "");
    }

    private boolean phoneAlreadyExists(String phone, Contact excluded) {
        String target = cleanPhone(phone);
        if (target.isEmpty()) return false;

        for (Contact c : contactList) {
            if (c == excluded) continue;
            if (cleanPhone(c.getPhone()).equals(target)) {
                return true;
            }
        }
        return false;
    }

    private void openEditContactModal(Contact contact) {
        Dialog dialog = createStyledDialog(R.layout.dialog_add_contact);
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        if (tvTitle != null) tvTitle.setText("Modificar contacto");

        EditText etCName = dialog.findViewById(R.id.etCName);
        EditText etCNum = dialog.findViewById(R.id.etCNum);
        EditText etCPublicKey = dialog.findViewById(R.id.etCPublicKey);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveContact);
        Button btnCancel = dialog.findViewById(R.id.btnCancelContact);
        TextView tvCErr = dialog.findViewById(R.id.tvCErr);

        if (etCName != null) etCName.setText(contact.getName());
        if (etCNum != null) etCNum.setText(contact.getPhone());
        if (etCPublicKey != null) etCPublicKey.setText(contact.getPublicKey());

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etCName.getText().toString().trim();
            String num = etCNum.getText().toString().trim();
            String pubKey = etCPublicKey != null ? etCPublicKey.getText().toString().trim() : "";

            if (name.isEmpty() || num.isEmpty()) {
                tvCErr.setText("Completa nombre y número de teléfono");
                tvCErr.setVisibility(View.VISIBLE);
            } else if (phoneAlreadyExists(num, contact)) {
                tvCErr.setText("Ya existe otro contacto con ese número de teléfono");
                tvCErr.setVisibility(View.VISIBLE);
            } else {
                contact.setName(name);
                contact.setPhone(num);
                contact.setPublicKey(pubKey);
                saveContactsToPrefs();
                renderNewChat(etNcSearch.getText().toString());
                dialog.dismiss();
                Toast.makeText(this, "Cambios guardados correctamente 💾", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void confirmDeleteContact(Contact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar contacto")
                .setMessage("¿Estás seguro de que deseas eliminar a " + contact.getName() + " de tu agenda?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    contactList.remove(contact);
                    saveContactsToPrefs();
                    renderNewChat(etNcSearch.getText().toString());
                    Toast.makeText(this, "Contacto removido de la agenda", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void saveNewGroupToPrefs(String groupName, List<Contact> selectedMembers) {
        SharedPreferences prefs = getGetPreferencesShared();
        List<Group> currentGroups = new ArrayList<>();
        String json = prefs.getString("groups_list", null);

        if (json != null) {
            Type type = new TypeToken<ArrayList<Group>>() {}.getType();
            List<Group> loaded = gson.fromJson(json, type);
            if (loaded != null) currentGroups.addAll(loaded);
        }

        Group newGroup = new Group(groupName, selectedMembers);
        currentGroups.add(newGroup);

        String updatedJson = gson.toJson(currentGroups);
        prefs.edit().putString("groups_list", updatedJson).apply();

        Toast.makeText(this, "Grupo '" + groupName + "' creado correctamente 👥", Toast.LENGTH_SHORT).show();
        renderGroups();
    }

    private void renderGroups() {
        if (llGroupsContainer == null) return;
        llGroupsContainer.removeAllViews();

        SharedPreferences prefs = getGetPreferencesShared();
        String json = prefs.getString("groups_list", null);
        if (json == null) {
            llGroupsSection.setVisibility(View.GONE);
            return;
        }

        Type type = new TypeToken<ArrayList<Group>>() {}.getType();
        ArrayList<Group> savedGroups = gson.fromJson(json, type);
        if (savedGroups == null || savedGroups.isEmpty()) {
            llGroupsSection.setVisibility(View.GONE);
            return;
        }

        llGroupsSection.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Group g : savedGroups) {
            if (g == null) continue;
            View row = inflater.inflate(R.layout.item_group_member, llGroupsContainer, false);

            TextView tvAvatar = row.findViewById(R.id.tvMemberAvatar);
            TextView tvName = row.findViewById(R.id.tvMemberName);
            TextView tvPhone = row.findViewById(R.id.tvMemberPhone);
            CheckBox cb = row.findViewById(R.id.cbMember);

            tvAvatar.setText("👥");
            tvAvatar.setTextColor(Color.parseColor("#04240f"));
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(ContextCompat.getColor(this, R.color.green_accent));
            tvAvatar.setBackground(circle);

            tvName.setText(g.getName());
            tvPhone.setText("Sala de chat grupal");
            if (cb != null) cb.setVisibility(View.GONE);

            LinearLayout actionsLayout = row.findViewById(R.id.llActionsContainer);
            if (actionsLayout != null) {
                actionsLayout.removeAllViews();

                Button btnEdit = new Button(this);
                btnEdit.setText("✎");
                btnEdit.setTextSize(18);
                btnEdit.setTextColor(ContextCompat.getColor(this, R.color.green_accent));
                btnEdit.setBackground(null);
                btnEdit.setPadding(12, 0, 12, 0);
                btnEdit.setOnClickListener(v -> openEditGroupModal(g));

                Button btnDelete = new Button(this);
                btnDelete.setText("×");
                btnDelete.setTextSize(22);
                btnDelete.setTextColor(Color.parseColor("#e05a5a"));
                btnDelete.setBackground(null);
                btnDelete.setPadding(12, 0, 12, 0);
                btnDelete.setOnClickListener(v -> confirmDeleteGroup(g));
                actionsLayout.addView(btnEdit);
                actionsLayout.addView(btnDelete);
            }
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("contact_name", g.getName());
                intent.putExtra("contact_phone", "Sala grupal");
                intent.putExtra("is_group", true);
                startActivity(intent);
            });
            llGroupsContainer.addView(row);
        }
    }

    private void openEditGroupModal(Group group) {
        final List<Contact> temporaryMembers = new ArrayList<>();
        if (group.getMembers() != null) {
            temporaryMembers.addAll(group.getMembers());
        }

        Context ctx = this;
        LinearLayout modalLayout = new LinearLayout(ctx);
        modalLayout.setOrientation(LinearLayout.VERTICAL);
        modalLayout.setPadding(45, 30, 45, 30);

        final EditText etGroupNameInput = new EditText(ctx);
        etGroupNameInput.setText(group.getName());
        etGroupNameInput.setHint("Nombre del grupo");
        etGroupNameInput.setSelection(group.getName().length());
        etGroupNameInput.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
        etGroupNameInput.setHintTextColor(Color.parseColor("#8aa0b3"));
        modalLayout.addView(etGroupNameInput);

        final TextView tvCounterLabel = new TextView(ctx);
        tvCounterLabel.setText("Miembros del Grupo (" + temporaryMembers.size() + " seleccionados)");
        tvCounterLabel.setTextColor(ContextCompat.getColor(ctx, R.color.green_accent));
        tvCounterLabel.setTextSize(13);
        tvCounterLabel.setPadding(0, 25, 0, 15);
        modalLayout.addView(tvCounterLabel);

        RecyclerView rvModalList = new RecyclerView(ctx);
        rvModalList.setLayoutManager(new LinearLayoutManager(ctx));

        int maxHeightPx = (int) (200 * getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams rvParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, maxHeightPx
        );
        rvModalList.setLayoutParams(rvParams);

        RecyclerView.Adapter modalAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @androidx.annotation.NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member, parent, false);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(@androidx.annotation.NonNull RecyclerView.ViewHolder holder, int position) {
                Contact contact = contactList.get(position);

                TextView tvAvatar = holder.itemView.findViewById(R.id.tvMemberAvatar);
                TextView tvName = holder.itemView.findViewById(R.id.tvMemberName);
                TextView tvPhone = holder.itemView.findViewById(R.id.tvMemberPhone);
                CheckBox cb = holder.itemView.findViewById(R.id.cbMember);
                LinearLayout actionsLayout = holder.itemView.findViewById(R.id.llActionsContainer);

                setupMemberCheckbox(cb, actionsLayout);

                if (tvName != null) tvName.setText(contact.getName());
                if (tvPhone != null) tvPhone.setText(contact.getPhone());
                if (tvAvatar != null) {
                    String name = contact.getName() != null ? contact.getName() : "";
                    tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
                    tvAvatar.setTextColor(ContextCompat.getColor(ctx, R.color.green_accent));
                    tvAvatar.setBackground(null);
                }

                final String targetCleanPhone = contact.getPhone() != null ? contact.getPhone().replaceAll("\\D", "") : "";

                boolean isCurrentlyChecked = false;
                for (Contact m : temporaryMembers) {
                    String memberCleanPhone = m.getPhone() != null ? m.getPhone().replaceAll("\\D", "") : "";
                    if (!targetCleanPhone.isEmpty() && targetCleanPhone.equals(memberCleanPhone)) {
                        isCurrentlyChecked = true;
                        break;
                    }
                }

                if (cb != null) {
                    cb.setOnCheckedChangeListener(null);
                    cb.setClickable(false);
                    cb.setChecked(isCurrentlyChecked);
                }

                View.OnClickListener toggleListener = v -> {
                    if (cb == null) return;

                    boolean nextState = !cb.isChecked();
                    cb.setChecked(nextState);

                    if (nextState) {
                        boolean exists = false;
                        for (Contact m : temporaryMembers) {
                            String memberCleanPhone = m.getPhone() != null ? m.getPhone().replaceAll("\\D", "") : "";
                            if (memberCleanPhone.equals(targetCleanPhone)) { exists = true; break; }
                        }
                        if (!exists) {
                            Contact cleanContact = new Contact(contact.getName(), targetCleanPhone, contact.getPublicKey());
                            temporaryMembers.add(cleanContact);
                        }
                    } else {
                        temporaryMembers.removeIf(m -> {
                            String memberCleanPhone = m.getPhone() != null ? m.getPhone().replaceAll("\\D", "") : "";
                            return memberCleanPhone.equals(targetCleanPhone);
                        });
                    }

                    notifyItemChanged(position);
                    tvCounterLabel.setText("Miembros del Grupo (" + temporaryMembers.size() + " seleccionados)");
                    tvCounterLabel.setTextColor(ContextCompat.getColor(ctx, R.color.green_accent));
                };

                holder.itemView.setOnClickListener(toggleListener);
            }

            @Override
            public int getItemCount() {
                return contactList.size();
            }
        };

        rvModalList.setAdapter(modalAdapter);
        modalLayout.addView(rvModalList);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Modificar Grupo 👥");
        builder.setView(modalLayout);

        builder.setPositiveButton("Guardar cambios 💾", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog alertDialog = builder.create();
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0b141a")));
        }
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String updatedName = etGroupNameInput.getText().toString().trim();

            if (updatedName.isEmpty()) {
                Toast.makeText(ctx, "Por favor, ingresá un nombre válido para el grupo.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (temporaryMembers.isEmpty()) {
                tvCounterLabel.setTextColor(Color.parseColor("#e05a5a"));
                Toast.makeText(ctx, "Seleccioná al menos un miembro para el grupo", Toast.LENGTH_SHORT).show();
                return;
            }

            group.setName(updatedName);
            group.setMembers(temporaryMembers);

            SharedPreferences prefs = getGetPreferencesShared();
            String json = prefs.getString("groups_list", null);
            if (json != null) {
                Type type = new TypeToken<ArrayList<Group>>() {}.getType();
                ArrayList<Group> savedList = gson.fromJson(json, type);
                if (savedList != null) {
                    for (Group currentGroup : savedList) {
                        if (currentGroup.getId().equals(group.getId())) {
                            currentGroup.setName(updatedName);
                            currentGroup.setMembers(temporaryMembers);
                            break;
                        }
                    }
                    prefs.edit().putString("groups_list", gson.toJson(savedList)).apply();
                }
            }

            renderGroups();
            Toast.makeText(ctx, "Grupo modificado correctamente 💾.", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        });
    }

    private void confirmDeleteGroup(Group group) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar grupo")
                .setMessage("¿Estás seguro de que deseas eliminar por completo el grupo '" + group.getName() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    SharedPreferences prefs = getGetPreferencesShared();
                    String json = prefs.getString("groups_list", null);
                    if (json != null) {
                        Type type = new TypeToken<ArrayList<Group>>() {}.getType();
                        ArrayList<Group> list = gson.fromJson(json, type);
                        if (list != null) {
                            list.removeIf(current -> current.getId().equals(group.getId()));
                            prefs.edit().putString("groups_list", gson.toJson(list)).apply();
                        }
                    }
                    renderGroups();
                    Toast.makeText(this, "Grupo eliminado correctamente 🗑️", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void openNewGroupModal() {
        final List<Contact> selectedMembers = new ArrayList<>();

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_create_group);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etGroupNameInput = dialog.findViewById(R.id.etGCreateName);
        TextView tvCounterLabel = dialog.findViewById(R.id.tvCreateGroupCounterLabel);
        RecyclerView rvModalList = dialog.findViewById(R.id.rvCreateGroupMembers);
        Button btnCancel = dialog.findViewById(R.id.btnCancelCreateGroup);
        Button btnSave = dialog.findViewById(R.id.btnSaveCreateGroup);

        final int counterOriginalColor = tvCounterLabel != null
                ? tvCounterLabel.getCurrentTextColor()
                : ContextCompat.getColor(this, R.color.green_accent);

        if (rvModalList != null) {
            rvModalList.setLayoutManager(new LinearLayoutManager(this));

            RecyclerView.Adapter modalAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @androidx.annotation.NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member, parent, false);
                    return new RecyclerView.ViewHolder(view) {};
                }
                @Override
                public void onBindViewHolder(@androidx.annotation.NonNull RecyclerView.ViewHolder holder, int position) {
                    Contact contact = contactList.get(position);
                    TextView tvAvatar = holder.itemView.findViewById(R.id.tvMemberAvatar);
                    TextView tvName = holder.itemView.findViewById(R.id.tvMemberName);
                    TextView tvPhone = holder.itemView.findViewById(R.id.tvMemberPhone);
                    CheckBox cb = holder.itemView.findViewById(R.id.cbMember);
                    LinearLayout actionsLayout = holder.itemView.findViewById(R.id.llActionsContainer);
                    setupMemberCheckbox(cb, actionsLayout);
                    if (tvName != null) tvName.setText(contact.getName());
                    if (tvPhone != null) tvPhone.setText(contact.getPhone());
                    if (tvAvatar != null) {
                        String name = contact.getName() != null ? contact.getName() : "";
                        tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
                        tvAvatar.setTextColor(ContextCompat.getColor(NewChatActivity.this, R.color.green_accent));
                        tvAvatar.setBackground(null);
                    }

                    final String targetCleanPhone = contact.getPhone() != null ? contact.getPhone().replaceAll("\\D", "") : "";

                    boolean isChecked = false;
                    for (Contact m : selectedMembers) {
                        if (m.getPhone().equals(targetCleanPhone)) { isChecked = true; break; }
                    }
                    if (cb != null) {
                        cb.setOnCheckedChangeListener(null);
                        cb.setClickable(false);
                        cb.setChecked(isChecked);
                    }
                    View.OnClickListener toggleListener = v -> {
                        if (cb == null) return;
                        boolean nextState = !cb.isChecked();
                        cb.setChecked(nextState);
                        if (nextState) {
                            boolean exists = false;
                            for (Contact m : selectedMembers) {
                                if (m.getPhone().equals(targetCleanPhone)) { exists = true; break; }
                            }
                            if (!exists) {
                                Contact cleanContact = new Contact(contact.getName(), targetCleanPhone, contact.getPublicKey());
                                selectedMembers.add(cleanContact);
                            }
                        } else {
                            selectedMembers.removeIf(m -> m.getPhone().equals(targetCleanPhone));
                        }
                        notifyItemChanged(position);
                        if (tvCounterLabel != null) {
                            tvCounterLabel.setText("Seleccionar Miembros (" + selectedMembers.size() + " seleccionados)");
                            tvCounterLabel.setTextColor(counterOriginalColor);
                        }
                    };
                    holder.itemView.setOnClickListener(toggleListener);
                }
                @Override
                public int getItemCount() {
                    return contactList.size();
                }
            };
            rvModalList.setAdapter(modalAdapter);
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String groupName = etGroupNameInput != null ? etGroupNameInput.getText().toString().trim() : "";
                if (groupName.isEmpty()) {
                    Toast.makeText(this, "Ingresá un nombre para el grupo", Toast.LENGTH_SHORT).show();
                } else if (selectedMembers.isEmpty()) {
                    if (tvCounterLabel != null) {
                        tvCounterLabel.setTextColor(Color.parseColor("#e05a5a"));
                    }
                    Toast.makeText(this, "Seleccioná al menos un miembro", Toast.LENGTH_SHORT).show();
                } else {
                    saveNewGroupToPrefs(groupName, selectedMembers);
                    dialog.dismiss();
                }
            });
        }
        dialog.show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}