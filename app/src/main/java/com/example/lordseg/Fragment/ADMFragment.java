package com.example.lordseg.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lordseg.Actives.AdicionarUsuarioADM;
import com.example.lordseg.Actives.MainActivity;
import com.example.lordseg.Adaptadores.AdaptadorConversasRecentes;
import com.example.lordseg.Model.UserResponse;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ADMFragment: Exibe a lista de todos os usuários cadastrados e permite adicionar novos.
 */
public class ADMFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdaptadorConversasRecentes adapter;
    private List<UserResponse> userList = new ArrayList<>();
    private String token;
    private FloatingActionButton fabAddUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adm, container, false);

        recyclerView = view.findViewById(R.id.recycler_usuarios_adm);
        fabAddUser = view.findViewById(R.id.fab_add_user);
        
        if (getActivity() instanceof MainActivity) {
            token = ((MainActivity) getActivity()).token;
        }

        setupRecyclerView();

        fabAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AdicionarUsuarioADM.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recarrega a lista toda vez que a tela volta a ficar visível
        loadAllUsers();
    }

    private void setupRecyclerView() {
        adapter = new AdaptadorConversasRecentes(userList, token, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadAllUsers() {
        if (token == null) return;

        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        service.getAllUsers("Bearer " + token).enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserResponse>> call, @NonNull Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userList.clear();
                    for (UserResponse user : response.body()) {
                        if (user.name != null && !"LordSeg".equalsIgnoreCase(user.name.trim())) {
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Erro ao carregar usuários: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserResponse>> call, @NonNull Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Falha de conexão", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
