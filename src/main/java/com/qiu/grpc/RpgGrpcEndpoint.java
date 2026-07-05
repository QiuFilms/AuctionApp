package com.qiu.grpc; // <--- Zmień na swój pakiet!

import com.qiu.entities.ItemUser;
import com.qiu.entities.User;
import com.qiu.grpc.RpgGrpcEndpoint;
import com.qiu.mmoauctions.config.JwtService;
import com.qiu.repositories.AuctionRepository;
import com.qiu.repositories.ItemRepository;
import com.qiu.repositories.ItemUserRepository;
import com.qiu.repositories.UserRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.Ssl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@GrpcService
public class RpgGrpcEndpoint extends com.qiu.grpc.RpgServiceGrpc.RpgServiceImplBase {

    @Autowired
    private UserRepository userRepository; //


    @Autowired
    private AuctionRepository auctionRepository; //

    @Autowired
    private ItemUserRepository itemUserRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.qiu.services.ItemUserService itemUserService; //
    // --------------------------------------------------------
    // a) CZYTANIE DANYCH
    // --------------------------------------------------------
    private final PasswordEncoder passwordEncoder; // <--- To pole

    public RpgGrpcEndpoint(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public void getInventory(com.qiu.grpc.UserRequest request, StreamObserver<com.qiu.grpc.InventoryResponse> responseObserver) {

        System.out.println(request.getToken());
        String username = jwtService.extractUsername(request.getToken());

        java.util.Optional<com.qiu.entities.User> userOptional = userRepository.findByUsername(username);
        java.util.List<String> items = new java.util.ArrayList<>();

        if (userOptional.isPresent()) {
            com.qiu.entities.User user = userOptional.get();


            java.util.List<ItemUser> itemUsers = itemUserRepository.findByUser(user);

            for (ItemUser iu : itemUsers) {
                // Bezpieczny dostęp do nazwy przedmiotu
                if (iu.getItem() != null) {
                    items.add(iu.getItem().getName());
                }
            }
        } else {
            items.add("Błąd: Nie znaleziono użytkownika");
        }

        com.qiu.grpc.InventoryResponse response = com.qiu.grpc.InventoryResponse.newBuilder()
                .addAllItems(items)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAuctions(com.qiu.grpc.Empty request, StreamObserver<com.qiu.grpc.AuctionListResponse> responseObserver) {
        // 1. Pobierz aktywne aukcje z bazy danych
        java.util.List<com.qiu.entities.Auction> activeAuctions =
                auctionRepository.findAvailableAuctions(java.time.LocalDateTime.now()); //[cite: 3]

        // 2. Zamień obiekty Auction na listę Stringów (dla uproszczenia w gRPC)
        java.util.List<String> auctions = activeAuctions.stream()
                .map(auction -> "Aukcja ID: " + auction.getId() + " - do: " + auction.getEndDate())
                .toList();

        // 3. Zbuduj odpowiedź
        com.qiu.grpc.AuctionListResponse response = com.qiu.grpc.AuctionListResponse.newBuilder()
                .addAllActiveAuctions(auctions)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getItems(com.qiu.grpc.Empty request, StreamObserver<com.qiu.grpc.InventoryResponse> responseObserver) {
        // 1. Pobierz aktywne aukcje z bazy danych
        java.util.List<com.qiu.entities.Item> items = itemRepository.findAll();

        // 2. Zamień obiekty Auction na listę Stringów (dla uproszczenia w gRPC)
        java.util.List<String> itemsFormatted = items.stream()
                .map(item -> "Item ID: " + item.getId() + " | Name: " + item.getName())
                .toList();

        // 3. Zbuduj odpowiedź
        com.qiu.grpc.InventoryResponse response = com.qiu.grpc.InventoryResponse.newBuilder()
                .addAllItems(itemsFormatted)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // --------------------------------------------------------
    // b) MODYFIKACJA PRZEDMIOTÓW
    // --------------------------------------------------------

    @Override
    public void addItemToInventory(com.qiu.grpc.ModifyItemRequest request, StreamObserver<com.qiu.grpc.ActionResponse> responseObserver) {
        String username = jwtService.extractUsername(request.getToken());

        Long itemId = request.getItemId(); // Konwersja String z gRPC na Long dla bazy

        // Szukamy użytkownika i przedmiotu
        java.util.Optional<com.qiu.entities.User> userOpt = userRepository.findByUsername(username);
        java.util.Optional<com.qiu.entities.Item> itemOpt = itemRepository.findById(itemId);

        if (userOpt.isPresent() && itemOpt.isPresent()) {
            // Tworzymy powiązanie (zakładając standardową encję ItemUser)

            com.qiu.entities.ItemUser itemUser = new com.qiu.entities.ItemUser();
            itemUser.setUser(userOpt.get());
            itemUser.setItem(itemOpt.get());

            // Zapis do bazy
            itemUserRepository.save(itemUser);

            com.qiu.grpc.ActionResponse response = com.qiu.grpc.ActionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Przedmiot pomyślnie dodany do ekwipunku.")
                    .build();
            responseObserver.onNext(response);
        } else {
            System.out.println(userOpt.get().getUsername());
            System.out.println(itemOpt.get().getName());
            com.qiu.grpc.ActionResponse response = com.qiu.grpc.ActionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Błąd: Nie znaleziono użytkownika lub przedmiotu.")
                    .build();
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void removeItemFromInventory(com.qiu.grpc.ModifyItemRequest request, StreamObserver<com.qiu.grpc.ActionResponse> responseObserver) {
        String username = jwtService.extractUsername(request.getToken());

        long itemId = request.getItemId();

        List<ItemUser> items = itemUserRepository.getByUserIdAndItemId(userRepository.findByUsername(username).get().getId(), itemId);

        if (items.isEmpty()) {
            // Obsługa przypadku, gdy przedmiotu nie ma w ekwipunku
            com.qiu.grpc.ActionResponse response = com.qiu.grpc.ActionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Przedmiot nie istnieje w ekwipunku użytkownika.")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        if(items.size() == auctionRepository.findBySellerAndItemId(userRepository.findByUsername(username).get(), itemId).size()){
            com.qiu.grpc.ActionResponse response = com.qiu.grpc.ActionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Przedmiot jest na aukcji.")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }
        // 2. Usunięcie przedmiotu z bazy danych

        itemUserService.delete(items.get(0));

        // 3. Budowanie pozytywnej odpowiedzi
        com.qiu.grpc.ActionResponse response = com.qiu.grpc.ActionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Przedmiot pomyślnie usunięty.")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }



    @Override
    public void uploadAvatar(com.qiu.grpc.UploadAvatarRequest request, StreamObserver<com.qiu.grpc.UploadAvatarResponse> responseObserver) {
        String username = jwtService.extractUsername(request.getToken());

        byte[] imageBytes = request.getImageData().toByteArray();
        String contentType = request.getContentType(); // Pobieramy typ pliku z gRPC

        java.util.Optional<com.qiu.entities.User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            com.qiu.entities.User user = userOpt.get();
            user.setAvatar(imageBytes);
            user.setAvatarContentType(contentType); // Zapis do encji

            userRepository.save(user);

            com.qiu.grpc.UploadAvatarResponse response = com.qiu.grpc.UploadAvatarResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Avatar zaktualizowany (Typ: " + contentType + ")")
                    .build();
            responseObserver.onNext(response);
        } else {
            com.qiu.grpc.UploadAvatarResponse response = com.qiu.grpc.UploadAvatarResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Przedmiot nie istnieje w ekwipunku użytkownika.")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onCompleted();
    }

    @Override
    public void downloadAvatar(com.qiu.grpc.UserRequest request, StreamObserver<com.qiu.grpc.DownloadAvatarResponse> responseObserver) {
        String username = jwtService.extractUsername(request.getToken());

        java.util.Optional<com.qiu.entities.User> userOpt = userRepository.findByUsername(username);

        byte[] imageBytes = new byte[0];
        String contentType = "application/octet-stream"; // Domyślny bezpieczny typ

        if (userOpt.isPresent() && userOpt.get().getAvatar() != null) {
            imageBytes = userOpt.get().getAvatar();

            // Pobieramy typ pliku z bazy
            if (userOpt.get().getAvatarContentType() != null) {
                contentType = userOpt.get().getAvatarContentType();
            }
        }

        com.qiu.grpc.DownloadAvatarResponse response = com.qiu.grpc.DownloadAvatarResponse.newBuilder()
                .setImageData(com.google.protobuf.ByteString.copyFrom(imageBytes))
                .setContentType(contentType) // Zwracamy typ do Go
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void verifyUser(com.qiu.grpc.LoginRequest request, StreamObserver<com.qiu.grpc.LoginResponse> responseObserver) {
        try {
            // 1. Weryfikacja użytkownika
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // 2. Sprawdzenie hasła (zakładam, że passwordEncoder jest dostępny)
            if (userDetails != null && passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {

                // Pobranie ID użytkownika (zakładam, że Twoja encja User posiada metodę getId())
                // Musisz rzutować UserDetails na swoją klasę encji lub wyszukać ją w repozytorium
                Optional<User> user = userRepository.findByUsername(request.getUsername());

                final String jwtToken = jwtService.generateToken(userDetails);

                com.qiu.grpc.LoginResponse response = com.qiu.grpc.LoginResponse.newBuilder()
                        .setToken(jwtToken)
                        .setSuccess(true)
                        .setMessage("Logowanie poprawne")
                        .build();

                responseObserver.onNext(response);
            } else {
                responseObserver.onNext(com.qiu.grpc.LoginResponse.newBuilder().setSuccess(false).build());
            }
        } catch (Exception e) {
            responseObserver.onNext(com.qiu.grpc.LoginResponse.newBuilder().setSuccess(false).build());
        }
        responseObserver.onCompleted();
    }
}