package com.example.seminariofinal;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- API DE MENSAJES Y CONTACTOS ---

    // Corresponde a: path('api/send-message/', views.send_message_api)
    @POST("api/send-message/")
    Call<Map<String, Object>> sendMessage(@Body Map<String, Object> data);

    // Corresponde a: path('api/delete-message/<int:message_id>/', views.delete_message_api)
    @POST("api/delete-message/{message_id}/")
    Call<Map<String, Object>> deleteMessage(@Path("message_id") int messageId);

    // Corresponde a: path('api/hide-chat/<int:room_id>/', views.hide_chat_from_home_api)
    @POST("api/hide-chat/{room_id}/")
    Call<Map<String, Object>> hideChat(@Path("room_id") int roomId);

    // Corresponde a: path('api/search-users/', views.search_users_api)
    @GET("api/search-users/")
    Call<List<Map<String, Object>>> searchUsers(@Query("q") String query);

    // Corresponde a: path('api/save-contact/', views.save_contact_api)
    @POST("api/save-contact/")
    Call<Map<String, Object>> saveContact(@Body Map<String, Object> contactData);

    // Corresponde a: path('api/delete-contact/<int:contact_id>/', views.delete_contact_api)
    @POST("api/delete-contact/{contact_id}/")
    Call<Map<String, Object>> deleteContact(@Path("contact_id") int contactId);

    // Corresponde a: path('api/update-security-level/', views.update_security_level_api)
    @POST("api/update-security-level/")
    Call<Map<String, Object>> updateSecurityLevel(@Body Map<String, Object> data);

    // --- API DE GRUPOS ---

    // Corresponde a: path('api/create-group/', views.create_group_api)
    @POST("api/create-group/")
    Call<Map<String, Object>> createGroup(@Body Map<String, Object> groupData);

    // Corresponde a: path('api/edit-group/<int:group_id>/', views.edit_group_api)
    @POST("api/edit-group/{group_id}/")
    Call<Map<String, Object>> editGroup(@Path("group_id") int groupId, @Body Map<String, Object> groupData);

    // Corresponde a: path('api/delete-group/<int:group_id>/', views.delete_group_api)
    @POST("api/delete-group/{group_id}/")
    Call<Map<String, Object>> deleteGroup(@Path("group_id") int groupId);

    // --- CLAVE PÚBLICA ---

    // Corresponde a: path('actualizar-clave-publica/', views.actualizar_clave_publica_api)
    @POST("actualizar-clave-publica/")
    Call<Map<String, Object>> actualizarClavePublica(@Body Map<String, Object> keyData);

// --- API DE LLAMADAS ---

    // Emulador A: Notifica al backend que desea llamar a un usuario
    // Body esperado: {"caller_phone": "111111", "receiver_phone": "222222"}

    // --- SESIÓN Y LOGIN ---
    @POST("api/login/")
    Call<Map<String, Object>> login(@Body Map<String, Object> credentials);

    @POST("api/start-call/")
    Call<Map<String, Object>> startCall(@Body Map<String, Object> callData);

    // Emulador B: Polling periódica para verificar llamadas entrantes pendientes
    // Query esperada: ?my_phone=222222
    @GET("api/check-incoming-call/")
    Call<Map<String, Object>> checkIncomingCall(@Query("my_phone") String myPhone);

    // Emulador B: Notifica la acción del usuario (contestar / rechazar)
    // Body esperado: {"caller_phone": "111111", "receiver_phone": "222222", "action": "accept" / "reject"}
    @POST("api/respond-call/")
    Call<Map<String, Object>> respondCall(@Body Map<String, Object> responseData);

    @GET("api/calls/pending/")
    Call<Map<String, Object>> getPendingCall();

    @POST("api/calls/{id}/state/")
    Call<Map<String, Object>> setCallState(@Path("id") int id, @Body Map<String, Object> body);
}