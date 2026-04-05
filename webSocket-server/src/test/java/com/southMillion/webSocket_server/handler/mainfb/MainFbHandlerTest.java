package com.SouthMillion.webSocket_server.handler.mainfb;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.service.grpc.MainFbGrpcClient;
import org.SouthMillion.proto.mainfb.GetProgressResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainFbHandlerTest {

    @Mock
    private MainFbGrpcClient mainFbGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private BagFeign bagFeign;
    @Mock
    private WalletHttpClient walletHttpClient;

    @InjectMocks
    private MainFbHandler mainFbHandler;

    @Test
    void handleClaimReward_refreshesBagAndWallet() {
        PlayerSession session = org.mockito.Mockito.mock(PlayerSession.class);
        when(mainFbGrpcClient.getProgress("2001")).thenReturn(GetProgressResponse.newBuilder().build());
        when(bagFeign.list("2001")).thenReturn(List.of());
        when(walletHttpClient.info("2001")).thenReturn(null);

        ReflectionTestUtils.invokeMethod(mainFbHandler, "handleClaimReward", session, 2001L);

        verify(mainFbGrpcClient).claimChapterReward("2001", 1);
        verify(bagFeign, atLeastOnce()).list("2001");
        verify(walletHttpClient, atLeastOnce()).info("2001");
    }
}
