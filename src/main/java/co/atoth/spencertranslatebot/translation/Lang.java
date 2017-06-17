package co.atoth.spencertranslatebot.translation;

public enum Lang {
    IW("il"), RO("ro");

    public String country;

    private Lang(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}