package com.example.sshtori.devices;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sshtori.R;
import com.example.sshtori.models.SmartDevice;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<SmartDevice> devices;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(SmartDevice device);
    }

    public DeviceAdapter(List<SmartDevice> devices, OnDeviceClickListener listener) {
        this.devices = new ArrayList<>(devices);
        this.listener = listener;
    }

    public void updateDevices(List<SmartDevice> newDevices) {
        this.devices.clear();
        this.devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        SmartDevice device = devices.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private ImageView deviceIcon;
        private TextView deviceName;
        private TextView deviceType;
        private TextView deviceAddress;
        private ImageView connectionStatus;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.device_card);
            deviceIcon = itemView.findViewById(R.id.device_icon);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceType = itemView.findViewById(R.id.device_type);
            deviceAddress = itemView.findViewById(R.id.device_address);
            connectionStatus = itemView.findViewById(R.id.connection_status);
        }

        public void bind(SmartDevice device, OnDeviceClickListener listener) {
            if (deviceName != null) {
                deviceName.setText(device.getName());
            }

            if (deviceType != null) {
                deviceType.setText(device.getType().getDisplayName());
            }

            if (deviceAddress != null) {
                deviceAddress.setText(device.getBaseUrl());
            }

            if (deviceIcon != null) {
                switch (device.getType()) {
                    case CURTAIN:
                        deviceIcon.setImageResource(R.drawable.ic_curtain);
                        break;
                    case LIGHT:
                        deviceIcon.setImageResource(R.drawable.ic_light);
                        break;
                    case SOCKET:
                        deviceIcon.setImageResource(R.drawable.ic_socket);
                        break;
                    default:
                        deviceIcon.setImageResource(R.drawable.ic_device);
                }
            }

            if (connectionStatus != null) {
                if (device.isConnected()) {
                    connectionStatus.setImageResource(android.R.drawable.presence_online);
                } else {
                    connectionStatus.setImageResource(android.R.drawable.presence_offline);
                }
            }

            if (cardView != null) {
                cardView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeviceClick(device);
                    }
                });
            }
        }
    }
}