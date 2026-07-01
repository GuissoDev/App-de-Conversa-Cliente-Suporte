package com.example.lordseg.Actives;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lordseg.Adaptadores.ChatAdapter;
import com.example.lordseg.Model.ConversationModel;
import com.example.lordseg.Model.MessageModel;
import com.example.lordseg.Model.MessageRequest;
import com.example.lordseg.Model.UserResponse;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

/**
 * ChatActivity: Tela de chat que utiliza a API LordSeg (MySQL).
 * Histórico via REST, Tempo Real via STOMP (WebSocket) e Upload de Mídias via Multipart REST.
 */
public class ChatActivity extends AppCompatActivity {

    private EditText edit_mensagem;
    private ImageView btn_send;
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private final List<MessageModel> messageList = new ArrayList<>();

    private String otherUserId;
    private String otherUserName;
    private String token;
    private Long myId;
    private Long currentConversationId;
    private StompClient mStompClient;
    public static boolean isActivityVisible = false;

    private Uri imageUri;
    private Uri documentUri;
    private String documentName;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> documentLauncher;

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        Log.i("APP_VERSION", "Iniciando ChatActivity - Build: " + System.currentTimeMillis());

        IniciarLaunchers();

        otherUserName = getIntent().getStringExtra("nome");
        otherUserId = getIntent().getStringExtra("uid");
        token = getIntent().getStringExtra("token");

        IniciarComponents();

        if (token != null) {
            initStomp();
            loadMyIdAndHistory();
        } else {
            Toast.makeText(this, "Erro de autenticação", Toast.LENGTH_SHORT).show();
            finish();
        }

        btn_send.setOnClickListener(v -> {
            String message = edit_mensagem.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageViaStomp(message);
            }
        });
    }

    private void IniciarLaunchers() {
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result) confirmAndSendImage();
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                imageUri = uri;
                confirmAndSendImage();
            }
        });

        documentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                documentUri = uri;
                documentName = "Documento";
                try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) documentName = cursor.getString(nameIndex);
                    }
                }
                confirmAndSendDocument();
            }
        });
    }

    private void IniciarComponents() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        edit_mensagem = findViewById(R.id.edit_mensagem);
        btn_send = findViewById(R.id.btn_send);
        recyclerView = findViewById(R.id.message_list);

        if (otherUserName != null) toolbar.setTitle(otherUserName);
        toolbar.setNavigationOnClickListener(v -> finish());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ChatAdapter(messageList, "meu_id_placeholder");
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btn_camera).setOnClickListener(v -> mostrarDialogoSelecaoImagem());
        findViewById(R.id.btn_add_document).setOnClickListener(v -> documentLauncher.launch("*/*"));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));

            if (ime.bottom > 0 && !messageList.isEmpty()) {
                recyclerView.postDelayed(() -> {
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                }, 100);
            }
            return insets;
        });
    }

    private void loadMyIdAndHistory() {
        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        service.getMe("Bearer " + token).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    myId = response.body().id;
                    adapter.setCurrentUserId(String.valueOf(myId));
                    obterOuCriarConversaOficial();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                loadHistory();
            }
        });
    }

    private void obterOuCriarConversaOficial() {
        if (otherUserId == null || myId == null) return;

        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        Map<String, Long> request = new HashMap<>();
        request.put("userId1", myId);
        request.put("userId2", Long.parseLong(otherUserId));

        service.createOrGetConversation(request, "Bearer " + token).enqueue(new Callback<ConversationModel>() {
            @Override
            public void onResponse(@NonNull Call<ConversationModel> call, @NonNull Response<ConversationModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentConversationId = response.body().id;
                    Log.d("CHAT", "Conversa oficial ID: " + currentConversationId);

                    subscribeToConversationTopic(currentConversationId);
                    loadHistory();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConversationModel> call, @NonNull Throwable t) {
                Log.e("CHAT", "Erro ao obter conversa oficial");
                loadHistory();
            }
        });
    }

    private void loadHistory() {
        if (otherUserId == null) return;

        long idParaCarregar;
        if (currentConversationId != null) {
            idParaCarregar = currentConversationId;
        } else {
            try {
                idParaCarregar = Long.parseLong(otherUserId);
            } catch (Exception e) {
                idParaCarregar = 1L;
            }
        }

        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        service.getMessages(idParaCarregar, "Bearer " + token).enqueue(new Callback<List<MessageModel>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<List<MessageModel>> call, @NonNull Response<List<MessageModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messageList.clear();
                    messageList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }

                    // sendReadReceipt(); // REMOVIDO para estabilidade absoluta
                } else {
                    Log.e("CHAT", "Erro ao carregar mensagens: " + response.code());
                    // Tratamento resiliente: se o JSON vier quebrado mas tiver dados parciais
                    if (response.code() == 400) {
                        Log.w("CHAT", "Detectado erro de serialização no backend. Verifique a configuração do Hibernate.");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MessageModel>> call, @NonNull Throwable t) {
                Toast.makeText(ChatActivity.this, "Erro ao carregar histórico", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initStomp() {
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://lordseg-api-env.eba-agw2yuke.sa-east-1.elasticbeanstalk.com/ws/chat");

        mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            Log.d("STOMP", "Opened");
                            // runOnUiThread(this::sendReadReceipt); // Desativado para estabilidade
                            break;
                        case ERROR:
                            Log.e("STOMP", "Error", lifecycleEvent.getException());
                            // Nova política de reconexão resiliente após 5 segundos
                            recyclerView.postDelayed(() -> {
                                if (mStompClient != null && !mStompClient.isConnected()) {
                                    Log.d("STOMP", "Tentando reconectar...");
                                    mStompClient.connect();
                                }
                            }, 5000);
                            break;
                        case CLOSED: Log.d("STOMP", "Closed"); break;
                    }
                }, throwable -> {
                    Log.e("STOMP", "Erro no ciclo de vida do WebSocket", throwable);
                });

        mStompClient.topic("/topic/messages")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    Log.d("STOMP", "Nova mensagem recebida no tópico global: " + topicMessage.getPayload());
                    runOnUiThread(this::loadHistory);
                }, throwable -> {
                    Log.e("STOMP", "Erro ao assinar tópico de mensagens", throwable);
                });

        mStompClient.connect();
    }

    @SuppressLint("CheckResult")
    private void sendMessageViaStomp(String message) {
        if (currentConversationId == null || myId == null) {
            Log.w("CHAT", "Dados insuficientes para enviar. Tentando obter id...");
            sendMessageViaRest(message);
            return;
        }

        // Criar o objeto JSON para o servidor
        String jsonMsg = "{\"senderId\":" + myId +
                         ",\"conversationId\":" + currentConversationId +
                         ",\"content\":\"" + message + "\"}";

        if (mStompClient != null && mStompClient.isConnected()) {
            Log.d("STOMP", "Enviando via WebSocket: " + jsonMsg);
            mStompClient.send("/app/chat", jsonMsg)
                    .compose(t -> t.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()))
                    .subscribe(() -> {
                        edit_mensagem.setText("");
                        // Força um reload manual rápido caso o servidor não broadcast
                        new android.os.Handler().postDelayed(this::loadHistory, 500);
                    }, throwable -> {
                        Log.e("STOMP", "Erro ao enviar via WebSocket, tentando API REST", throwable);
                        sendMessageViaRest(message);
                    });
        } else {
            Log.w("STOMP", "WebSocket desconectado, enviando via API REST");
            sendMessageViaRest(message);
        }
    }

    private void sendMessageViaRest(String message) {
        if (otherUserId == null) return;

        MessageModel localMsg = new MessageModel();
        localMsg.content = message;
        localMsg.senderId = myId;
        localMsg.timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        messageList.add(localMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        MessageRequest request = new MessageRequest();

        if (currentConversationId != null) {
            request.conversationId = currentConversationId;
        } else {
            try {
                request.conversationId = Long.parseLong(otherUserId);
            } catch (Exception e) {
                request.conversationId = 1L;
            }
        }

        request.senderId = myId;
        request.content = message;

        service.sendMessage(request, "Bearer " + token).enqueue(new Callback<MessageModel>() {
            @Override
            public void onResponse(@NonNull Call<MessageModel> call, @NonNull Response<MessageModel> response) {
                if (response.isSuccessful()) {
                    edit_mensagem.setText("");
                } else {
                    if (!messageList.isEmpty()) {
                        messageList.remove(messageList.size() - 1);
                        adapter.notifyItemRemoved(messageList.size());
                    }
                    Toast.makeText(ChatActivity.this, "Erro ao salvar no servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageModel> call, @NonNull Throwable t) {
                Toast.makeText(ChatActivity.this, "Falha de conexão ao enviar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * BLOQUEADO: sendReadReceipt chamado mas ignorado propositalmente para estabilidade.
     */
    private void sendReadReceipt() {
        Log.i("ULTRA_STABILITY_FIX", "BLOQUEADO: sendReadReceipt chamado mas ignorado propositalmente.");
    }

    /**
     * Assina o canal exclusivo da conversa para receber atualizações de leitura/status em tempo real.
     */
    @SuppressLint("CheckResult")
    private void subscribeToConversationTopic(Long conversationId) {
        if (mStompClient == null || conversationId == null) return;

        mStompClient.topic("/topic/chat." + conversationId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    String payload = topicMessage.getPayload();
                    Log.d("ULTRA_STABILITY_FIX", "Payload recebido: " + payload);

                    // Apenas atualiza a UI se for uma mensagem nova (não leitura) para evitar loops infinitos
                    if (payload.contains("\"content\"") || payload.contains("[IMAGEM]")) {
                        runOnUiThread(this::loadHistory);
                    }
                }, throwable -> {
                    Log.e("STOMP", "Erro ao assinar canal de leitura da conversa " + conversationId, throwable);
                });
    }

    private void mostrarDialogoSelecaoImagem() {
        String[] options = {"Câmera", "Galeria"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Enviar Foto")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera(); else galleryLauncher.launch("image/*");
                }).show();
    }

    private void openCamera() {
        try {
            File photoFile = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "temp_image.jpg");
            imageUri = FileProvider.getUriForFile(this, "com.example.lordseg.fileprovider", photoFile);
            takePictureLauncher.launch(imageUri);
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir câmera", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ATUALIZADO: Converte a imagem capturada em binário, comprime e faz o upload Multipart.
     */
    private void confirmAndSendImage() {
        if (imageUri == null || token == null) {
            Log.e("UPLOAD_IMAGE", "URI ou Token nulo. Cancelando upload.");
            return;
        }

        try {
            Log.d("UPLOAD_IMAGE", "Iniciando processamento da URI: " + imageUri);
            
            // 1. Carrega a imagem e comprime para evitar Erro 413 (Payload Too Large)
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) {
                Log.e("UPLOAD_IMAGE", "Não foi possível decodificar o Bitmap da imagem.");
                return;
            }

            File file = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            
            // Redimensiona se for muito grande (opcional, mas recomendado)
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float maxDimension = 1280;
            if (width > maxDimension || height > maxDimension) {
                float scale = maxDimension / Math.max(width, height);
                bitmap = Bitmap.createScaledBitmap(bitmap, (int)(width * scale), (int)(height * scale), true);
            }

            // Comprime para JPEG com 70% de qualidade
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            outputStream.flush();
            outputStream.close();

            Log.d("UPLOAD_IMAGE", "Arquivo comprimido criado: " + file.getAbsolutePath() + " (Tamanho: " + file.length() / 1024 + " KB)");

            if (file.length() == 0) {
                Log.e("UPLOAD_IMAGE", "Arquivo criado está vazio. Abortando.");
                return;
            }

            // 2. Encapsula o arquivo no formato Multipart do OkHttp
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            // 3. Faz a chamada HTTP assíncrona usando o Retrofit
            ApiService service = RetrofitClient.getClient().create(ApiService.class);
            Log.d("UPLOAD_IMAGE", "Iniciando upload para o servidor...");

            service.uploadImage(body, "Bearer " + token).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("UPLOAD_IMAGE", "Upload concluído com sucesso!");
                        // 4. Se o upload der certo, pega a URL da foto salva no servidor
                        String imageUrlFromServer = response.body().get("url");

                        // 5. Envia a URL como tag de texto pelo WebSocket seguro
                        sendMessageViaStomp("[IMAGEM]: " + imageUrlFromServer);
                    } else {
                        Log.e("UPLOAD_IMAGE", "Erro no servidor: " + response.code());
                        try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                            if (errorBody != null) {
                                Log.e("UPLOAD_IMAGE", "Detalhe do erro: " + errorBody.string());
                            }
                        } catch (Exception ignored) {}
                        Toast.makeText(ChatActivity.this, "Falha ao enviar imagem: Erro " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    Log.e("UPLOAD_IMAGE", "Falha total na conexão de upload", t);
                    Toast.makeText(ChatActivity.this, "Erro de rede no upload da foto", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("UPLOAD_IMAGE", "Erro ao processar imagem para upload", e);
            Toast.makeText(this, "Erro ao processar imagem", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Implementação Real: Converte o documento em binário e faz o upload Multipart.
     */
    private void confirmAndSendDocument() {
        if (documentUri == null || token == null) return;

        try {
            // 1. Abre o arquivo selecionado e cria uma cópia temporária no cache do app
            InputStream inputStream = getContentResolver().openInputStream(documentUri);
            File file = new File(getCacheDir(), documentName != null ? documentName : "documento_anexo.pdf");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            // 2. Monta a requisição Multipart com o tipo MIME correto do documento
            String mimeType = getContentResolver().getType(documentUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType != null ? mimeType : "*/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            // 3. Envia o arquivo para a mesma rota do Spring Boot (ela aceita qualquer arquivo!)
            ApiService service = RetrofitClient.getClient().create(ApiService.class);
            service.uploadImage(body, "Bearer " + token).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // 4. Se o upload no S3 funcionar, pegamos a URL estável do arquivo
                        String docUrlFromServer = response.body().get("url");

                        // 5. Enviamos com o prefixo [DOCUMENTO]: pelo WebSocket
                        sendMessageViaStomp("[DOCUMENTO]: " + docUrlFromServer);
                    } else {
                        Toast.makeText(ChatActivity.this, "Falha ao enviar documento para o servidor", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    Toast.makeText(ChatActivity.this, "Erro de rede no upload do documento", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("UPLOAD_DOCUMENT", "Erro ao processar documento para upload", e);
            Toast.makeText(this, "Erro ao processar arquivo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (mStompClient != null) mStompClient.disconnect();
        super.onDestroy();
    }
}