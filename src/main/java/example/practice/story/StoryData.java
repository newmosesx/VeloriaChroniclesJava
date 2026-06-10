package example.practice.story;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The story, loaded from story.json - the single authoring surface. You write
// chapters, dialogue, choices, routes, and character reveals in JSON; the code
// never needs to change to continue the story.
//
// SCHEMA (everything optional except title + paragraphs):
// {
//   "chapters": [
//     {
//       "id": "ch9_council",            // stable handle for routing (default: "ch<N>")
//       "title": "The Kingdom suffers",
//       "mode": "debate",               // "story" (default) or "debate" -> which panel
//       "next": "ch10_aftermath",       // chapter after this one (default: the next entry)
//       "paragraphs": [
//         { "speaker": "House Speaker", "text": "\"Order!\"" },
//         "Bare strings are allowed - the speaker defaults to Narrator."
//       ],
//       "choiceAt": 18,                 // paragraph index where choices appear
//       "timeout": { "token": "SILENCE", "response": "... (the Emperor stammers)" },
//       "choices": [
//         { "label": "1. \"I was securing the future!\" (Authority)",
//           "token": "AUTHORITY",
//           "response": "I was securing the future! We did what was necessary.",
//           "effects": ["gold:-25000", "limitersOff", "desertion:0.10", "morale:-5"],
//           "goto": "route_authority" }
//       ]
//     }
//   ],
//   "characters": [
//     { "name": "Kaelen Duskbane", "subtitle": "The Mercenary",
//       "chapter": "ch1", "paragraph": 1 }
//   ]
// }
//
// ROUTING: a choice's "goto" jumps to that chapter id (consumed by StoryRouter);
// a chapter's "next" decides where the Next-Chapter button leads. Branches
// reconverge by pointing their "next" at the same chapter. Omit both and the
// story is linear, exactly as before.
//
// The file is looked for at: ./story.json, then ./src/main/resources/story.json,
// then on the classpath as /story.json. If none exists, a single explanatory
// chapter loads so the game still boots.
public class StoryData {

    public static class Chapter {
        public String title;
        public String[] paragraphs;
        public String[] perspectives;
        // --- authoring metadata (new) ---
        public String id = "";
        public String mode = "story";          // "story" | "debate"
        public String nextId = null;           // null -> the following entry
        public int choiceAt = -1;              // -1 -> no choice point
        public List<ChoiceSpec> choices = new ArrayList<>();
        public TimeoutSpec timeout = null;

        public Chapter(String title, String[] paragraphs, String[] perspectives) {
            this.title = title;
            this.paragraphs = paragraphs;
            this.perspectives = perspectives;
        }
    }

    // One authored choice: what the button says, what the Emperor speaks (token),
    // what lands in the chat (response), mechanical fallout, and where it routes.
    public static class ChoiceSpec {
        public String label = "";
        public String token = null;            // DialogueToken name, e.g. "AUTHORITY"
        public String response = "";
        public List<String> effects = new ArrayList<>();
        public String gotoId = null;
    }

    public static class TimeoutSpec {
        public String token = "SILENCE";
        public String response = "... (The Emperor stammers in silence)";
        public String gotoId = null;
    }

    public static class CharacterBeat {
        public String name, subtitle;
        public int chapterIndex, paragraph;
    }

    public static final List<Chapter> CHAPTERS = new ArrayList<>();
    private static final List<CharacterBeat> CHARACTER_BEATS = new ArrayList<>();

    static { load(); }

    // ------------------------------------------------------------- loading
    private static void load() {
        String text = readStoryFile();
        if (text == null) {
            CHAPTERS.add(new Chapter("story.json not found",
                    new String[]{ "Place story.json in the project root (next to where you run the game), "
                            + "in src/main/resources/, or on the classpath. "
                            + "Run tools/StoryExport once to generate it from the old hardcoded story." },
                    new String[]{ "Narrator" }));
            return;
        }
        try {
            parseStory(text);
        } catch (Exception ex) {
            CHAPTERS.clear();
            CHAPTERS.add(new Chapter("story.json failed to load",
                    new String[]{ String.valueOf(ex.getMessage()) },
                    new String[]{ "Narrator" }));
        }
    }

    private static String readStoryFile() {
        try {
            File f = new File("story.json");
            if (f.isFile()) return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            f = new File("src/main/resources/story.json");
            if (f.isFile()) return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            InputStream in = StoryData.class.getResourceAsStream("/story.json");
            if (in != null) {
                byte[] all = in.readAllBytes();
                in.close();
                return new String(all, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static void parseStory(String text) {
        Map<String, Object> root = Json.obj(Json.parse(text));

        int n = 0;
        for (Object chO : Json.arrOf(root, "chapters")) {
            Map<String, Object> c = Json.obj(chO);
            n++;

            List<Object> paras = Json.arrOf(c, "paragraphs");
            String[] texts = new String[paras.size()];
            String[] speakers = new String[paras.size()];
            for (int p = 0; p < paras.size(); p++) {
                Object po = paras.get(p);
                if (po instanceof String) {              // bare string = Narrator line
                    texts[p] = (String) po;
                    speakers[p] = "Narrator";
                } else {
                    Map<String, Object> pm = Json.obj(po);
                    texts[p] = Json.str(pm, "text", "");
                    speakers[p] = Json.str(pm, "speaker", "Narrator");
                }
            }

            Chapter ch = new Chapter(Json.str(c, "title", "Untitled"), texts, speakers);
            ch.id = Json.str(c, "id", "ch" + n);
            ch.mode = Json.str(c, "mode", "story");
            ch.nextId = Json.str(c, "next", null);
            ch.choiceAt = Json.intVal(c, "choiceAt", -1);

            Map<String, Object> to = Json.objOf(c, "timeout");
            if (to != null) {
                TimeoutSpec t = new TimeoutSpec();
                t.token = Json.str(to, "token", "SILENCE");
                t.response = Json.str(to, "response", t.response);
                t.gotoId = Json.str(to, "goto", null);
                ch.timeout = t;
            }

            for (Object choO : Json.arrOf(c, "choices")) {
                Map<String, Object> cm = Json.obj(choO);
                ChoiceSpec cs = new ChoiceSpec();
                cs.label = Json.str(cm, "label", "(unlabelled choice)");
                cs.token = Json.str(cm, "token", null);
                cs.response = Json.str(cm, "response", "");
                cs.gotoId = Json.str(cm, "goto", null);
                for (Object e : Json.arrOf(cm, "effects"))
                    if (e instanceof String) cs.effects.add((String) e);
                ch.choices.add(cs);
            }
            CHAPTERS.add(ch);
        }

        for (Object cbO : Json.arrOf(root, "characters")) {
            Map<String, Object> cm = Json.obj(cbO);
            CharacterBeat b = new CharacterBeat();
            b.name = Json.str(cm, "name", "");
            b.subtitle = Json.str(cm, "subtitle", "");
            b.paragraph = Json.intVal(cm, "paragraph", 0);
            b.chapterIndex = indexOfId(Json.str(cm, "chapter", ""));
            if (b.chapterIndex >= 0 && !b.name.isEmpty()) CHARACTER_BEATS.add(b);
        }
    }

    // ------------------------------------------------------------- old API
    public static String getParagraph(int ch, int p) {
        if (ch >= CHAPTERS.size() || p >= CHAPTERS.get(ch).paragraphs.length) return "End of Story.";
        return CHAPTERS.get(ch).paragraphs[p];
    }

    public static String getPerspective(int ch, int p) {
        if (ch >= CHAPTERS.size() || p >= CHAPTERS.get(ch).perspectives.length) return "Narrator";
        return CHAPTERS.get(ch).perspectives[p];
    }

    // ------------------------------------------------------------- new API
    public static Chapter chapterAt(int ch) {
        return (ch >= 0 && ch < CHAPTERS.size()) ? CHAPTERS.get(ch) : null;
    }

    public static boolean isDebate(int ch) {
        Chapter c = chapterAt(ch);
        return c != null && "debate".equalsIgnoreCase(c.mode);
    }

    public static int indexOfId(String id) {
        if (id == null) return -1;
        for (int i = 0; i < CHAPTERS.size(); i++)
            if (id.equals(CHAPTERS.get(i).id)) return i;
        return -1;
    }

    // Where the Next-Chapter button leads from here; -1 = the story ends here.
    public static int nextIndexOf(int ch) {
        Chapter c = chapterAt(ch);
        if (c == null) return -1;
        if (c.nextId != null) return indexOfId(c.nextId);
        return (ch + 1 < CHAPTERS.size()) ? ch + 1 : -1;
    }

    public static List<CharacterBeat> characterBeats() { return CHARACTER_BEATS; }
}