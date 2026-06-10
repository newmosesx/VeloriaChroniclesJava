package example.practice.tools;

import example.practice.story.Json;
import example.practice.story.StoryData;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

// ONE-SHOT CONVERTER. Run this MAIN once, BEFORE replacing StoryData.java, while
// the old hardcoded chapters are still in it. It writes story.json with every
// chapter verbatim, marks the council chapter (index 8) as a debate with its
// three choices wired to three ROUTE chapter stubs it also creates, and exports
// the character reveal beats. Then replace StoryData/DebateManager with the new
// versions, and from that day on you only ever edit story.json.
//
// Run from the project root (so story.json lands next to your run directory):
//   IntelliJ: right-click StoryExport -> Run main()
public class StoryExport {

    public static void main(String[] args) throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("{\n  \"version\": 1,\n  \"chapters\": [\n");

        int n = StoryData.CHAPTERS.size();
        for (int i = 0; i < n; i++) {
            StoryData.Chapter ch = StoryData.CHAPTERS.get(i);
            b.append("    {\n");
            b.append("      \"id\": \"ch").append(i + 1).append("\",\n");
            b.append("      \"title\": \"").append(Json.escape(ch.title)).append("\",\n");
            if (i == 8) {
                // the council chapter: debate mode + the three current choices,
                // each routed to a stub chapter appended below.
                b.append("      \"mode\": \"debate\",\n");
                b.append("      \"choiceAt\": 18,\n");
                b.append("      \"timeout\": { \"token\": \"SILENCE\", \"response\": \"... (The Emperor stammers in silence)\" },\n");
                b.append("      \"choices\": [\n");
                b.append("        { \"label\": \"1. \\\"I was securing the future!\\\" (Authority)\",\n");
                b.append("          \"token\": \"AUTHORITY\",\n");
                b.append("          \"response\": \"I was securing the future! We did what was necessary.\",\n");
                b.append("          \"goto\": \"route_authority\" },\n");
                b.append("        { \"label\": \"2. \\\"We must unite in this crisis.\\\" (Diplomacy)\",\n");
                b.append("          \"token\": \"DIPLOMACY\",\n");
                b.append("          \"response\": \"We must unite in this crisis. Let funds be diverted to the people.\",\n");
                b.append("          \"effects\": [\"gold:-25000\"],\n");
                b.append("          \"goto\": \"route_diplomacy\" },\n");
                b.append("        { \"label\": \"3. \\\"Silence! Guards, arrest this fool!\\\" (Tyranny)\",\n");
                b.append("          \"token\": \"TYRANNY\",\n");
                b.append("          \"response\": \"Silence! Guards, arrest this fool immediately!\",\n");
                b.append("          \"effects\": [\"limitersOff\", \"desertion:0.10\"],\n");
                b.append("          \"goto\": \"route_tyranny\" },\n");
                // trim trailing comma of last choice
                b.setLength(b.length() - 2); b.append("\n      ],\n");
            }
            b.append("      \"paragraphs\": [\n");
            for (int p = 0; p < ch.paragraphs.length; p++) {
                String spk = p < ch.perspectives.length ? ch.perspectives[p] : "Narrator";
                b.append("        { \"speaker\": \"").append(Json.escape(spk))
                        .append("\", \"text\": \"").append(Json.escape(ch.paragraphs[p])).append("\" }");
                b.append(p < ch.paragraphs.length - 1 ? ",\n" : "\n");
            }
            b.append("      ]\n    },\n");
        }

        // three route stubs - replace their placeholder text with your prose.
        appendRoute(b, "route_authority", "Route: The Iron Answer",
                "The chamber holds its breath. The Emperor's claim of necessity hangs in the air - now write what it costs him. (REPLACE ME)");
        appendRoute(b, "route_diplomacy", "Route: The Open Hand",
                "Coin flows from the treasury toward the people, and the council watches to see if mercy reads as weakness. (REPLACE ME)");
        appendRoute(b, "route_tyranny", "Route: The Drawn Blade",
                "Guards seize the councilman mid-sentence. Something in the House breaks that no rule can mend. (REPLACE ME)");
        b.setLength(b.length() - 2); b.append("\n");   // trim last comma

        b.append("  ],\n  \"characters\": [\n");
        b.append("    { \"name\": \"Kaelen Duskbane\", \"subtitle\": \"The Mercenary\", \"chapter\": \"ch1\", \"paragraph\": 1 },\n");
        b.append("    { \"name\": \"Lyra Veylen\", \"subtitle\": \"The Fugitive Healer\", \"chapter\": \"ch1\", \"paragraph\": 3 },\n");
        b.append("    { \"name\": \"Bram Thorne\", \"subtitle\": \"The Disgraced Knight\", \"chapter\": \"ch1\", \"paragraph\": 3 },\n");
        b.append("    { \"name\": \"Iriah Sable\", \"subtitle\": \"The Rogue\", \"chapter\": \"ch1\", \"paragraph\": 3 },\n");
        b.append("    { \"name\": \"Mara Voss\", \"subtitle\": \"Wanted \\u00b7 Traitorous Magistrate\", \"chapter\": \"ch2\", \"paragraph\": 8 },\n");
        b.append("    { \"name\": \"Joric Fen\", \"subtitle\": \"Former Garrison Captain\", \"chapter\": \"ch8\", \"paragraph\": 4 },\n");
        b.append("    { \"name\": \"House Speaker\", \"subtitle\": \"Speaker of the House\", \"chapter\": \"ch9\", \"paragraph\": 0 }\n");
        b.append("  ]\n}\n");

        // sanity: parse what we just built before writing it
        Json.parse(b.toString());

        try (PrintWriter w = new PrintWriter("story.json", StandardCharsets.UTF_8)) {
            w.print(b);
        }
        System.out.println("story.json written: " + StoryData.CHAPTERS.size()
                + " chapters exported + 3 route stubs + 7 character reveals. Parsed OK.");
    }

    private static void appendRoute(StringBuilder b, String id, String title, String stub) {
        b.append("    {\n");
        b.append("      \"id\": \"").append(id).append("\",\n");
        b.append("      \"title\": \"").append(Json.escape(title)).append("\",\n");
        b.append("      \"mode\": \"story\",\n");                 // <- back to the story panel
        b.append("      \"next\": \"END_PLACEHOLDER\",\n");       // point all three at your convergence chapter
        b.append("      \"paragraphs\": [\n");
        b.append("        { \"speaker\": \"Narrator\", \"text\": \"").append(Json.escape(stub)).append("\" }\n");
        b.append("      ]\n    },\n");
    }
}