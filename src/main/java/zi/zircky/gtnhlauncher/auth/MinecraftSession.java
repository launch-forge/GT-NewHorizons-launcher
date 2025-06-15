package zi.zircky.gtnhlauncher.auth;

public record MinecraftSession(
    String uuid,
    String username,
    String accessToken,
    String refreshToken
) {
}
