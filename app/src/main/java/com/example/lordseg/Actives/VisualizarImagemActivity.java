package com.example.lordseg.Actives;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.lordseg.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class VisualizarImagemActivity extends AppCompatActivity {

    private String imageUrl;
    private ImageView imageView;
    private MaterialToolbar toolbar;
    private FloatingActionButton fabDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizar_imagem);

        imageUrl = getIntent().getStringExtra("imageUrl");
        String senderName = getIntent().getStringExtra("senderName");
        long timeMillis = getIntent().getLongExtra("time", 0);

        imageView = findViewById(R.id.img_fullscreen);
        toolbar = findViewById(R.id.toolbar_visualizar);
        fabDownload = findViewById(R.id.fab_download);

        if (senderName != null && timeMillis != 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            String timeStr = sdf.format(new java.util.Date(timeMillis));
            toolbar.setTitle(senderName + " - " + timeStr);
        }

        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imageView);
        } else {
            Toast.makeText(this, "Erro ao carregar imagem.", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.btn_voltar_fullscreen).setOnClickListener(v -> finish());

        fabDownload.setOnClickListener(v -> baixarImagem());
    }

    private void baixarImagem() {
        if (imageUrl == null) return;

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Baixando Imagem");
            request.setDescription("Salvando imagem do chat...");
            
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            String fileName = "LordSeg_" + System.currentTimeMillis() + ".jpg";
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Download iniciado...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao baixar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}