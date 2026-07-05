package main

import (
	"database/sql"
	"fmt"
	"log"
	_ "mmo-auctions-go/docs"
	"mmo-auctions-go/internal/auth"
	"mmo-auctions-go/internal/database"
	"mmo-auctions-go/internal/grpcclient"
	"mmo-auctions-go/internal/handlers"
	"net/http"

	_ "github.com/mattn/go-sqlite3"

	httpSwagger "github.com/swaggo/http-swagger"
)

type statusRecorder struct {
	http.ResponseWriter
	statusCode int
}

var db *sql.DB

func InitDB() {
	var err error
	database.DB, err = sql.Open("sqlite3", "./stats.db")
	if err != nil {
		log.Fatal(err)
	}
	statement, err := database.DB.Prepare("CREATE TABLE IF NOT EXISTS endpoint_stats (path TEXT, method TEXT, status INTEGER, count INTEGER, PRIMARY KEY (path, method, status))")
	if err != nil {
		log.Fatal(err)
	}
	statement.Exec()
}

func (rec *statusRecorder) WriteHeader(code int) {
	rec.statusCode = code
	rec.ResponseWriter.WriteHeader(code)
}

func LoggingMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		rec := &statusRecorder{ResponseWriter: w, statusCode: http.StatusOK}
		next.ServeHTTP(rec, r)

		query := `INSERT INTO endpoint_stats (path, method, status, count) VALUES (?, ?, ?, 1)
                  ON CONFLICT(path, method, status) DO UPDATE SET count = count + 1`
		database.DB.Exec(query, r.URL.Path, r.Method, rec.statusCode)
	}
}

func CORSMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// w.Header().Set("Access-Control-Allow-Origin", "https://localhost:8443")
		w.Header().Set("Access-Control-Allow-Origin", "https://16.171.254.158:8443")

		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE, PUT")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}

// @title Arcane Market API
// @version 1.0
// @description API serwera Go (BFF) dla systemu aukcyjnego.
// @host localhost:8080
// @BasePath /
// @securityDefinitions.apikey BearerAuth
// @in header
// @name Authorization
// @description Wpisz "Bearer {twój_token}" w pole poniżej.
func main() {
	fmt.Println("Uruchamianie API w Go (BFF)...")
	InitDB()

	client, conn := grpcclient.NewClient("spring-app:9090")
	defer conn.Close()

	api := handlers.API{GrpcClient: client}
	handle := func(path string, h http.HandlerFunc) {
		http.HandleFunc(path, LoggingMiddleware(CORSMiddleware(auth.JWTMiddleware(h))))
	}

	http.HandleFunc("/swagger/", httpSwagger.WrapHandler)
	http.HandleFunc("/api/login", api.Login)
	http.HandleFunc("/api/stats", api.GetStats)

	handle("/api/inventory", api.GetInventory)
	handle("/api/avatar/upload", api.UploadAvatar)
	handle("/api/avatar/download", api.DownloadAvatar)

	handle("/api/auctions", api.GetAuctions)
	handle("/api/items/add", api.AddItemToInventory)
	handle("/api/items", api.GetItems)
	handle("/api/items/delete", api.RemoveItem)

	fmt.Println("Serwer REST HTTPS nasłuchuje na https://localhost:8080")

	err := http.ListenAndServeTLS(":8080", "cert.pem", "key.pem", nil)

	if err != nil {
		log.Fatalf("Błąd serwera HTTPS: %v", err)
	}

}
