package com.example.lordseg.Actives;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.lordseg.Fragment.ChatFragment;
import com.example.lordseg.Fragment.FragmentCameras;
import com.example.lordseg.Fragment.TelaPerfil;
import com.example.lordseg.Fragment.ADMFragment;
import com.example.lordseg.Model.UserResponse;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.Network.SessionManager;
import com.example.lordseg.R;
import com.google.android.material.navigation.NavigationBarView;
import com.example.lordseg.databinding.ActivityMainBinding;

import org.json.JSONObject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

/**
 * MainActivity: Tela principal do ecossistema LordSeg.
 * Gerencia a navegação e o ciclo de vida dos módulos (Chat, Câmeras, Perfil).
 */
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    public String token;
    public static boolean isAppInForeground = false;
    private StompClient mStompClient;
    private static final String CHANNEL_ID = "CHAT_NOTIFICATIONS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionManager sessionManager = new SessionManager(this);
        token = getIntent().getStringExtra("token");
        if (token == null) {
            token = sessionManager.getToken();
        }

        // 1. AJUSTE: systemBars.bottom garante que o menu não fique por baixo dos botões do Android
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        verificarAcessoADM();
        requestNotificationPermission();
        createNotificationChannel();

        if (token != null) {
            initGlobalWebSocket();
        }

        if(savedInstanceState == null){
            replaceFragment(new ChatFragment());
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.adm) {
                replaceFragment(new ADMFragment());
            } else if (id == R.id.chat) {
                replaceFragment(new ChatFragment());
            } else if (id == R.id.cameras) {
                replaceFragment(new FragmentCameras());
            } else if (id == R.id.perfil) {
                replaceFragment(new TelaPerfil());
            }

            if (binding.toolbar != null) {
                android.view.MenuItem searchItem = binding.toolbar.getMenu().findItem(R.id.pesquisa);
                if (searchItem != null) {
                    searchItem.setVisible(id == R.id.chat);
                }
            }
            return true;
        });

        if (binding.toolbar != null) {
            binding.toolbar.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.pesquisa) {
                    startActivity(new Intent(MainActivity.this, PesquisarUsuario.class));
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isAppInForeground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isAppInForeground = false;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.CAMERA
            };

            boolean allGranted = true;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, 101);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 102);
            }
        }
    }

    private void verificarAcessoADM() {
        if (token == null) return;

        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        service.getMe("Bearer " + token).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                // 2. AJUSTE: Gatekeeper de Segurança. Evita NullPointerException se a tela já foi fechada
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String nome = response.body().name;
                    if (nome != null && "LordSeg".equalsIgnoreCase(nome.trim())) {
                        runOnUiThread(() -> {
                            binding.bottomNavigation.getMenu().findItem(R.id.adm).setVisible(true);
                        });
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                // Falha silenciosa: O painel ADM simplesmente não aparece se a API cair.
            }
        });
    }

    private void replaceFragment(Fragment fragment) {
        // 3. AJUSTE: Proteção extra e transição fluida nativa para não "cortar" o vídeo secamente
        if (isFinishing() || isDestroyed()) return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Adiciona um Fade nativo e leve ao trocar de telas
        fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        fragmentTransaction.replace(R.id.fragment_layout, fragment);
        fragmentTransaction.commit();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Mensagens LordSeg";
            String description = "Notificações de novas mensagens no chat";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("CheckResult")
    private void initGlobalWebSocket() {
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://lordseg-api-env.eba-agw2yuke.sa-east-1.elasticbeanstalk.com/ws/chat");
        
        mStompClient.topic("/topic/messages")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    try {
                        JSONObject json = new JSONObject(topicMessage.getPayload());
                        String content = json.getString("content");
                        long senderId = json.getLong("senderId");

                        // Se a mensagem não for minha, mostra notificação
                        // (Idealmente comparar com o MEU ID, mas como fallback mostramos se ChatActivity não estiver aberta)
                        if (!ChatActivity.isActivityVisible) {
                            showNotification("Nova Mensagem", content);
                        }
                    } catch (Exception e) {
                        Log.e("WS_GLOBAL", "Erro ao processar mensagem", e);
                    }
                }, throwable -> Log.e("WS_GLOBAL", "Erro no WebSocket", throwable));

        mStompClient.connect();
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    protected void onDestroy() {
        if (mStompClient != null) mStompClient.disconnect();
        super.onDestroy();
    }
}