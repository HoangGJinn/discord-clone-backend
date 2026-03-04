package com.discordclone.backend.utils.agora;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class AccessToken2 {
    public enum kPrivilege {
        kJoinChannel(1),
        kPublishAudioStream(2),
        kPublishVideoStream(3),
        kPublishDataStream(4),
        kPublishAudioCdn(5),
        kPublishVideoCdn(6),
        kRequestPublishAudioStream(7),
        kRequestPublishVideoStream(8),
        kRequestPublishDataStream(9),
        kInvitePublishAudioStream(10),
        kInvitePublishVideoStream(11),
        kInvitePublishDataStream(12),
        kAdministrateChannel(101),
        kRtmLogin(1000);

        public short intValue;

        kPrivilege(int value) {
            intValue = (short) value;
        }
    }

    private static final int HMAC_SHA256_LENGTH = 32;
    private static final int VERSION_LENGTH = 3;
    private static final int APP_ID_LENGTH = 32;

    public String appId;
    public String appCertificate;
    public int issueTs;
    public int expireTs;
    public int salt;
    public Map<Short, Integer> privileges;

    private static final String VERSION = "007";

    public AccessToken2(String appId, String appCertificate, int issueTs, int expireTs) {
        this.appId = appId;
        this.appCertificate = appCertificate;
        this.issueTs = issueTs;
        this.expireTs = expireTs;
        this.salt = (int) (Math.random() * 99999999) + 1;
        this.privileges = new TreeMap<>();
    }

    public void addPrivilege(kPrivilege privilege, int expireTs) {
        privileges.put(privilege.intValue, expireTs);
    }

    public String build() throws Exception {
        if (!isUUID(appId) || !isUUID(appCertificate)) {
            return "";
        }

        byte[] signing = getSigning();
        ByteBuf buf = new ByteBuf()
                .putString(appId).putInt(issueTs).putInt(expireTs).putInt(salt).putShortMap(privileges);
        byte[] message = buf.asBytes();
        byte[] signature = hmacSign(signing, message);

        byte[] info = new ByteBuf().putBytes(signature).putBytes(message).asBytes();

        Deflater deflater = new Deflater();
        deflater.setInput(info);
        deflater.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            bos.write(buffer, 0, count);
        }
        bos.close();
        byte[] compressed = bos.toByteArray();

        return VERSION + Base64.getEncoder().encodeToString(compressed);
    }

    private byte[] getSigning() throws Exception {
        return hmacSign(appCertificate.getBytes(), Integer.toString(issueTs).getBytes());
    }

    private boolean isUUID(String uuid) {
        if (uuid == null || uuid.length() != 32) {
            return false;
        }
        return uuid.matches("[0-9a-fA-F]*");
    }

    private byte[] hmacSign(byte[] key, byte[] message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(message);
    }

    public static class ByteBuf {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public ByteBuf putShort(short v) {
            baos.write((v >>> 8) & 0xFF);
            baos.write(v & 0xFF);
            return this;
        }

        public ByteBuf putInt(int v) {
            baos.write((v >>> 24) & 0xFF);
            baos.write((v >>> 16) & 0xFF);
            baos.write((v >>> 8) & 0xFF);
            baos.write(v & 0xFF);
            return this;
        }

        public ByteBuf putBytes(byte[] v) {
            putShort((short) v.length);
            baos.writeBytes(v);
            return this;
        }

        public ByteBuf putString(String v) {
            return putBytes(v.getBytes());
        }

        public ByteBuf putShortMap(Map<Short, Integer> map) {
            putShort((short) map.size());
            for (Map.Entry<Short, Integer> entry : map.entrySet()) {
                putShort(entry.getKey());
                putInt(entry.getValue());
            }
            return this;
        }

        public byte[] asBytes() {
            return baos.toByteArray();
        }
    }
}
