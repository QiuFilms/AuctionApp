package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"mmo-auctions-go/internal/database"
	pb "mmo-auctions-go/pb"
	"net/http"
	"strconv"
)

type API struct {
	GrpcClient pb.RpgServiceClient
}

type ItemPayload struct {
	ItemID int64 `json:"itemId"`
}

type LoginPayload struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// @Summary Logowanie użytkownika
// @Description Weryfikuje dane użytkownika i zwraca token JWT
// @Tags Auth
// @Accept json
// @Produce json
// @Param credentials body LoginPayload true "Dane logowania (username i password)"
// @Success 200 {object} map[string]string "Zwraca obiekt z polem 'token'"
// @Failure 400 {object} map[string]string "Błędne dane wejściowe"
// @Failure 500 {object} map[string]string "Błąd wewnętrzny serwera"
// @Router /api/login [post]
func (api *API) Login(w http.ResponseWriter, r *http.Request) {
	var payload LoginPayload
	if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
		http.Error(w, "Błędne dane", http.StatusBadRequest)
		return
	}

	resp, err := api.GrpcClient.VerifyUser(r.Context(), &pb.LoginRequest{
		Username: payload.Username,
		Password: payload.Password,
	})

	if err != nil || !resp.Success {
		http.Error(w, "Nieprawidłowe dane logowania", http.StatusUnauthorized)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"token": resp.Token,
	})
}

// @Summary Pobierz ekwipunek
// @Description Zwraca listę przedmiotów w ekwipunku użytkownika
// @Tags Inventory
// @Security BearerAuth
// @Produce json
// @Success 200 {object} pb.InventoryResponse
// @Router /api/inventory [get]
func (api *API) GetInventory(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Metoda niedozwolona", http.StatusMethodNotAllowed)
		return
	}

	token, _ := r.Context().Value("token").(string)
	resp, err := api.GrpcClient.GetInventory(context.Background(), &pb.UserRequest{Token: token})
	if err != nil {
		http.Error(w, `{"error": "Błąd komunikacji z serwerem gRPC"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

// @Summary Prześlij avatar
// @Description Wgrywa plik graficzny jako avatar użytkownika
// @Tags Avatar
// @Security BearerAuth
// @Accept multipart/form-data
// @Param avatar formData file true "Plik avatara (png, jpeg, gif)"
// @Success 200 {object} pb.UploadAvatarResponse
// @Router /api/avatar/upload [post]
func (api *API) UploadAvatar(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Metoda niedozwolona", http.StatusMethodNotAllowed)
		return
	}

	const maxMemory = 1 << 20
	err := r.ParseMultipartForm(maxMemory)
	if err != nil {
		http.Error(w, `{"error": "Plik jest za duży (limit 1MB)"}`, http.StatusBadRequest)
		return
	}

	file, fileHeader, err := r.FormFile("avatar")
	if err != nil {
		http.Error(w, `{"error": "Brak pliku 'avatar'"}`, http.StatusBadRequest)
		return
	}
	defer file.Close()

	if fileHeader.Size > maxMemory {
		http.Error(w, `{"error": "Plik przekracza limit 1MB"}`, http.StatusRequestEntityTooLarge)
		return
	}

	contentType := fileHeader.Header.Get("Content-Type")
	if contentType == "" {
		contentType = "application/octet-stream"
	}

	fileBytes, err := io.ReadAll(file)
	if err != nil {
		http.Error(w, `{"error": "Nie można odczytać pliku"}`, http.StatusInternalServerError)
		return
	}

	token, _ := r.Context().Value("token").(string)
	resp, err := api.GrpcClient.UploadAvatar(r.Context(), &pb.UploadAvatarRequest{
		Token:       token,
		ImageData:   fileBytes,
		ContentType: contentType,
	})

	if err != nil {
		http.Error(w, `{"error": "Błąd zapisu avatara na serwerze"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

// @Summary Pobierz avatar
// @Description Pobiera plik avatara użytkownika
// @Tags Avatar
// @Security BearerAuth
// @Produce image/png
// @Success 200 {file} binary
// @Router /api/avatar/download [get]
func (api *API) DownloadAvatar(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Metoda niedozwolona", http.StatusMethodNotAllowed)
		return
	}

	token, _ := r.Context().Value("token").(string)

	resp, err := api.GrpcClient.DownloadAvatar(context.Background(), &pb.UserRequest{
		Token: token,
	})
	if err != nil {
		http.Error(w, `{"error": "Nie udało się pobrać avatara"}`, http.StatusInternalServerError)
		return
	}

	if len(resp.ImageData) == 0 {
		http.Error(w, `{"error": "Brak avatara"}`, http.StatusNotFound)
		return
	}

	// 4. Ustawienie nagłówków dla przeglądarki
	w.Header().Set("Content-Type", resp.ContentType)
	w.Header().Set("Content-Length", fmt.Sprint(len(resp.ImageData)))

	w.Write(resp.ImageData)
}

// @Summary Lista aukcji
// @Description Zwraca listę wszystkich dostępnych aukcji
// @Tags Auctions
// @Security BearerAuth
// @Produce json
// @Success 200 {object} pb.AuctionListResponse
// @Router /api/auctions [get]
func (api *API) GetAuctions(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Metoda niedozwolona", http.StatusMethodNotAllowed)
		return
	}

	resp, err := api.GrpcClient.GetAuctions(r.Context(), &pb.Empty{})
	if err != nil {
		http.Error(w, "Błąd pobierania aukcji: "+err.Error(), http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

// @Summary Dodaj przedmiot
// @Description Dodaje przedmiot do ekwipunku użytkownika
// @Tags Items
// @Security BearerAuth
// @Accept json
// @Param body body object{itemId=int64} true "ID przedmiotu"
// @Success 200 {object} pb.ActionResponse
// @Router /api/items/add [post]
func (api *API) AddItemToInventory(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Metoda niedozwolona", http.StatusMethodNotAllowed)
		return
	}

	token, _ := r.Context().Value("token").(string)
	var payload ItemPayload
	err := json.NewDecoder(r.Body).Decode(&payload)
	if err != nil {
		http.Error(w, "Nieprawidłowy JSON", http.StatusBadRequest)
		return
	}

	itemId := payload.ItemID
	fmt.Println(itemId)

	resp, err := api.GrpcClient.AddItemToInventory(r.Context(), &pb.ModifyItemRequest{
		Token:  token,
		ItemId: itemId,
	})
	if err != nil {
		http.Error(w, "Błąd dodawania przedmiotu: "+err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(resp)
}

// @Summary Lista przedmiotów
// @Description Pobiera katalog wszystkich dostępnych przedmiotów
// @Tags Items
// @Security BearerAuth
// @Produce json
// @Success 200 {object} pb.InventoryResponse
// @Router /api/items [get]
func (api *API) GetItems(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Metoda niedozwolona", http.StatusMethodNotAllowed)
		return
	}
	resp, err := api.GrpcClient.GetItems(r.Context(), &pb.Empty{})
	if err != nil {
		http.Error(w, "Błąd pobierania przedmiotów: "+err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(resp)
}

// @Summary Usuń przedmiot
// @Description Usuwa przedmiot z ekwipunku użytkownika
// @Tags Items
// @Security BearerAuth
// @Param itemId query int64 true "ID przedmiotu do usunięcia"
// @Success 200 {object} pb.ActionResponse
// @Router /api/items/delete [delete]
func (api *API) RemoveItem(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodDelete {
		http.Error(w, "Metoda niedozwolona", http.StatusMethodNotAllowed)
		return
	}

	token, _ := r.Context().Value("token").(string)

	itemId, err := strconv.ParseInt(r.URL.Query().Get("itemId"), 10, 64)
	resp, err := api.GrpcClient.RemoveItemFromInventory(r.Context(), &pb.ModifyItemRequest{
		Token:  token,
		ItemId: itemId,
	})
	if err != nil {
		http.Error(w, "Błąd usuwania przedmiotu: "+err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(resp)
}

// @Summary Pobierz statystyki użycia endpointów
// @Description Zwraca zestawienie liczby wywołań poszczególnych endpointów z podziałem na statusy HTTP
// @Tags Stats
// @Produce json
// @Success 200 {array} map[string]interface{} "Lista statystyk"
// @Failure 500 {object} map[string]string "Błąd pobierania statystyk"
// @Router /api/stats [get]
func (api *API) GetStats(w http.ResponseWriter, r *http.Request) {
	rows, err := database.DB.Query("SELECT path, method, status, count FROM endpoint_stats")
	if err != nil {
		http.Error(w, "Błąd bazy danych", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	stats := []map[string]interface{}{}
	for rows.Next() {
		var path, method string
		var status, count int
		rows.Scan(&path, &method, &status, &count)
		stats = append(stats, map[string]interface{}{
			"path": path, "method": method, "status": status, "count": count,
		})
	}
	json.NewEncoder(w).Encode(stats)
}
