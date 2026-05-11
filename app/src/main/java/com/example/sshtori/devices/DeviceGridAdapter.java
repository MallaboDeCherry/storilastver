package com.example.sshtori.devices;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sshtori.R;
import com.example.sshtori.models.SmartDevice;

import java.util.ArrayList;
import java.util.List;

public class DeviceGridAdapter extends RecyclerView.Adapter<DeviceGridAdapter.DeviceViewHolder> {

    private List<SmartDevice> devices;
    private OnDeviceClickListener clickListener;
    private OnDeviceLongClickListener longClickListener;

    // Анимации
    private Animation bounceAnimation;
    private Animation fadeInAnimation;

    public interface OnDeviceClickListener {
        void onDeviceClick(SmartDevice device);
    }

    public interface OnDeviceLongClickListener {
        void onDeviceLongClick(SmartDevice device);
    }

    public DeviceGridAdapter(OnDeviceClickListener clickListener,
                             OnDeviceLongClickListener longClickListener) {
        this.devices = new ArrayList<>();
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
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
                .inflate(R.layout.item_device_grid, parent, false);

        // Инициализация анимаций
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        SmartDevice device = devices.get(position);
        holder.bind(device, clickListener, longClickListener);

        // Анимация появления элемента с задержкой
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(50f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(position * 50)
                .start();
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private ImageView deviceIcon;
        private TextView deviceName;
        private TextView deviceStatus;
        private ImageView connectionIcon;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.device_card);
            deviceIcon = itemView.findViewById(R.id.device_icon);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceStatus = itemView.findViewById(R.id.device_status);
            connectionIcon = itemView.findViewById(R.id.connection_icon);
        }

        public void bind(SmartDevice device,
                         OnDeviceClickListener clickListener,
                         OnDeviceLongClickListener longClickListener) {

            deviceName.setText(device.getName());

            // Устанавливаем иконку в зависимости от типа
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

            // Статус подключения
            if (device.isConnected()) {
                deviceStatus.setText("Подключено");
                deviceStatus.setTextColor(0xFF4CAF50);
                connectionIcon.setImageResource(android.R.drawable.presence_online);
            } else {
                deviceStatus.setText("Не подключено");
                deviceStatus.setTextColor(0xFFF44336);
                connectionIcon.setImageResource(android.R.drawable.presence_offline);
            }

            // Анимация при нажатии с эффектом пульсации
            cardView.setOnClickListener(v -> {
                // Анимация нажатия
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                            if (clickListener != null) {
                                clickListener.onDeviceClick(device);
                            }
                        })
                        .start();
            });

            // Анимация при долгом нажатии
            cardView.setOnLongClickListener(v -> {
                // Анимация увеличения
                v.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(150)
                                    .start();
                            if (longClickListener != null) {
                                longClickListener.onDeviceLongClick(device);
                            }
                        })
                        .start();
                return true;
            });

            // Анимация изменения статуса (пульсация при подключении)
            if (device.isConnected()) {
                connectionIcon.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            connectionIcon.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(200)
                                    .start();
                        })
                        .start();
            }
        }
    }
}