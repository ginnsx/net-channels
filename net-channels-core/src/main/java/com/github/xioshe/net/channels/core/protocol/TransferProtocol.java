package com.github.xioshe.net.channels.core.protocol;


import com.github.xioshe.net.channels.core.model.TransferPacket;

public interface TransferProtocol {
    /**
     * 将数据包转换为二维码数据
     */
    String packetToQRCode(TransferPacket packet);

    /**
     * 从二维码数据解析数据包
     */
    TransferPacket qrCodeToPacket(String qrCodeData);

    /**
     * 计算数据校验和
     */
    String calculateChecksum(byte[] data);

    /**
     * 验证数据包完整性
     */
    boolean validatePacket(TransferPacket packet);
}