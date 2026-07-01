package com.example.lordseg.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lordseg.Adaptadores.AdaptadorConversasRecentes;
import com.example.lordseg.Model.UserResponse;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.R;
import com.example.lordseg.Actives.MainActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ChatFragment: Exibe a lista de conversas recentes vindas da API MySQL.
 */
public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdaptadorConversasRecentes adapter;
    private List<UserResponse> userList = new ArrayList<>();
    
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat_fragment, container, false);

        recyclerView = view.findViewById(R.id.recycler_conversas_recentes);
        setupRecyclerView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConversations();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    private void startPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadConversations();
                pollingHandler.postDelayed(this, 10000); // Atualiza a cada 10 segundos
            }
        };
        pollingHandler.postDelayed(pollingRunnable, 10000);
    }

    private void stopPolling() {
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    private void setupRecyclerView() {
        String token = "";
        if (getActivity() instanceof MainActivity) {
            token = ((MainActivity) getActivity()).token;
        }

        adapter = new AdaptadorConversasRecentes(userList, token);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadConversations() {
        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        String token = "";
        if (getActivity() instanceof MainActivity) {
            token = "Bearer " + ((MainActivity) getActivity()).token;
        }

        // AGORA BUSCA AS CONVERSAS REAIS (Não apenas o 'Me')
        service.getConversations(token).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<UserResponse>> call, @NonNull Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<UserResponse> filteredList = new ArrayList<>();
                    Set<Long> seenUserIds = new HashSet<>();
                    
                    for (UserResponse user : response.body()) {
                        if (user.id != null && !seenUserIds.contains(user.id)) {
                            filteredList.add(user);
                            seenUserIds.add(user.id);
                        }
                    }
                    
                    userList.clear();
                    userList.addAll(filteredList);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserResponse>> call, @NonNull Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Erro ao carregar conversas", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
