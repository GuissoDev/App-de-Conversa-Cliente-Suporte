package com.example.lordseg.Network;

import com.example.lordseg.Model.AuthResponse;
import com.example.lordseg.Model.ConversationModel;
import com.example.lordseg.Model.LoginRequest;
import com.example.lordseg.Model.MessageModel;
import com.example.lordseg.Model.MessageRequest;
import com.example.lordseg.Model.RegisterRequest;
import com.example.lordseg.Model.UserResponse;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("/api/auth/register")
    Call<UserResponse> register(@Body RegisterRequest request);

    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("/api/users/me")
    Call<UserResponse> getMe(@Header("Authorization") String token);

    @GET("/api/messages/{conversationId}")
    Call<List<MessageModel>> getMessages(@Path("conversationId") long conversationId,
                                         @Header("Authorization") String token);

    @POST("/api/messages")
    Call<MessageModel> sendMessage(@Body MessageRequest request,
                                   @Header("Authorization") String token);

    @GET("/api/users")
    Call<List<UserResponse>> getAllUsers(@Header("Authorization") String token);

    @GET("/api/conversations")
    Call<List<UserResponse>> getConversations(@Header("Authorization") String token);

    @POST("/api/conversations")
    Call<ConversationModel> createOrGetConversation(@Body Map<String, Long> request, @Header("Authorization") String token);

    @GET("/api/conversations/user/{userId}")
    Call<List<ConversationModel>> getConversationHistory(@Path("userId") long userId, @Header("Authorization") String token);

    @DELETE("/api/users/{id}")
    Call<Void> deleteUser(@Path("id") long id, @Header("Authorization") String token);

    @DELETE("/api/conversations/{id}")
    Call<Void> deleteConversation(@Path("id") long id, @Header("Authorization") String token);

    @Multipart
    @POST("/api/messages/upload")
    Call<Map<String, String>> uploadImage(@Part MultipartBody.Part file,
                                          @Header("Authorization") String token);
}
