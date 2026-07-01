package com.example.lordseg.Model;

public class CameraModel {
    private String nome;
    private String numero;
    private String ip;
    private String usuario;
    private String senha;
    private String porta;
    private String pathMain;
    private String pathSub;
    private String urlSubStream;
    private String urlMainStream;

    public CameraModel(String nome, String numero, String ip, String usuario, String senha, String porta, String pathMain, String pathSub) {
        this.nome = nome;
        this.numero = numero;
        this.ip = ip;
        this.usuario = usuario;
        this.senha = senha;
        this.porta = porta;
        this.pathMain = pathMain;
        this.pathSub = pathSub;
        
        // Monta a URL RTSP completa: rtsp://user:pass@ip:port/path
        this.urlMainStream = "rtsp://" + usuario + ":" + senha + "@" + ip + ":" + porta + pathMain;
        this.urlSubStream = "rtsp://" + usuario + ":" + senha + "@" + ip + ":" + porta + pathSub;
    }

    public String getNome() { return nome; }
    public String getNumero() { return numero; }
    public String getIp() { return ip; }
    public String getUsuario() { return usuario; }
    public String getSenha() { return senha; }
    public String getPorta() { return porta; }
    public String getPathMain() { return pathMain; }
    public String getPathSub() { return pathSub; }
    public String getUrlSubStream() { return urlSubStream; }
    public String getUrlMainStream() { return urlMainStream; }

    public String getExibicaoNome() {
        return numero + " - " + nome;
    }
}