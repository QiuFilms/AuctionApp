FROM golang:1.26-alpine AS builder

# Instalacja niezbędnych bibliotek do CGO
RUN apk add --no-cache gcc musl-dev

WORKDIR /app
COPY . .

# Włączenie CGO dla SQLite
ENV CGO_ENABLED=1
RUN go build -o main ./api/main.go
# Jeśli API korzysta z certyfikatów, pamiętaj o ich skopiowaniu
EXPOSE 8080
CMD ["./main"]