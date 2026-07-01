package com.example.lordseg.Adaptadores;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.example.lordseg.Actives.ChatActivity;
import com.example.lordseg.Actives.DetalhesUsuarioADM;
import com.example.lordseg.Model.UserResponse;
import com.example.lordseg.Network.ApiService;
import com.example.lordseg.Network.RetrofitClient;
import com.example.lordseg.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdaptadorConversasRecentes extends RecyclerView.Adapter<AdaptadorConversasRecentes.ViewHolder> {

    private final List<UserResponse> userList;
    private final String token;
    private boolean isModoADM = false;

    public AdaptadorConversasRecentes(List<UserResponse> userList, String token) {
        this.userList = userList;
        this.token = token;
    }

    public AdaptadorConversasRecentes(List<UserResponse> userList, String token, boolean isModoADM) {
        this.userList = userList;
        this.token = token;
        this.isModoADM = isModoADM;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserResponse user = userList.get(position);
        holder.textNome.setText(user.name);

        // 1. Exibir Última Mensagem no lugar do Email
        holder.textLastMessage.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.Ofwhite));
        if (user.lastMessage != null && !user.lastMessage.isEmpty()) {
            holder.textLastMessage.setText(user.lastMessage);
        } else {
            holder.textLastMessage.setText("Nenhuma mensagem ainda");
        }

        holder.textNome.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

        // 2. Status Online / Offline
        if (user.online) {
            holder.textStatus.setText("Online");
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.Green));
        } else {
            holder.textStatus.setText("Ofline");
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
        }

        // 3. Contador de Mensagens Não Lidas
        if (user.unreadCount > 0) {
            holder.textUnreadCount.setVisibility(View.VISIBLE);
            holder.textUnreadCount.setText(String.valueOf(user.unreadCount));
            // 4. Tom mais claro quando há mensagens novas
            holder.cardFundo.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.OfGreen));
            holder.textLastMessage.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.textLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.textUnreadCount.setVisibility(View.GONE);
            holder.cardFundo.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.card_background_light_green));
            holder.textLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Carregar foto de perfil se existir
        if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(holder.imgUser);
        } else {
            holder.imgUser.setImageResource(R.drawable.ic_person);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent;
            if (isModoADM) {
                // No modo ADM, vai para a tela de Detalhes
                intent = new Intent(v.getContext(), DetalhesUsuarioADM.class);
                intent.putExtra("email", user.email);
                intent.putExtra("phone", user.phone);
            } else {
                // No modo normal, vai para o Chat
                intent = new Intent(v.getContext(), ChatActivity.class);
            }
            
            intent.putExtra("nome", user.name);
            intent.putExtra("uid", String.valueOf(user.id));
            intent.putExtra("token", token);
            v.getContext().startActivity(intent);
        });

        // Clique longo para excluir conversa (Apenas fora do modo ADM)
        if (!isModoADM) {
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Excluir Conversa")
                        .setMessage("Deseja apagar esta conversa do seu histórico?")
                        .setPositiveButton("Excluir", (dialog, which) -> {
                            excluirConversaNoMySQL(user, position, v);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            });
        }
    }

    private void excluirConversaNoMySQL(UserResponse user, int position, View v) {
        if (token == null || user == null) return;

        // Remoção da tela
        if (position >= 0 && position < userList.size()) {
            userList.remove(position);
            notifyItemRemoved(position);
        }

        // Se conversationId for nulo, tenta deletar pelo id do usuário como fallback
        Long idParaExcluir = (user.conversationId != null) ? user.conversationId : user.id;

        ApiService service = RetrofitClient.getClient().create(ApiService.class);
        service.deleteConversation(idParaExcluir, "Bearer " + token).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("API_CHAT", "Excluído no servidor");
                    Toast.makeText(v.getContext(), "Conversa apagada!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w("API_CHAT", "Servidor retornou erro " + response.code());
                    // NÃO re-adicionamos à lista para garantir que a UI não mostre o item "voltando"
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("API_CHAT", "Falha de rede ao excluir");
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.linha_de_usuarios, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textLastMessage, textStatus, textUnreadCount;
        MaterialCardView cardFundo;
        ImageView imgUser;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.text_nome_user);
            textLastMessage = itemView.findViewById(R.id.text_email_user); // Usando o ID existente para a mensagem
            textStatus = itemView.findViewById(R.id.text_status_user);
            textUnreadCount = itemView.findViewById(R.id.unread_count);
            cardFundo = itemView.findViewById(R.id.card_fundo_user);
            imgUser = itemView.findViewById(R.id.img_user);
        }
    }
}
