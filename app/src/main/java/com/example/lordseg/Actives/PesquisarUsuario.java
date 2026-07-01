package com.example.lordseg.Actives;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lordseg.Adaptadores.AdaptadorConversasRecentes;
import com.example.lordseg.Model.UserResponse;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.Network.SessionManager;
import com.example.lordseg.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PesquisarUsuario: Tela para buscar usuários cadastrados no MySQL para iniciar um chat.
 * Regras:
 * 1. Lista só aparece após clicar em pesquisar.
 * 2. LordSeg pesquisa todos (exceto ele mesmo).
 * 3. Outros usuários só pesquisam o LordSeg.
 */
public class PesquisarUsuario extends AppCompatActivity {

    private EditText editPesquisa;
    private ImageView btnPesquisar;
    private RecyclerView recyclerView;
    private AdaptadorConversasRecentes adapter;
    private List<UserResponse> fullUserList = new ArrayList<>();
    private List<UserResponse> filteredList = new ArrayList<>();
    private String token;
    private MaterialToolbar toolbar;
    private boolean isLordSeg = false;
    private Long myId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pesquisar_usuario);

        SessionManager sessionManager = new SessionManager(this);
        token = sessionManager.getToken();

        IniciarComponents();
        setupRecyclerView();
        
        // Primeiro identifica quem é o usuário logado
        identificarUsuarioLogado();

        btnPesquisar.setOnClickListener(v -> {
            String query = editPesquisa.getText().toString().trim();
            if (!query.isEmpty()) {
                realizarPesquisa(query);
            } else {
                Toast.makeText(this, "Digite algo para pesquisar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void IniciarComponents() {
        editPesquisa = findViewById(R.id.edit_pesquisa);
        btnPesquisar = findViewById(R.id.btn_pesquisar);
        recyclerView = findViewById(R.id.recycler_usuarios);
        toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        adapter = new AdaptadorConversasRecentes(filteredList, token);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        // Inicialmente escondido
        recyclerView.setVisibility(View.GONE);
    }

    private void identificarUsuarioLogado() {
        if (token == null) return;
        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        service.getMe("Bearer " + token).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    myId = response.body().id;
                    String nome = response.body().name;
                    isLordSeg = (nome != null && "LordSeg".equalsIgnoreCase(nome.trim()));
                }
            }
            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e("PESQUISA", "Erro ao identificar usuário");
            }
        });
    }

    private void realizarPesquisa(String query) {
        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        service.getAllUsers("Bearer " + token).enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserResponse>> call, @NonNull Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullUserList.clear();
                    String searchQuery = query.toLowerCase();

                    for (UserResponse user : response.body()) {
                        String userName = (user.name != null) ? user.name.trim() : "";
                        String userEmail = (user.email != null) ? user.email.trim() : "";

                        // Regra 1: Filtrar por texto da pesquisa
                        boolean matchesQuery = userName.toLowerCase().contains(searchQuery) || 
                                              userEmail.toLowerCase().contains(searchQuery);
                        
                        if (!matchesQuery) continue;

                        // Regra 2: LordSeg vê todos exceto ele mesmo
                        if (isLordSeg) {
                            if (!user.id.equals(myId)) {
                                fullUserList.add(user);
                            }
                        } 
                        // Regra 3: Outros usuários só veem o LordSeg
                        else {
                            if ("LordSeg".equalsIgnoreCase(userName)) {
                                fullUserList.add(user);
                            }
                        }
                    }

                    filteredList.clear();
                    filteredList.addAll(fullUserList);
                    adapter.notifyDataSetChanged();
                    recyclerView.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
                    
                    if (filteredList.isEmpty()) {
                        Toast.makeText(PesquisarUsuario.this, "Nenhum usuário encontrado", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserResponse>> call, @NonNull Throwable t) {
                Log.e("PESQUISA", "Falha na API: " + t.getMessage());
            }
        });
    }
}