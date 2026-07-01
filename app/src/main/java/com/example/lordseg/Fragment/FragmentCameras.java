package com.example.lordseg.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lordseg.Adaptadores.CameraAdapter;
import com.example.lordseg.Model.CameraModel;
import com.example.lordseg.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentCameras extends Fragment {

    private RecyclerView recyclerCameras;
    private CameraAdapter adapter;
    private final List<CameraModel> listaCameras = new ArrayList<>();

    private ConstraintLayout layoutTelaCheia;
    private PlayerView playerViewFullscreen;
    private Button btnFecharFullscreen;
    private ExoPlayer playerFullscreen;

    private CardView cardAddCamera;
    private EditText editTipo, editIp, editUser, editPass, editPort, editPath;
    private FloatingActionButton fabAdd;
    private TextView textTituloForm;
    private Button btnSalvarCamera;

    private CameraModel cameraSendoEditada = null;
    private int posicaoSendoEditada = -1;

    private TextView textDataHoraFull, textCameraLabelFull;
    private ImageView btnMuteFull;
    private boolean isMuted = false;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    private static final String PREFS_NAME = "LordSeg_Cameras";
    private static final String CAMERAS_KEY = "camera_list";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cameras, container, false);

        // Vínculos da Grade
        recyclerCameras = view.findViewById(R.id.recycler_cameras);

        // Vínculos da Tela Cheia
        layoutTelaCheia = view.findViewById(R.id.layout_tela_cheia);
        playerViewFullscreen = view.findViewById(R.id.player_view_fullscreen);
        btnFecharFullscreen = view.findViewById(R.id.btn_fechar_fullscreen);

        // Vínculos Add Camera
        cardAddCamera = view.findViewById(R.id.card_add_camera);
        editTipo = view.findViewById(R.id.edit_camera_tipo);
        editIp = view.findViewById(R.id.edit_camera_ip);
        editUser = view.findViewById(R.id.edit_camera_user);
        editPass = view.findViewById(R.id.edit_camera_pass);
        editPort = view.findViewById(R.id.edit_camera_port);
        editPath = view.findViewById(R.id.edit_camera_path);
        
        fabAdd = view.findViewById(R.id.fab_add_camera);
        textTituloForm = view.findViewById(R.id.text_titulo_add_camera);
        btnSalvarCamera = view.findViewById(R.id.btn_salvar_camera);

        // Vínculos Overlay Fullscreen
        textDataHoraFull = view.findViewById(R.id.text_data_hora_full);
        textCameraLabelFull = view.findViewById(R.id.text_camera_label_full);
        btnMuteFull = view.findViewById(R.id.btn_mute_full);

        loadCameras();
        configurarRecyclerView();
        configurarListeners(view);

        return view;
    }

    private void configurarRecyclerView() {
        recyclerCameras.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new CameraAdapter(getContext(), listaCameras, this::abrirTelaCheia, this::confirmDeleteCamera);
        recyclerCameras.setAdapter(adapter);
    }

    private void configurarListeners(View view) {
        btnFecharFullscreen.setOnClickListener(v -> fecharTelaCheia());

        btnMuteFull.setOnClickListener(v -> toggleMute());
        
        fabAdd.setOnClickListener(v -> abrirFormulario(null, -1));

        btnSalvarCamera.setOnClickListener(v -> {
            String tipo = editTipo.getText().toString().trim();
            String ip = editIp.getText().toString().trim();
            String user = editUser.getText().toString().trim();
            String pass = editPass.getText().toString().trim();
            String port = editPort.getText().toString().trim();
            String path = editPath.getText().toString().trim();

            if (tipo.isEmpty() || ip.isEmpty() || user.isEmpty() || pass.isEmpty() || port.isEmpty() || path.isEmpty()) {
                Toast.makeText(getContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cameraSendoEditada != null) {
                // Modo Edição
                CameraModel editada = new CameraModel(tipo, cameraSendoEditada.getNumero(), ip, user, pass, port, path, path.replace("subtype=0", "subtype=1"));
                listaCameras.set(posicaoSendoEditada, editada);
                adapter.notifyItemChanged(posicaoSendoEditada);
                Toast.makeText(getContext(), "Câmera atualizada", Toast.LENGTH_SHORT).show();
            } else {
                // Modo Adição
                addCamera(tipo, ip, user, pass, port, path);
            }

            fecharFormulario();
        });

        view.findViewById(R.id.btn_cancelar_add).setOnClickListener(v -> fecharFormulario());
    }

    private void abrirFormulario(CameraModel camera, int position) {
        cameraSendoEditada = camera;
        posicaoSendoEditada = position;

        if (camera != null) {
            textTituloForm.setText("Editar Câmera");
            btnSalvarCamera.setText("Atualizar");
            editTipo.setText(camera.getNome());
            editIp.setText(camera.getIp());
            editUser.setText(camera.getUsuario());
            editPass.setText(camera.getSenha());
            editPort.setText(camera.getPorta());
            editPath.setText(camera.getPathMain());
        } else {
            textTituloForm.setText("Adicionar Nova Câmera");
            btnSalvarCamera.setText("Adicionar");
            // Mantém os padrões no formulário de adição
        }

        cardAddCamera.setVisibility(View.VISIBLE);
        cardAddCamera.bringToFront();
    }

    private void fecharFormulario() {
        cardAddCamera.setVisibility(View.GONE);
        editTipo.setText("");
        editIp.setText("");
        // Reseta para padrões
        editUser.setText("admin");
        editPass.setText("Lordseg2025");
        editPort.setText("554");
        editPath.setText("/cam/realmonitor?channel=1&subtype=0");
        
        cameraSendoEditada = null;
        posicaoSendoEditada = -1;
        saveCameras();
    }

    private void addCamera(String tipo, String ip, String user, String pass, String port, String path) {
        int nextIndex = listaCameras.size() + 1;
        String numero = String.format(Locale.getDefault(), "camera%02d", nextIndex);
        
        // Gera o path do sub-stream automaticamente (ajuste comum para câmeras chinesas)
        String subPath = path.contains("subtype=0") ? path.replace("subtype=0", "subtype=1") : path;
        
        CameraModel newCamera = new CameraModel(tipo, numero, ip, user, pass, port, path, subPath);
        listaCameras.add(newCamera);
        saveCameras();
        adapter.notifyItemInserted(listaCameras.size() - 1);
    }

    private void confirmDeleteCamera(CameraModel camera, int position) {
        String[] opcoes = {"Editar", "Excluir", "Cancelar"};
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Opções da " + camera.getExibicaoNome())
                .setItems(opcoes, (dialog, which) -> {
                    if (which == 0) {
                        abrirFormulario(camera, position);
                    } else if (which == 1) {
                        excluirCameraDefinitivamente(camera, position);
                    }
                })
                .show();
    }

    private void excluirCameraDefinitivamente(CameraModel camera, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Excluir Câmera")
                .setMessage("Deseja realmente excluir a " + camera.getExibicaoNome() + "?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    listaCameras.remove(position);
                    reorganizarNumeracao();
                    saveCameras();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Câmera removida", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void reorganizarNumeracao() {
        List<CameraModel> temp = new ArrayList<>(listaCameras);
        listaCameras.clear();
        for (int i = 0; i < temp.size(); i++) {
            CameraModel old = temp.get(i);
            String novoNumero = String.format(Locale.getDefault(), "camera%02d", i + 1);
            listaCameras.add(new CameraModel(old.getNome(), novoNumero, old.getIp(), old.getUsuario(), old.getSenha(), old.getPorta(), old.getPathMain(), old.getPathSub()));
        }
    }

    private void saveCameras() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(listaCameras);
        editor.putString(CAMERAS_KEY, json);
        editor.apply();
    }

    private void loadCameras() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(CAMERAS_KEY, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<CameraModel>>() {}.getType();
            List<CameraModel> savedList = gson.fromJson(json, type);
            if (savedList != null) {
                listaCameras.clear();
                // Migração/Correção de campos nulos caso venha de uma versão anterior
                for (CameraModel cam : savedList) {
                    if (cam.getUsuario() == null) {
                        // Se for uma câmera antiga, tenta manter os links originais mas preenche os campos novos com padrões
                        listaCameras.add(new CameraModel(
                            cam.getNome(), 
                            cam.getNumero() != null ? cam.getNumero() : "camera", 
                            cam.getIp() != null ? cam.getIp() : "0.0.0.0",
                            "admin", "Lordseg2025", "554", 
                            "/cam/realmonitor?channel=1&subtype=0", 
                            "/cam/realmonitor?channel=1&subtype=1"
                        ));
                    } else {
                        listaCameras.add(cam);
                    }
                }
            }
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (playerFullscreen != null) {
            playerFullscreen.setVolume(isMuted ? 0f : 1f);
        }
        btnMuteFull.setImageResource(isMuted ? android.R.drawable.ic_lock_silent_mode : android.R.drawable.ic_lock_silent_mode_off);
    }

    private void iniciarRelogio() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy EEE HH:mm:ss", Locale.getDefault());
                textDataHoraFull.setText(sdf.format(new Date()));
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void pararRelogio() {
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void abrirTelaCheia(CameraModel camera) {
        recyclerCameras.setVisibility(View.GONE);
        fabAdd.setVisibility(View.GONE);
        adapter.setGradePausada(true);

        layoutTelaCheia.setVisibility(View.VISIBLE);
        textCameraLabelFull.setText(camera.getExibicaoNome());
        iniciarRelogio();

        if (getContext() != null) {
            playerFullscreen = new ExoPlayer.Builder(requireContext()).build();
            playerViewFullscreen.setPlayer(playerFullscreen);
            playerFullscreen.setVolume(isMuted ? 0f : 1f);

            MediaItem mediaItem = MediaItem.fromUri(camera.getUrlMainStream());
            RtspMediaSource rtspSource = new RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .createMediaSource(mediaItem);

            playerFullscreen.setMediaSource(rtspSource);
            playerFullscreen.prepare();
            playerFullscreen.setPlayWhenReady(true);
        }
    }

    private void fecharTelaCheia() {
        pararRelogio();
        if (playerFullscreen != null) {
            playerFullscreen.release();
            playerFullscreen = null;
        }

        layoutTelaCheia.setVisibility(View.GONE);
        recyclerCameras.setVisibility(View.VISIBLE);
        fabAdd.setVisibility(View.VISIBLE);
        adapter.setGradePausada(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (playerFullscreen != null) {
            playerFullscreen.release();
        }
        if (adapter != null) {
            adapter.setGradePausada(true);
        }
    }
}