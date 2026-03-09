package com.discordclone.backend.utils.agora;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;

/**
 * Agora AccessToken2 (version 007) builder.
 * Little-endian byte order, HMAC-SHA256 signing.
 * Ref: https://github.com/AgoraIO/Tools/tree/master/DynamicKey/AgoraDynamicKey/java
 */
public class AccessToken2 {

    public enum kPrivilege {
        kJoinChannel(1),
        kPublishAudioStream(2),
        kPublishVideoStream(3),
        kPublishDataStream(4),
        kAdministrateChannel(101),
        kRtmLogin(1000);

        public final short intValue;
        kPrivilege(int value) { this.intValue = (short) value; }
    }

    private static final String VERSION = "007";

    public String appId;
    public String appCertificate;
    public int issueTs;
    public int expireTs;
    public int salt;
    public Map<Short, Integer> privileges;

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
        if (appId == null || appId.length() != 32) return "";
        if (appCertificate == null || appCertificate.length() != 32) return "";

        // signing = HMAC-SHA256(appCertificate, appId + issueTs_LE + salt_LE)
        ByteBuf signBuf = new ByteBuf();
        signBuf.putRawBytes(appId.getBytes());
        signBuf.putUint32(issueTs);
        signBuf.putUint32(salt);
        byte[] signing = hmacSign(appCertificate.getBytes(), signBuf.asBytes());

        // message = appId + issueTs_LE + expireTs_LE + salt_LE + privileges
        ByteBuf msgBuf = new ByteBuf();
        msgBuf.putRawBytes(appId.getBytes());
        msgBuf.putUint32(issueTs);
        msgBuf.putUint32(expireTs);
        msgBuf.putUint32(salt);
        msgBuf.putUint16((short) privileges.size());
        for (Map.Entry<Short, Integer> entry : privileges.entrySet()) {
            msgBuf.putUint16(entry.getKey());
            msgBuf.putUint32(entry.getValue());
        }
        byte[] message = msgBuf.asBytes();

        // signature = HMAC-SHA256(signing, message)
        byte[] signature = hmacSign(signing, message);

        // token = VERSION + Base64(compress(uint16LE(sig_len) + signature + message))
        ByteBuf tokenBuf = new ByteBuf();
        tokenBuf.putUint16((short) signature.length);
        tokenBuf.putRawBytes(signature);
        tokenBuf.putRawBytes(message);
        byte[] compressed = compress(tokenBuf.asBytes());

        return VERSION + Base64.getEncoder().encodeToString(compressed);
    }

    private byte[] hmacSign(byte[] key, byte[] message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(message);
    }

    private byte[] compress(byte[] input) throws Exception {
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            bos.write(buffer, 0, deflater.deflate(buffer));
        }
        bos.close();
        return bos.toByteArray();
    }

    // ─── Little-endian ByteBuf ───────────────────────────────────────────────

    public static class ByteBuf {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public ByteBuf putRawBytes(byte[] v) {
            baos.writeBytes(v);
            return this;
        }

        public ByteBuf putBytes(byte[] v) {
            putUint16((short) v.length);
            baos.writeBytes(v);
            return this;
        }

        public ByteBuf putUint16(short v) {
            baos.write(v & 0xFF);
            baos.write((v >>> 8) & 0xFF);
            return this;
        }

        public ByteBuf putUint32(int v) {
            baos.write(v & 0xFF);
            baos.write((v >>> 8) & 0xFF);
            baos.write((v >>> 16) & 0xFF);
            baos.write((v >>> 24) & 0xFF);
            return this;
        }

        public byte[] asBytes() {
            return baos.toByteArray();
        }
    }
}
