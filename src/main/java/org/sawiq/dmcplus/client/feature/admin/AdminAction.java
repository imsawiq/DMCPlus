package org.sawiq.dmcplus.client.feature.admin;

public enum AdminAction {
    TEMP_BAN("Бан"),
    TEXT_MUTE("Мут"),
    VOICE_MUTE("Войс-мут");

    private final String displayName;

    AdminAction(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
