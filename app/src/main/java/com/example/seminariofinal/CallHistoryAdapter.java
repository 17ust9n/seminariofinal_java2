package com.example.seminariofinal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CallHistoryAdapter
        extends RecyclerView.Adapter<CallHistoryAdapter.CallViewHolder> {

    public interface OnCallActionListener {
        void onDeleteCall(CallHistory call);
    }

    private final Context context;
    private final List<CallHistory> callList;
    private final OnCallActionListener listener;

    public CallHistoryAdapter(
            Context context,
            List<CallHistory> callList,
            OnCallActionListener listener
    ) {
        this.context = context;
        this.callList = callList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CallViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_call_history, parent, false);

        return new CallViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CallViewHolder holder,
            int position
    ) {
        CallHistory call = callList.get(position);

        holder.tvCallName.setText(call.getContactName());
        holder.tvCallPhone.setText(call.getContactPhone());
        holder.tvCallDate.setText(call.getDate());

        if ("VI".equals(call.getType())) {
            holder.tvCallIcon.setText("🎥");

            if (call.isOutgoing()) {
                holder.tvCallType.setText("Videollamada realizada");
            } else {
                holder.tvCallType.setText("Videollamada recibida");
            }

        } else {
            holder.tvCallIcon.setText("📞");

            if (call.isOutgoing()) {
                holder.tvCallType.setText("Llamada de voz realizada");
            } else {
                holder.tvCallType.setText("Llamada de voz recibida");
            }
        }

        holder.btnDeleteCall.setOnClickListener(v ->
                listener.onDeleteCall(call)
        );
    }

    @Override
    public int getItemCount() {
        return callList.size();
    }

    static class CallViewHolder extends RecyclerView.ViewHolder {

        TextView tvCallIcon;
        TextView tvCallName;
        TextView tvCallPhone;
        TextView tvCallDate;
        TextView tvCallType;
        ImageButton btnDeleteCall;

        public CallViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCallIcon = itemView.findViewById(R.id.tvCallIcon);
            tvCallName = itemView.findViewById(R.id.tvCallName);
            tvCallPhone = itemView.findViewById(R.id.tvCallPhone);
            tvCallDate = itemView.findViewById(R.id.tvCallDate);
            tvCallType = itemView.findViewById(R.id.tvCallType);
            btnDeleteCall = itemView.findViewById(R.id.btnDeleteCall);
        }
    }
}