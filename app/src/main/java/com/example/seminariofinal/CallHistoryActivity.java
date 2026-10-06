package com.example.seminariofinal;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CallHistoryActivity extends AppCompatActivity
        implements CallHistoryAdapter.OnCallActionListener {

    private RecyclerView rvCallHistory;
    private LinearLayout emptyHistory;
    private TextView tvCallMessage;

    private CallHistoryAdapter adapter;
    private final List<CallHistory> callList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_history);

        initViews();
        setupRecyclerView();
        loadCallHistory();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        rvCallHistory = findViewById(R.id.rvCallHistory);
        emptyHistory = findViewById(R.id.emptyHistory);
        tvCallMessage = findViewById(R.id.tvCallMessage);
    }

    private void setupRecyclerView() {
        adapter = new CallHistoryAdapter(
                this,
                callList,
                this
        );

        rvCallHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvCallHistory.setAdapter(adapter);
    }

    private void loadCallHistory() {
        callList.clear();

        /*
         * IMPORTANTE:
         * No agregamos llamadas de prueba.
         *
         * El historial se mostrará vacío hasta que
         * el sistema real de llamadas guarde una llamada.
         */

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (callList.isEmpty()) {
            rvCallHistory.setVisibility(View.GONE);
            emptyHistory.setVisibility(View.VISIBLE);
        } else {
            rvCallHistory.setVisibility(View.VISIBLE);
            emptyHistory.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDeleteCall(CallHistory call) {

        new AlertDialog.Builder(this)
                .setTitle("Eliminar llamada")
                .setMessage(
                        "¿Querés eliminar esta llamada del registro?"
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Eliminar",
                        (dialog, which) -> {

                            callList.remove(call);

                            adapter.notifyDataSetChanged();

                            updateEmptyState();

                            showDeleteMessage();
                        }
                )
                .show();
    }

    private void showDeleteMessage() {

        tvCallMessage.setText(
                "Llamada eliminada correctamente"
        );

        tvCallMessage.setVisibility(View.VISIBLE);

        new Handler().postDelayed(
                () -> tvCallMessage.setVisibility(View.GONE),
                3000
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Cuando volvemos a esta pantalla,
         * volvemos a cargar el historial.
         *
         * Esto será importante cuando conectemos
         * el registro con las llamadas reales.
         */
        loadCallHistory();
    }
}