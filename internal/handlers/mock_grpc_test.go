package handlers_test

import (
	"context"
	pb "mmo-auctions-go/pb"

	"google.golang.org/grpc"
)

type MockRpgClient struct {
	pb.RpgServiceClient
}

func (m *MockRpgClient) VerifyUser(ctx context.Context, in *pb.LoginRequest, opts ...grpc.CallOption) (*pb.LoginResponse, error) {
	if in.Username == "testGO" && in.Password == "test" {
		return &pb.LoginResponse{Token: "valid-token", Success: true}, nil
	}
	return &pb.LoginResponse{Success: false}, nil
}

func (m *MockRpgClient) GetInventory(ctx context.Context, in *pb.UserRequest, opts ...grpc.CallOption) (*pb.InventoryResponse, error) {
	if in.Token == "valid-token" {
		return &pb.InventoryResponse{Items: []string{"Sword", "Shield"}}, nil
	}
	return nil, context.DeadlineExceeded
}

func (m *MockRpgClient) GetAuctions(ctx context.Context, in *pb.Empty, opts ...grpc.CallOption) (*pb.AuctionListResponse, error) {
	return &pb.AuctionListResponse{ActiveAuctions: []string{"Aukcja1", "Aukcja2"}}, nil
}

func (m *MockRpgClient) AddItemToInventory(ctx context.Context, in *pb.ModifyItemRequest, opts ...grpc.CallOption) (*pb.ActionResponse, error) {
	return &pb.ActionResponse{Success: true, Message: "Dodano"}, nil
}

func (m *MockRpgClient) RemoveItemFromInventory(ctx context.Context, in *pb.ModifyItemRequest, opts ...grpc.CallOption) (*pb.ActionResponse, error) {
	return &pb.ActionResponse{Success: true, Message: "Usunięto"}, nil
}

func (m *MockRpgClient) UploadAvatar(ctx context.Context, in *pb.UploadAvatarRequest, opts ...grpc.CallOption) (*pb.UploadAvatarResponse, error) {
	return &pb.UploadAvatarResponse{Success: true, Message: "Wgrano"}, nil
}
