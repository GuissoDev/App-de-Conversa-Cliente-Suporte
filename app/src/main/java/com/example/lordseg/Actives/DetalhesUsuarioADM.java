package com.example.lordseg.Actives;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.Network.SessionManager;
import com.example.lordseg.R;
import com.google.android.material.appbar.MaterialToolbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DetalhesUsuarioADM: Exibe informações detalhadas de um usuário selecionado pelo administrador.
 * Agora com funcionalidade de exclusão via API MySQL funcional.
 */
public class DetalhesUsuarioADM extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView textNomeHeader, textNome, textEmail, textPhone;
    private ImageView imgPerfil;
    private AppCompatButton btnExcluir;
    private String token;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalhes_usuario_adm);

        SessionManager sessionManager = new SessionManager(this);
        token = sessionManager.getToken();

        IniciarComponents();

        // Recebe os dados passados pelo adaptador
        String nome = getIntent().getStringExtra("nome");
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");
        userId = getIntent().getStringExtra("uid");

        // Preenche a tela
        if (nome != null) {
            textNomeHeader.setText(nome);
            textNome.setText(nome);
            toolbar.setTitle("Detalhes de " + nome);
        }
        if (email != null) textEmail.setText(email);
        if (phone != null) textPhone.setText(phone);

        toolbar.setNavigationOnClickListener(v -> finish());

        btnExcluir.setOnClickListener(v -> {
            confirmarExclusao();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container_user_adm), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void IniciarComponents() {
        toolbar = findViewById(R.id.toolbar_detalhes);
        textNomeHeader = findViewById(R.id.text_user_header);
        textNome = findViewById(R.id.text_user);
        textEmail = findViewById(R.id.text_EmailUser);
        textPhone = findViewById(R.id.text_PhoneUser);
        imgPerfil = findViewById(R.id.img_perfil_adm);
        btnExcluir = findViewById(R.id.btn_excluir);
    }

    private void confirmarExclusao() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir permanentemente este usuário do sistema?")
                .setPositiveButton("Excluir", (dialog, which) -> excluirUsuario())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirUsuario() {
        if (userId == null || token == null) {
            Toast.makeText(this, "Erro: Dados insuficientes", Toast.LENGTH_SHORT).show();
            return;
        }

        long idLong = Long.parseLong(userId);
        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        
        service.deleteUser(idLong, "Bearer " + token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DetalhesUsuarioADM.this, "Usuário excluído com sucesso!", Toast.LENGTH_SHORT).show();
                    finish(); // Fecha a tela e volta para a lista
                } else {
                    Log.e("API_ADM", "Erro ao excluir: " + response.code());
                    Toast.makeText(DetalhesUsuarioADM.this, "Erro ao excluir: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("API_ADM", "Falha de conexão ao excluir: " + t.getMessage());
                Toast.makeText(DetalhesUsuarioADM.this, "Falha de conexão com o servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
