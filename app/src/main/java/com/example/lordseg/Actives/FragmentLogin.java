package com.example.lordseg.Actives;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lordseg.Model.AuthResponse;
import com.example.lordseg.Model.LoginRequest;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.Network.SessionManager;
import com.example.lordseg.R;
import com.example.lordseg.databinding.ActivityFragmentLoginBinding;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FragmentLogin: Esta tela permite que o usuário realize o login utilizando e-mail e senha.
 * Utiliza exclusivamente a API LordSeg (MySQL).
 */
public class FragmentLogin extends AppCompatActivity {

    private ActivityFragmentLoginBinding binding;

    // Lista de mensagens de retorno para o usuário
    String[] mensagens = {"Preencha todos os campos!", "Login realizado com sucesso!", "Erro ao Logar!",
            "A senha precisa ter no mínimo 6 caracteres!"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        binding = ActivityFragmentLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Ajusta o layout para não ficar embaixo das barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.telaLogin, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Clique para ir para a tela de cadastro caso o usuário não tenha conta
        binding.textTelaCadastro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FragmentLogin.this, FragmentCadastro.class);
                startActivity(intent);
            }
        });

        // Clique no botão de Entrar
        binding.btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.editEmail.getText().toString();
                String senha = binding.editSenha.getText().toString();

                // Valida se os campos não estão vazios antes de tentar autenticar
                if(email.isEmpty() || senha.isEmpty()){
                    Snackbar snackbar = Snackbar.make(v, mensagens[0], Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(Color.WHITE);
                    snackbar.setTextColor(Color.BLACK);
                    snackbar.show();
                }else{
                    binding.btnEntrar.setEnabled(false); // Desativa o botão para evitar múltiplos cliques
                    binding.progressbar.setVisibility(View.VISIBLE); // Mostra o carregamento na hora
                    LogarNaApiMySQL(v);
                }
            }
        });
    }

    /**
     * Realiza o login na API LordSeg para obter o token JWT.
     */
    private void LogarNaApiMySQL(View v) {
        String email = binding.editEmail.getText().toString();
        String senha = binding.editSenha.getText().toString();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.usernameOrEmail = email;
        loginRequest.password = senha;

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.login(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                binding.btnEntrar.setEnabled(true);
                binding.progressbar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().token;
                    
                    // Salva o token no dispositivo para não precisar logar de novo
                    SessionManager sessionManager = new SessionManager(FragmentLogin.this);
                    sessionManager.saveToken(token);

                    // Se o login for sucesso, mostra a barra de progresso e aguarda antes de mudar de tela
                    binding.progressbar.setVisibility(View.VISIBLE);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            TelaPrincipal(token);
                        }
                    }, 1000);
                } else {
                    Snackbar snackbar = Snackbar.make(v, "Erro: " + response.code() + " - E-mail ou senha incorretos", Snackbar.LENGTH_LONG);
                    snackbar.setBackgroundTint(Color.RED);
                    snackbar.setTextColor(Color.WHITE);
                    snackbar.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                binding.btnEntrar.setEnabled(true);
                binding.progressbar.setVisibility(View.GONE);
                
                Log.e("API_AUTH", "Falha na API MySQL: " + t.getMessage());
                Snackbar snackbar = Snackbar.make(v, "Servidor offline ou sem internet. Tente reiniciar o servidor na AWS.", Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(Color.RED);
                snackbar.setTextColor(Color.WHITE);
                snackbar.show();
            }
        });
    }

    /**
     * Redireciona o usuário para a MainActivity após o login com sucesso.
     */
    private void TelaPrincipal(String token){
        Intent intent = new Intent(FragmentLogin.this, MainActivity.class);
        intent.putExtra("token", token);
        startActivity(intent);
        finish();
    }
}
