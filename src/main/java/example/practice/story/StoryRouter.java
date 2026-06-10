package example.practice.story;

// One pending jump, set by a choice's "goto" and consumed by MainGUI's
// advanceParagraph. Tiny on purpose: routing is data (story.json); this is
// just the hand-off between the choice button and the page turn.
public final class StoryRouter {

    private StoryRouter() {}

    private static String pendingChapterId = null;

    public static void jumpTo(String chapterId) { pendingChapterId = chapterId; }
    public static boolean hasPending() { return pendingChapterId != null; }

    // Returns the chapter INDEX to jump to, or -1 if none (or the id is unknown).
    public static int consumeJumpIndex() {
        if (pendingChapterId == null) return -1;
        int idx = StoryData.indexOfId(pendingChapterId);
        pendingChapterId = null;
        return idx;
    }

    public static void reset() { pendingChapterId = null; }
}