package grpcclient

import (
	"log"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	"mmo-auctions-go/pb"
)

func NewClient(address string) (pb.RpgServiceClient, *grpc.ClientConn) {
	conn, err := grpc.Dial(address, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("Nie udało się połączyć ze Springiem (gRPC): %v", err)
	}

	client := pb.NewRpgServiceClient(conn)
	return client, conn
}
