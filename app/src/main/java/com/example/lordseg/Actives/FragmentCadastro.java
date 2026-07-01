package com.example.lordseg.Actives;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
 * FragmentCadastro: Esta tela permite que novos usuários criem uma conta.
 * Utiliza exclusivamente a API LordSeg (MySQL).
 */
public class FragmentCadastro extends AppCompatActivity {

    private TextView btn_back;
    private EditText edit_nome, edit_email, edit_senha, edit_telefone;
    private Button btn_cadastrar;

    // Mensagens informativas para o usuário
    String[] mensagens = {"Preencha todos os campos!", "Cadastro realizado com sucesso!", "Erro ao cadastrar!",
                           "A senha precisa ter no mínimo 6 caracteres!", "Digite um telefone válido!"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fragment_cadastro);

        // Configura o preenchimento para respeitar as barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tela_cadastro), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        IniciarComponents();

        // Clique no botão "Voltar" para retornar à tela de Login
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FragmentCadastro.this, FragmentLogin.class);
                startActivity(intent);
            }
        });

        // Clique no botão de Cadastrar
        btn_cadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nome = edit_nome.getText().toString();
                String email = edit_email.getText().toString();
                String senha = edit_senha.getText().toString();
                String telefone = edit_telefone.getText().toString();

                // Validações básicas de campos vazios e tamanho de telefone
                if(nome.isEmpty() || email.isEmpty() || senha.isEmpty() || telefone.isEmpty()){
                   Snackbar snackbar = Snackbar.make(v, mensagens[0], Snackbar.LENGTH_SHORT);
                   snackbar.setBackgroundTint(Color.WHITE);
                   snackbar.setTextColor(Color.BLACK);
                   snackbar.show();
                }else if (telefone.length() < 11) {
                    Snackbar snackbar = Snackbar.make(v, mensagens[4], Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(Color.WHITE);
                    snackbar.setTextColor(Color.BLACK);
                    snackbar.show();
                }else{
                    CadastrarUsuarioAPI(v);
                }
            }
        });
    }

    /**
     * Tenta criar o usuário na API (MySQL).
     */
    private void CadastrarUsuarioAPI(View v) {
        String nome = edit_nome.getText().toString();
        String email = edit_email.getText().toString();
        String senha = edit_senha.getText().toString();
        String telefone = edit_telefone.getText().toString();

        RegisterRequest request = new RegisterRequest();
        request.name = nome;
        request.email = email;
        request.phone = telefone;
        request.password = senha;
        // Usa o e-mail como username base
        request.username = email.split("@")[0] + "_" + (int)(Math.random() * 1000);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.register(request).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("API_AUTH", "Sucesso ao cadastrar na API MySQL: " + response.body());
                    Snackbar snackbar = Snackbar.make(v, mensagens[1], Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(Color.GREEN);
                    snackbar.setTextColor(Color.WHITE);
                    snackbar.show();
                    
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(FragmentCadastro.this, FragmentLogin.class);
                        startActivity(intent);
                        finish();
                    }, 1500);
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e("API_AUTH", "Erro do Servidor (" + response.code() + "): " + errorBody);
                    Snackbar snackbar = Snackbar.make(v, "Erro no servidor: " + response.code(), Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e("API_AUTH", "FALHA TOTAL DE CONEXÃO: " + t.getMessage(), t);
                Snackbar snackbar = Snackbar.make(v, "Falha de conexão: " + t.getMessage(), Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        });
    }

    /**
     * Inicializa os componentes do layout.
     */
    private void IniciarComponents(){
        btn_back = findViewById(R.id.btn_back);
        edit_nome = findViewById(R.id.edit_nome);
        edit_email = findViewById(R.id.edit_email);
        edit_senha = findViewById(R.id.edit_senha);
        edit_telefone = findViewById(R.id.edit_telefone);
        btn_cadastrar = findViewById(R.id.btn_cadastrar);
    }
}
