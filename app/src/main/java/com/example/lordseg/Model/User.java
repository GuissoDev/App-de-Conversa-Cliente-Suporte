package com.example.lordseg.Model;

import java.util.List;

/**
 * User: Representa os dados básicos de um usuário.
 * Refatorado para remover dependências do Firebase Firestore.
 */
public class User {
    private String nome;
    private String email;
    private String telefone;
    private String fotoUrl;
    private String status;
    private String fcmToken;
    private String nome_minusculo;
    private List<String> palavras_chave;

    public User() {
    }

    public User(String nome, String email, String telefone) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getNome_minusculo() {
        return nome_minusculo;
    }

    public void setNome_minusculo(String nome_minusculo) {
        this.nome_minusculo = nome_minusculo;
    }

    public List<String> getPalavras_chave() {
        return palavras_chave;
    }

    public void setPalavras_chave(List<String> palavras_chave) {
        this.palavras_chave = palavras_chave;
    }
}
