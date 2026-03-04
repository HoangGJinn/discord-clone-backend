package com.discordclone.backend.utils.agora;

public class RtcTokenBuilder2 {
    public enum Role {
        ROLE_PUBLISHER(1),
        ROLE_SUBSCRIBER(2);

        public int intValue;

        Role(int value) {
            intValue = value;
        }
    }

    /**
     * Build the RTC token with uid.
     *
     * @param appId           The App ID issued to you by Agora. Apply for a new App
     *                        ID from
     *                        Agora Dashboard if it is missing from your kit. See
     *                        Get an App ID.
     * @param appCertificate  Certificate of the application that you registered in
     *                        the Agora Dashboard. See Get an App Certificate.
     * @param channelName     Unique channel name for the AgoraRTC session in the
     *                        string format
     * @param uid             User ID. A 32-bit unsigned integer with a value
     *                        ranging from
     *                        1 to (2^32-1). uid must be unique.
     * @param role            Role_Publisher = 1: A broadcaster (host) in a
     *                        live-broadcast profile.
     *                        Role_Subscriber = 2: (Default) A audience in a
     *                        live-broadcast profile.
     * @param tokenExpire     represented by the number of seconds elapsed since
     *                        1/1/1970. If, for example, you want to access the
     *                        Agora Service within 10 minutes after the token is
     *                        generated, set tokenExpire as the current time stamp
     *                        + 600 (seconds).
     * @param privilegeExpire represented by the number of seconds elapsed since
     *                        1/1/1970.
     *                        If, for example, you want to enable your privilege for
     *                        10
     *                        minutes, set privilegeExpire as the current time stamp
     *                        + 600 (seconds).
     * @return The RTC token.
     */
    public String buildTokenWithUid(String appId, String appCertificate,
            String channelName, int uid, Role role, int tokenExpire, int privilegeExpire) {
        return buildTokenWithUserAccount(appId, appCertificate, channelName, String.valueOf(uid), role,
                tokenExpire, privilegeExpire);
    }

    /**
     * Build the RTC token with account.
     *
     * @param appId           The App ID issued to you by Agora. Apply for a new App
     *                        ID from
     *                        Agora Dashboard if it is missing from your kit. See
     *                        Get an App ID.
     * @param appCertificate  Certificate of the application that you registered in
     *                        the Agora Dashboard. See Get an App Certificate.
     * @param channelName     Unique channel name for the AgoraRTC session in the
     *                        string format
     * @param account         The user account.
     * @param role            Role_Publisher = 1: A broadcaster (host) in a
     *                        live-broadcast profile.
     *                        Role_Subscriber = 2: (Default) A audience in a
     *                        live-broadcast profile.
     * @param tokenExpire     represented by the number of seconds elapsed since
     *                        1/1/1970. If, for example, you want to access the
     *                        Agora Service within 10 minutes after the token is
     *                        generated, set tokenExpire as the current time stamp
     *                        + 600 (seconds).
     * @param privilegeExpire represented by the number of seconds elapsed since
     *                        1/1/1970.
     *                        If, for example, you want to enable your privilege for
     *                        10
     *                        minutes, set privilegeExpire as the current time stamp
     *                        + 600 (seconds).
     * @return The RTC token.
     */
    public String buildTokenWithUserAccount(String appId, String appCertificate,
            String channelName, String account, Role role, int tokenExpire, int privilegeExpire) {
        try {
            AccessToken2 accessToken = new AccessToken2(appId, appCertificate, 0, tokenExpire);
            accessToken.salt = 1;
            accessToken.addPrivilege(AccessToken2.kPrivilege.kJoinChannel, privilegeExpire);
            if (role == Role.ROLE_PUBLISHER) {
                accessToken.addPrivilege(AccessToken2.kPrivilege.kPublishAudioStream, privilegeExpire);
                accessToken.addPrivilege(AccessToken2.kPrivilege.kPublishVideoStream, privilegeExpire);
                accessToken.addPrivilege(AccessToken2.kPrivilege.kPublishDataStream, privilegeExpire);
            }
            return accessToken.build();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
