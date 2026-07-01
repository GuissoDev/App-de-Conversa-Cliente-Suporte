package com.example.lordseg.Adaptadores;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lordseg.Actives.VisualizarDocumentoActivity;
import com.example.lordseg.Actives.VisualizarImagemActivity;
import com.example.lordseg.Model.MessageModel;
import com.example.lordseg.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<MessageModel> messageList;
    private String currentUserId;

    // Constantes para identificar os 6 tipos de balões no chat
    private static final int VIEW_TYPE_TEXT_SENT = 1;
    private static final int VIEW_TYPE_TEXT_RECEIVED = 2;
    private static final int VIEW_TYPE_IMAGE_SENT = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
    private static final int VIEW_TYPE_DOC_SENT = 5;
    private static final int VIEW_TYPE_DOC_RECEIVED = 6;

    public ChatAdapter(List<MessageModel> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel model = messageList.get(position);

        boolean isMe = model.senderId != null && String.valueOf(model.senderId).equals(currentUserId);
        boolean isImage = model.content != null && model.content.startsWith("[IMAGEM]:");
        boolean isDoc = model.content != null && model.content.startsWith("[DOCUMENTO]:");

        if (isMe) {
            if (isImage) return VIEW_TYPE_IMAGE_SENT;
            if (isDoc) return VIEW_TYPE_DOC_SENT;
            return VIEW_TYPE_TEXT_SENT;
        } else {
            if (isImage) return VIEW_TYPE_IMAGE_RECEIVED;
            if (isDoc) return VIEW_TYPE_DOC_RECEIVED;
            return VIEW_TYPE_TEXT_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_IMAGE_SENT:
                return new SentImageViewHolder(inflater.inflate(R.layout.chat_message_enviada_img, parent, false));
            case VIEW_TYPE_IMAGE_RECEIVED:
                return new ReceivedImageViewHolder(inflater.inflate(R.layout.chat_message_received_img, parent, false));
            case VIEW_TYPE_DOC_SENT:
                return new SentDocViewHolder(inflater.inflate(R.layout.chat_message_enviada_doc, parent, false));
            case VIEW_TYPE_DOC_RECEIVED:
                return new ReceivedDocViewHolder(inflater.inflate(R.layout.chat_message_received_doc, parent, false));
            case VIEW_TYPE_TEXT_RECEIVED:
                return new ReceivedTextPackageViewHolder(inflater.inflate(R.layout.chat_message_recycler_row_received, parent, false));
            case VIEW_TYPE_TEXT_SENT:
            default:
                return new SentTextPackageViewHolder(inflater.inflate(R.layout.chat_message_enviada, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel model = messageList.get(position);

        String time = "";
        if (model.timestamp != null && model.timestamp.length() >= 16) {
            try {
                time = model.timestamp.substring(11, 16);
            } catch (Exception ignored) {}
        }

        // Processamento de Tags de Mídia
        boolean isImage = model.content != null && model.content.startsWith("[IMAGEM]:");
        boolean isDoc = model.content != null && model.content.startsWith("[DOCUMENTO]:");

        String cleanedUrl = model.content;
        if (isImage) cleanedUrl = model.content.replace("[IMAGEM]:", "").trim();
        if (isDoc) cleanedUrl = model.content.replace("[DOCUMENTO]:", "").trim();

        // Extrai o nome original do documento removendo o UUID gerado pelo S3
        String filename = "Documento Anexo";
        if (isDoc && cleanedUrl.contains("/")) {
            filename = cleanedUrl.substring(cleanedUrl.lastIndexOf("/") + 1);
            if (filename.contains("_")) {
                filename = filename.substring(filename.indexOf("_") + 1);
            }
        }

        final String finalUrl = cleanedUrl;
        final String finalFilename = filename;

        // --- RENDERIZAÇÃO DOS BALÕES ---

        if (holder instanceof SentTextPackageViewHolder) {
            SentTextPackageViewHolder textSentHolder = (SentTextPackageViewHolder) holder;
            textSentHolder.messageText.setText(model.content);
            configuraTiquesLeitura(textSentHolder.timeText, time, model.viewed);

        } else if (holder instanceof ReceivedTextPackageViewHolder) {
            ReceivedTextPackageViewHolder textRecvHolder = (ReceivedTextPackageViewHolder) holder;
            textRecvHolder.messageText.setText(model.content);
            textRecvHolder.timeText.setText(time);

        } else if (holder instanceof SentImageViewHolder) {
            SentImageViewHolder imgSentHolder = (SentImageViewHolder) holder;
            configuraTiquesLeitura(imgSentHolder.timeText, time, model.viewed);
            Glide.with(imgSentHolder.itemView.getContext()).load(finalUrl).placeholder(R.drawable.ic_launcher_background).into(imgSentHolder.imageView);

            imgSentHolder.itemView.setOnClickListener(v -> abrirVisualizadorImagem(v.getContext(), finalUrl, "Você", model.timestamp));

        } else if (holder instanceof ReceivedImageViewHolder) {
            ReceivedImageViewHolder imgRecvHolder = (ReceivedImageViewHolder) holder;
            imgRecvHolder.timeText.setText(time);
            Glide.with(imgRecvHolder.itemView.getContext()).load(finalUrl).placeholder(R.drawable.ic_launcher_background).into(imgRecvHolder.imageView);

            imgRecvHolder.itemView.setOnClickListener(v -> abrirVisualizadorImagem(v.getContext(), finalUrl, "Mensagem Recebida", model.timestamp));

        } else if (holder instanceof SentDocViewHolder) {
            SentDocViewHolder docSentHolder = (SentDocViewHolder) holder;
            docSentHolder.docNameText.setText(finalFilename);
            docSentHolder.timeText.setText(time);

            // Evento para abrir a tela de visualização/download do documento
            docSentHolder.itemView.setOnClickListener(v -> abrirVisualizadorDocumento(v.getContext(), finalUrl, finalFilename));

        } else if (holder instanceof ReceivedDocViewHolder) {
            ReceivedDocViewHolder docRecvHolder = (ReceivedDocViewHolder) holder;
            docRecvHolder.docNameText.setText(finalFilename);
            docRecvHolder.timeText.setText(time);

            docRecvHolder.itemView.setOnClickListener(v -> abrirVisualizadorDocumento(v.getContext(), finalUrl, finalFilename));
        }
    }

    private void configuraTiquesLeitura(TextView view, String time, boolean viewed) {
        if (viewed) {
            view.setText(time + " ✓✓");
            view.setTextColor(android.graphics.Color.parseColor("#34B7F1"));
        } else {
            view.setText(time + " ✓");
            view.setTextColor(android.graphics.Color.GRAY);
        }
    }

    private void abrirVisualizadorDocumento(android.content.Context context, String url, String name) {
        Intent intent = new Intent(context, VisualizarDocumentoActivity.class);
        intent.putExtra("doc_url", url);
        intent.putExtra("doc_name", name);
        context.startActivity(intent);
    }

    private void abrirVisualizadorImagem(android.content.Context context, String url, String sender, String timestamp) {
        Intent intent = new Intent(context, VisualizarImagemActivity.class);
        intent.putExtra("imageUrl", url);
        intent.putExtra("senderName", sender);
        
        // Tenta converter o timestamp para millis para a barra de título
        long millis = 0;
        if (timestamp != null) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                java.util.Date date = sdf.parse(timestamp);
                if (date != null) millis = date.getTime();
            } catch (Exception ignored) {}
        }
        intent.putExtra("time", millis);

        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // --- VIEW HOLDERS DE TEXTO E IMAGEM ---
    static class SentTextPackageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        public SentTextPackageViewHolder(@NonNull View itemView) { super(itemView); messageText = itemView.findViewById(R.id.show_message); timeText = itemView.findViewById(R.id.id_time); }
    }

    static class ReceivedTextPackageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        public ReceivedTextPackageViewHolder(@NonNull View itemView) { super(itemView); messageText = itemView.findViewById(R.id.show_message); timeText = itemView.findViewById(R.id.id_time); }
    }

    static class SentImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView; TextView timeText;
        public SentImageViewHolder(@NonNull View itemView) { super(itemView); imageView = itemView.findViewById(R.id.show_image); timeText = itemView.findViewById(R.id.id_time); }
    }

    static class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView; TextView timeText;
        public ReceivedImageViewHolder(@NonNull View itemView) { super(itemView); imageView = itemView.findViewById(R.id.show_image); timeText = itemView.findViewById(R.id.id_time); }
    }

    // --- NOVOS VIEW HOLDERS DE DOCUMENTO ---
    static class SentDocViewHolder extends RecyclerView.ViewHolder {
        TextView docNameText, timeText;
        public SentDocViewHolder(@NonNull View itemView) {
            super(itemView);
            docNameText = itemView.findViewById(R.id.text_document_name); // Vinculado ao seu XML de doc enviado
            timeText = itemView.findViewById(R.id.id_time);
        }
    }

    static class ReceivedDocViewHolder extends RecyclerView.ViewHolder {
        TextView docNameText, timeText;
        public ReceivedDocViewHolder(@NonNull View itemView) {
            super(itemView);
            docNameText = itemView.findViewById(R.id.text_document_name); // Vinculado ao seu XML de doc recebido
            timeText = itemView.findViewById(R.id.id_time);
        }
    }
}