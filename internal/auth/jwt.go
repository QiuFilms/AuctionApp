package auth

import (
	"context"
	"net/http"
	"strings"
)

var jwtSecretKey = []byte("ToJestNowyBardzoTajnyKluczDoAplikacjiMmoAuctionsKtoryMaWiecejNiz32Znaki!")

func JWTMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader == "" {
			http.Error(w, `{"error": "Brak nagłówka Authorization"}`, http.StatusUnauthorized)
			return
		}

		parts := strings.Split(authHeader, " ")
		if len(parts) != 2 || parts[0] != "Bearer" {
			http.Error(w, `{"error": "Nieprawidłowy format tokenu"}`, http.StatusUnauthorized)
			return
		}

		tokenString := parts[1]

		ctx := context.WithValue(r.Context(), "token", tokenString)

		next.ServeHTTP(w, r.WithContext(ctx))
	}
}
