package com.example.lordseg.Actives;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lordseg.Model.RegisterRequest;
import com.example.lordseg.Model.UserResponse;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.R;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AdicionarUsuarioADM: Tela para o administrador cadastrar novos usuários no MySQL.
 */
public class AdicionarUsuarioADM extends AppCompatActivity {

    private EditText editNome, editEmail, editSenha, editTelefone;
    private Button btnCadastrar;
    private TextView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fragment_cadastro); // Reutilizando layout de cadastro

        IniciarComponents();

        btnBack.setOnClickListener(v -> finish());

        btnCadastrar.setOnClickListener(v -> {
            String nome = editNome.getText().toString();
            String email = editEmail.getText().toString();
            String senha = editSenha.getText().toString();
            String telefone = editTelefone.getText().toString();

            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || telefone.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            } else {
                cadastrarNovoUsuario(nome, email, senha, telefone);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tela_cadastro), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void IniciarComponents() {
        editNome = findViewById(R.id.edit_nome);
        editEmail = findViewById(R.id.edit_email);
        editSenha = findViewById(R.id.edit_senha);
        editTelefone = findViewById(R.id.edit_telefone);
        btnCadastrar = findViewById(R.id.btn_cadastrar);
        btnBack = findViewById(R.id.btn_back);
    }

    private void cadastrarNovoUsuario(String nome, String email, String senha, String telefone) {
        RegisterRequest request = new RegisterRequest();
        request.name = nome;
        request.email = email;
        request.password = senha;
        request.phone = telefone;
        request.username = email.split("@")[0] + "_" + (int)(Math.random() * 1000);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.register(request).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdicionarUsuarioADM.this, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish(); // Volta para a lista ADM
                } else {
                    Toast.makeText(AdicionarUsuarioADM.this, "Erro ao cadastrar: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Toast.makeText(AdicionarUsuarioADM.this, "Falha de conexão com a API", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
