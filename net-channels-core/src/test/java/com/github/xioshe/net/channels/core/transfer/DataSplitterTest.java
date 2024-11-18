package com.github.xioshe.net.channels.core.transfer;

import com.github.xioshe.net.channels.core.codec.DataCompressor;
import com.github.xioshe.net.channels.core.crypto.AESCipher;
import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import com.github.xioshe.net.channels.core.protocol.QRCodeProtocol;
import com.github.xioshe.net.channels.core.session.SessionManager;
import com.github.xioshe.net.channels.core.session.TransferSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSplitterTest {

    @Mock
    private SessionManager sessionManager;
    @Mock
    private QRCodeProtocol protocol;
    @Mock
    private DataCompressor compressor;
    @Mock
    private AESCipher cipher;
    @Mock
    private TransferDataCache<List<String>> dataCache;

    private DataSplitter splitter;
    private static final String TEST_SESSION_ID = "test-session-1";
    private static final String TEST_DATA = "test-data-content";
    private static final int MAX_QR_SIZE = 2048;

    @BeforeEach
    void setUp() {
        splitter = DataSplitter.builder()
                .sessionManager(sessionManager)
                .protocol(protocol)
                .compressor(compressor)
                .cipher(cipher)
                .dataCache(dataCache)
                .maxQRDataSize(MAX_QR_SIZE)
                .build();
    }

    @Test
    void shouldSplitDataSuccessfully() throws Exception {
        // given
        String compressedData = "compressed-data";
        String encryptedData = "encrypted-data";
        when(compressor.compress(TEST_DATA)).thenReturn(compressedData);
        when(cipher.encrypt(compressedData)).thenReturn(encryptedData);
        when(protocol.packetToQRCode(any())).thenReturn("qr-code-data");
        when(sessionManager.createSession(anyString(), anyInt(), anyInt()))
                .thenReturn(new TransferSession(TEST_SESSION_ID, 1, encryptedData.length()));

        // when
        List<String> result = splitter.split(TEST_DATA, TEST_SESSION_ID);

        // then
        assertThat(result).isNotEmpty();
        verify(dataCache).store(eq(TEST_SESSION_ID), anyList());
        verify(sessionManager).createSession(TEST_SESSION_ID, 1, encryptedData.length());
    }

    @Test
    void shouldThrowExceptionWhenDataIsEmpty() {
        assertThatThrownBy(() -> splitter.split("", TEST_SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data cannot be null or empty");
    }

    @Test
    void shouldRetransmitChunksSuccessfully() {
        // given
        List<String> cachedPackets = Arrays.asList("packet1", "packet2", "packet3");
        List<Integer> requestedChunks = Arrays.asList(0, 2);
        when(dataCache.get(TEST_SESSION_ID)).thenReturn(Optional.of(cachedPackets));
        when(sessionManager.getSession(TEST_SESSION_ID))
                .thenReturn(new TransferSession(TEST_SESSION_ID, 3, 100));

        // when
        List<String> result = splitter.retransmit(TEST_SESSION_ID, requestedChunks);

        // then
        assertThat(result)
                .hasSize(2)
                .containsExactly("packet1", "packet3");
    }

    @Test
    void shouldThrowExceptionWhenSessionDataNotFound() {
        // given
        when(dataCache.get(TEST_SESSION_ID)).thenReturn(Optional.empty());
        when(sessionManager.getSession(TEST_SESSION_ID))
                .thenReturn(new TransferSession(TEST_SESSION_ID, 3, 100));

        // then
        assertThatThrownBy(() -> splitter.retransmit(TEST_SESSION_ID, List.of(0)))
                .isInstanceOf(NetChannelsException.class)
                .hasMessageContaining("Session data not found");
    }

//    @Test
//    void shouldThrowExceptionWhenPacketExceedsSize() throws Exception {
//        // given
//        String largeData = "x".repeat(MAX_QR_SIZE * 2);
//        when(compressor.compress(largeData)).thenReturn(largeData);
//        when(cipher.encrypt(largeData)).thenReturn(largeData);
//        when(sessionManager.createSession(anyString(), anyInt(), anyInt()))
//                .thenReturn(new TransferSession(TEST_SESSION_ID, 1, largeData.length()));
//
//        // then
//        assertThatThrownBy(() -> splitter.split(largeData, TEST_SESSION_ID))
//                .isInstanceOf(NetChannelsException.class)
//                .hasMessageContaining("Packet size exceeds QR code capacity");
//    }

    @Test
    void shouldCleanupSuccessfully() {
        // when
        splitter.cleanup(TEST_SESSION_ID);

        // then
        verify(dataCache).remove(TEST_SESSION_ID);
    }
}