package example.practice.story;

public class CharacterData {
    public String name;
    public String title;
    public String description;

    public CharacterData(String name, String title, String description) {
        this.name = name;
        this.title = title;
        this.description = description;
    }

    // Static array holding all characters, just like characters.h
    public static final CharacterData[] CHARACTERS = {
            new CharacterData("Kaelen Duskbane", "The Mercenary", "A young mercenary with cold gray eyes..."),
            new CharacterData("Lyra Veylen", "The Fugitive Healer", "A skilled healer on the run..."),
            new CharacterData("Bram Thorne", "The Disgraced Knight", "Once a decorated knight of the King's Guard..."),
            new CharacterData("Iriah Sable", "The Rogue", "A rogue whose sharp wit is matched only by...")
    };
}