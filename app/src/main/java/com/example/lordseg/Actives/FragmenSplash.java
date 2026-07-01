package com.example.lordseg.Actives;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.lordseg.Network.SessionManager;
import com.example.lordseg.R;

/**
 * FragmenSplash: Tela de abertura (Splash Screen).
 * Verifica se o usuário já está logado para pular a tela de login.
 */
public class FragmenSplash extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_splash);

        // Aguarda 1.5 segundos e verifica a sessão
        new Handler().postDelayed(this::verificarSessao, 1500);
    }

    private void verificarSessao() {
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            // Se já tem token salvo, vai direto para a tela principal
            Intent intent = new Intent(FragmenSplash.this, MainActivity.class);
            intent.putExtra("token", sessionManager.getToken());
            startActivity(intent);
        } else {
            // Se não tem token, vai para a tela de login
            Intent intent = new Intent(FragmenSplash.this, FragmentLogin.class);
            startActivity(intent);
        }
        finish();
    }
}
