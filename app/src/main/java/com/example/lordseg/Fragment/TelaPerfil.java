package com.example.lordseg.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.lordseg.Actives.FragmentLogin;
import com.example.lordseg.Model.UserResponse;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.Network.SessionManager;
import com.example.lordseg.R;
import com.example.lordseg.Actives.MainActivity;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TelaPerfil: Exibe os dados do perfil do usuário logado.
 * Refatorada para utilizar a API LordSeg (MySQL).
 */
public class TelaPerfil extends Fragment {

    private TextView nomeUsuario, emailUsuario, telefoneUsuario, headerNome;
    private Button btnDeslogar;
    private ImageView imgPerfil, btnAlterarFoto;

    private String token;
    private Uri imageUri;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> uCropLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_tela_perfil, container, false);

        IniciarLaunchers();
        IniciarComponents(view);
        loadUserProfile();

        btnAlterarFoto.setOnClickListener(v -> mostrarDialogoSelecaoImagem());

        btnDeslogar.setOnClickListener(v -> {
            // Limpa a sessão ao sair
            SessionManager sessionManager = new SessionManager(getContext());
            sessionManager.logout();

            Intent intent = new Intent(getActivity(), FragmentLogin.class);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        return view;
    }

    private void IniciarComponents(View view) {
        nomeUsuario = view.findViewById(R.id.text_user);
        emailUsuario = view.findViewById(R.id.text_EmailUser);
        telefoneUsuario = view.findViewById(R.id.text_PhoneUser);
        headerNome = view.findViewById(R.id.text_user_header);
        imgPerfil = view.findViewById(R.id.img_perfil);
        btnAlterarFoto = view.findViewById(R.id.btn_alterar_foto);
        btnDeslogar = view.findViewById(R.id.btn_deslogar);
    }

    private void IniciarLaunchers() {
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result && imageUri != null) startCrop(imageUri);
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) startCrop(uri);
        });

        uCropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                Uri resultUri = UCrop.getOutput(result.getData());
                if (resultUri != null) uploadProfilePhoto(resultUri);
            }
        });
    }

    private void mostrarDialogoSelecaoImagem() {
        String[] options = {"Câmera", "Galeria"};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Alterar Foto de Perfil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera(); else galleryLauncher.launch("image/*");
                }).show();
    }

    private void openCamera() {
        try {
            File photoFile = new File(requireActivity().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "profile_temp.jpg");
            imageUri = FileProvider.getUriForFile(requireContext(), "com.example.lordseg.fileprovider", photoFile);
            takePictureLauncher.launch(imageUri);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao abrir câmera", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCrop(Uri uri) {
        if (getContext() == null) return;
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped_profile.jpg"));
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true); // Estilo redondo configurado
        options.setShowCropGrid(false);
        options.setShowCropFrame(false);
        options.setToolbarTitle("Ajustar Foto");

        UCrop uCrop = UCrop.of(uri, destinationUri)
                .withAspectRatio(1, 1) // Formato quadrado/redondo
                .withMaxResultSize(500, 500);
        
        uCrop.withOptions(options);
        uCropLauncher.launch(uCrop.getIntent(requireContext()));
    }

    private void uploadProfilePhoto(Uri uri) {
        if (token == null || getContext() == null) return;

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return;
            
            File file = new File(requireContext().getCacheDir(), "profile_final.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "profile.jpg", requestFile);

            ApiService service = RetrofitClient.getClient().create(ApiService.class);
            service.uploadImage(body, token).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imageUrl = response.body().get("url");
                        // Exibe redondo imediatamente usando Glide
                        if (getActivity() != null) {
                            Glide.with(TelaPerfil.this).load(imageUrl).circleCrop().into(imgPerfil);
                        }
                        Toast.makeText(getContext(), "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("UPLOAD_PROFILE", "Erro no servidor: " + response.code());
                        Toast.makeText(getContext(), "Falha no servidor ao salvar foto", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    Toast.makeText(getContext(), "Erro de rede ao carregar foto", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("CROP", "Erro ao preparar upload", e);
        }
    }

    private void loadUserProfile() {
        if (getActivity() instanceof MainActivity) {
            token = "Bearer " + ((MainActivity) getActivity()).token;
        }

        if (token == null) return;

        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        service.getMe(token).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();
                    nomeUsuario.setText(user.name);
                    emailUsuario.setText(user.email);
                    telefoneUsuario.setText(user.phone);

                    headerNome.setText(user.name);

                    if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
                        Glide.with(TelaPerfil.this)
                                .load(user.photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_person)
                                .into(imgPerfil);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Erro ao carregar perfil", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
