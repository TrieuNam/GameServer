package com.SouthMillion.crafting_service.grpc;

import com.SouthMillion.crafting_service.service.CraftingService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.dto.crafting.CraftingDTOs;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.grpc.crafting.*;

import java.time.Instant;
import java.util.List;

/**
 * gRPC Server cho crafting-service.
 *
 * Lớp này CHỈ chuyển đổi gRPC ↔ DTO rồi delegate sang CraftingService.
 * Mọi logic nghiệp vụ và quyết định dùng gRPC hay REST đều nằm trong CraftingService.
 *
 * KHÔNG inject BagFeign hay WalletFeign tại đây — tránh trộn lẫn protocol trong gRPC handler.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CraftingServiceGrpcImpl extends CraftingServiceGrpc.CraftingServiceImplBase {

    private final CraftingService craftingService;

    @Override
    public void getRecipes(GetRecipesRequest request, StreamObserver<RecipesResponse> responseObserver) {
        try {
            log.info("[grpc-crafting] GetRecipes: roleId={}, level={}",
                    request.getRoleId(), request.getLevelFilter());

            // CraftingService tự lấy bag counts qua gRPC và điền currentAmount vào từng Material
            List<CraftingDTOs.RecipeInfo> recipeInfos = craftingService.getRecipes(
                    String.valueOf(request.getRoleId()),
                    request.getLevelFilter() > 0 ? request.getLevelFilter() : null
            );

            RecipesResponse.Builder responseBuilder = RecipesResponse.newBuilder();
            for (CraftingDTOs.RecipeInfo info : recipeInfos) {
                Recipe.Builder recipeBuilder = Recipe.newBuilder()
                        .setRecipeId(info.getRecipeId() != null ? info.getRecipeId() : 0)
                        .setRecipeName(info.getRecipeName() != null ? info.getRecipeName() : "")
                        .setResultItemId(info.getResultItemId() != null ? info.getResultItemId() : 0)
                        .setResultAmount(info.getResultAmount() != null ? info.getResultAmount() : 1)
                        .setCraftTimeSeconds(info.getCraftTime() != null ? info.getCraftTime() : 0L)
                        .setRequiredLevel(info.getRequiredLevel() != null ? info.getRequiredLevel() : 1)
                        .setCoinCost(info.getCoinCost() != null ? info.getCoinCost() : 0L)
                        .setCanCraft(Boolean.TRUE.equals(info.getCanCraft()));

                if (info.getMaterials() != null) {
                    for (CraftingDTOs.Material mat : info.getMaterials()) {
                        recipeBuilder.addMaterials(Material.newBuilder()
                                .setItemId(mat.getItemId() != null ? mat.getItemId() : 0)
                                .setRequiredAmount(mat.getAmount() != null ? mat.getAmount() : 0)
                                // currentAmount đã được CraftingService điền qua gRPC → bag-service
                                .setCurrentAmount(mat.getCurrentAmount() != null ? mat.getCurrentAmount() : 0)
                                .build());
                    }
                }
                responseBuilder.addRecipes(recipeBuilder.build());
            }

            responseBuilder.setStatus(ResponseStatus.newBuilder()
                    .setCode(200).setSuccess(true).build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("[grpc-crafting] GetRecipes success: count={}", recipeInfos.size());

        } catch (Exception e) {
            log.error("[grpc-crafting] GetRecipes failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void startCraft(StartCraftRequest request, StreamObserver<StartCraftResponse> responseObserver) {
        try {
            log.info("[grpc-crafting] StartCraft: roleId={}, recipeId={}, count={}",
                    request.getRoleId(), request.getRecipeId(), request.getCount());

            CraftingDTOs.CraftResponse craftResponse = craftingService.startCraft(
                    CraftingDTOs.CraftRequest.builder()
                            .roleId(String.valueOf(request.getRoleId()))
                            .recipeId(request.getRecipeId())
                            .count(request.getCount())
                            .build());

            StartCraftResponse.Builder responseBuilder = StartCraftResponse.newBuilder()
                    .setSuccess(craftResponse.getSuccess())
                    .setMessage(craftResponse.getMessage() != null ? craftResponse.getMessage() : "");

            if (craftResponse.getCraftingId() != null) {
                responseBuilder.setCraftingId(craftResponse.getCraftingId());
            }
            if (craftResponse.getEndTime() != null) {
                responseBuilder.setStartTime(Instant.now().getEpochSecond());
                responseBuilder.setEndTime(craftResponse.getEndTime());
            }

            responseBuilder.setStatus(ResponseStatus.newBuilder()
                    .setCode(craftResponse.getSuccess() ? 200 : 400)
                    .setSuccess(craftResponse.getSuccess())
                    .setMessage(craftResponse.getMessage() != null ? craftResponse.getMessage() : "")
                    .build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("[grpc-crafting] StartCraft success: craftingId={}", craftResponse.getCraftingId());

        } catch (Exception e) {
            log.error("[grpc-crafting] StartCraft failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCraftingStatus(GetCraftingStatusRequest request, StreamObserver<CraftingStatusResponse> responseObserver) {
        try {
            log.info("[grpc-crafting] GetCraftingStatus: roleId={}", request.getRoleId());

            List<CraftingDTOs.CraftingStatus> statusList = craftingService.getStatus(
                    String.valueOf(request.getRoleId()));

            CraftingStatusResponse.Builder responseBuilder = CraftingStatusResponse.newBuilder();
            for (CraftingDTOs.CraftingStatus status : statusList) {
                long endTime = status.getEndTime() != null ? status.getEndTime() : 0L;
                responseBuilder.addCraftingList(CraftingStatus.newBuilder()
                        .setCraftingId(status.getCraftingId() != null ? status.getCraftingId() : 0L)
                        .setRecipeId(status.getRecipeId() != null ? status.getRecipeId() : 0)
                        .setStartTime(status.getStartTime() != null ? status.getStartTime() : 0L)
                        .setEndTime(endTime)
                        .setCompleted(Instant.now().getEpochSecond() >= endTime)
                        .setCanClaim(Boolean.TRUE.equals(status.getCanClaim()))
                        .setStatusText(status.getStatus() != null ? status.getStatus() : "UNKNOWN")
                        .build());
            }

            responseBuilder.setStatus(ResponseStatus.newBuilder()
                    .setCode(200).setSuccess(true).build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("[grpc-crafting] GetCraftingStatus success: count={}", statusList.size());

        } catch (Exception e) {
            log.error("[grpc-crafting] GetCraftingStatus failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void claimCrafting(ClaimCraftingRequest request, StreamObserver<ClaimCraftingResponse> responseObserver) {
        try {
            log.info("[grpc-crafting] ClaimCrafting: roleId={}, craftingId={}",
                    request.getRoleId(), request.getCraftingId());

            CraftingDTOs.ClaimResponse claimResponse = craftingService.claim(
                    CraftingDTOs.ClaimRequest.builder()
                            .roleId(String.valueOf(request.getRoleId()))
                            .craftingId(request.getCraftingId())
                            .build());

            ClaimCraftingResponse.Builder responseBuilder = ClaimCraftingResponse.newBuilder()
                    .setSuccess(claimResponse.getSuccess())
                    .setMessage(claimResponse.getMessage() != null ? claimResponse.getMessage() : "");

            if (claimResponse.getRewards() != null) {
                for (CraftingDTOs.RewardItem reward : claimResponse.getRewards()) {
                    responseBuilder.addRewards(RewardItem.newBuilder()
                            .setItemId(reward.getItemId() != null ? reward.getItemId() : 0)
                            .setAmount(reward.getAmount() != null ? reward.getAmount() : 0)
                            .build());
                }
            }

            responseBuilder.setStatus(ResponseStatus.newBuilder()
                    .setCode(claimResponse.getSuccess() ? 200 : 400)
                    .setSuccess(claimResponse.getSuccess())
                    .setMessage(claimResponse.getMessage() != null ? claimResponse.getMessage() : "")
                    .build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            log.info("[grpc-crafting] ClaimCrafting success");

        } catch (Exception e) {
            log.error("[grpc-crafting] ClaimCrafting failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void cancelCrafting(CancelCraftingRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            log.info("[grpc-crafting] CancelCrafting: roleId={}, craftingId={}",
                    request.getRoleId(), request.getCraftingId());

            CraftingDTOs.CancelResponse cancelResp = craftingService.cancel(
                    CraftingDTOs.CancelRequest.builder()
                            .roleId(String.valueOf(request.getRoleId()))
                            .craftingId(request.getCraftingId())
                            .build());

            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(cancelResp.getSuccess() ? 200 : 400)
                    .setSuccess(cancelResp.getSuccess())
                    .setMessage(cancelResp.getMessage() != null ? cancelResp.getMessage() : "")
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[grpc-crafting] CancelCrafting failed", e);
            responseObserver.onError(e);
        }
    }
}

