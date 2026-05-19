package be.thespattt.zoneGuard.model;

public enum FlagState {
    ALLOW,
    DENY,
    UNSET;

    public static FlagState fromInput(String input) {
        return switch (input.toLowerCase()) {
            case "allow", "true", "yes", "on" -> ALLOW;
            case "deny", "false", "no", "off" -> DENY;
            case "unset", "default", "remove" -> UNSET;
            default -> throw new IllegalArgumentException("État de flag inconnu : " + input);
        };
    }
}
