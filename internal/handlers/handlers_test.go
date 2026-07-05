package handlers_test

import (
	"bytes"
	"context"
	"mmo-auctions-go/internal/handlers"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestLogin_Endpoint(t *testing.T) {
	api := &handlers.API{GrpcClient: &MockRpgClient{}}

	payload := `{"username": "testGO", "password": "test"}`
	req, _ := http.NewRequest("POST", "/api/login", bytes.NewBufferString(payload))
	rr := httptest.NewRecorder()

	api.Login(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("Oczekiwano 200, otrzymano %d", rr.Code)
	}
}

func TestGetInventory_Endpoint(t *testing.T) {
	api := &handlers.API{GrpcClient: &MockRpgClient{}}

	req, _ := http.NewRequest("GET", "/api/inventory", nil)
	ctx := context.WithValue(req.Context(), "token", "valid-token")
	req = req.WithContext(ctx)

	rr := httptest.NewRecorder()
	api.GetInventory(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("Oczekiwano 200, otrzymano %d", rr.Code)
	}
}

func TestGetAuctions_Endpoint(t *testing.T) {
	api := &handlers.API{GrpcClient: &MockRpgClient{}}
	req, _ := http.NewRequest("GET", "/api/auctions", nil)
	rr := httptest.NewRecorder()
	api.GetAuctions(rr, req)
	if rr.Code != http.StatusOK {
		t.Errorf("Oczekiwano 200, otrzymano %d", rr.Code)
	}
}

func TestAddItem_Endpoint(t *testing.T) {
	api := &handlers.API{GrpcClient: &MockRpgClient{}}
	payload := `{"itemId": 1}`
	req, _ := http.NewRequest("POST", "/api/items/add", bytes.NewBufferString(payload))
	ctx := context.WithValue(req.Context(), "token", "valid-token")
	req = req.WithContext(ctx)
	rr := httptest.NewRecorder()
	api.AddItemToInventory(rr, req)
	if rr.Code != http.StatusOK {
		t.Errorf("Oczekiwano 200, otrzymano %d", rr.Code)
	}
}

func TestRemoveItem_Endpoint(t *testing.T) {
	api := &handlers.API{GrpcClient: &MockRpgClient{}}
	req, _ := http.NewRequest("DELETE", "/api/items/delete?itemId=1", nil)
	ctx := context.WithValue(req.Context(), "token", "valid-token")
	req = req.WithContext(ctx)
	rr := httptest.NewRecorder()
	api.RemoveItem(rr, req)
	if rr.Code != http.StatusOK {
		t.Errorf("Oczekiwano 200, otrzymano %d", rr.Code)
	}
}
