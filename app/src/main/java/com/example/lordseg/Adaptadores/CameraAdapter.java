package com.example.lordseg.Adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lordseg.Model.CameraModel;
import com.example.lordseg.R;

import java.util.List;

public class CameraAdapter extends RecyclerView.Adapter<CameraAdapter.CameraViewHolder> {

    private List<CameraModel> cameraList;
    private Context context;
    private OnCameraClickListener listener;
    private OnCameraLongClickListener longClickListener;
    private boolean isGradePausada = false;

    public interface OnCameraClickListener {
        void onCameraClick(CameraModel camera);
    }

    public interface OnCameraLongClickListener {
        void onCameraLongClick(CameraModel camera, int position);
    }

    public CameraAdapter(Context context, List<CameraModel> cameraList, OnCameraClickListener listener, OnCameraLongClickListener longClickListener) {
        this.context = context;
        this.cameraList = cameraList;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    // Método para pausar a grade e economizar internet quando a tela cheia abrir
    public void setGradePausada(boolean pausada) {
        this.isGradePausada = pausada;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CameraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_camera_grid, parent, false);
        return new CameraViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CameraViewHolder holder, int position) {
        CameraModel camera = cameraList.get(position);
        holder.textNome.setText(camera.getExibicaoNome());

        // Se a grade estiver pausada, não inicia o vídeo para poupar banda
        if (isGradePausada) {
            holder.releasePlayer();
            return;
        }

        // Inicializa o Sub-stream (Qualidade Baixa)
        holder.initializePlayer(context, camera.getUrlSubStream());

        // Clique para abrir a tela cheia
        holder.itemView.setOnClickListener(v -> listener.onCameraClick(camera));

        // Clique longo para excluir
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCameraLongClick(camera, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return cameraList.size();
    }

    // Regra de Ouro: Destrói o player se o usuário rolar a tela para baixo
    @Override
    public void onViewRecycled(@NonNull CameraViewHolder holder) {
        super.onViewRecycled(holder);
        holder.releasePlayer();
    }

    public static class CameraViewHolder extends RecyclerView.ViewHolder {
        PlayerView playerView;
        TextView textNome;
        ExoPlayer player;

        public CameraViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.player_view_mini);
            textNome = itemView.findViewById(R.id.text_camera_nome);
        }

        @androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
        void initializePlayer(Context context, String urlSubStream) {
            if (player == null) {
                player = new ExoPlayer.Builder(context).build();
                playerView.setPlayer(player);
                MediaItem mediaItem = MediaItem.fromUri(urlSubStream);
                RtspMediaSource rtspSource = new RtspMediaSource.Factory()
                        .setForceUseRtpTcp(true)
                        .createMediaSource(mediaItem);
                player.setMediaSource(rtspSource);
                player.prepare();
                player.setPlayWhenReady(true);
            }
        }

        void releasePlayer() {
            if (player != null) {
                player.release();
                player = null;
                playerView.setPlayer(null);
            }
        }
    }
}