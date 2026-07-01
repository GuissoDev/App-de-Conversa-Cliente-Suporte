package com.example.lordseg.Actives;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lordseg.R;
import com.google.android.material.appbar.MaterialToolbar;

public class VisualizarDocumentoActivity extends AppCompatActivity {

    private String docUrl, docName;
    private TextView textDocName;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizar_documento);

        docUrl = getIntent().getStringExtra("docUrl");
        docName = getIntent().getStringExtra("docName");

        textDocName = findViewById(R.id.text_doc_name);
        toolbar = findViewById(R.id.toolbar_visualizar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        if (docName != null) {
            textDocName.setText(docName);
        }

        findViewById(R.id.btn_voltar).setOnClickListener(v -> finish());
        findViewById(R.id.btn_download).setOnClickListener(v -> baixarDocumento());
    }

    private void baixarDocumento() {
        if (docUrl == null) {
            Toast.makeText(this, "URL do documento inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(docUrl));
            request.setTitle(docName != null ? docName : "Documento");
            request.setDescription("Baixando arquivo...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, docName != null ? docName : "documento_" + System.currentTimeMillis());

            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Download iniciado...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao iniciar download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            android.util.Log.e("DownloadError", "Erro: ", e);
        }
    }
}
