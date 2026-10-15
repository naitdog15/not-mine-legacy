package com.temog.notminelegacy.xp;

import java.util.Arrays;

import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Pure XP-storage math shared by any XP-holding item: vanilla's level curve (saturating, in
 * {@code long}), the orb ladder and its partitions, manifest sanitising, and player balance
 * accounting. Nothing here knows about a specific item, config or NBT key; callers resolve their
 * own capacity and pass it in.
 *
 * <p>Chunks come from vanilla's own orb ladder ({@link #ORB_LADDER}), so every chunk is a value
 * vanilla itself would spawn; no partition produced here can create a single mega-orb (a Mending
 * amplifier). Chunk counts are bounded by {@link #MAX_CHUNKS}. A total too large to express within
 * that cap pays the largest expressible amount and drops the rest: it errs towards paying too
 * little and never mints XP.
 */
public final class XpMath {

    private XpMath() {}

    /**
     * Vanilla's own experience-orb tier table, ascending: the values
     * {@code ExperienceOrb#getExperienceValue} splits a lump sum into. Treat as immutable.
     */
    public static final int[] ORB_LADDER = {1, 3, 7, 17, 37, 73, 149, 307, 617, 1237, 2477};

    /** Hard ceiling on manifest length. Bounds NBT size and pickup time (2-tick delay per orb). */
    public static final int MAX_CHUNKS = 32;

    /** Pace the randomised partition keeps; not a limit, {@link #MAX_CHUNKS} is the limit. */
    private static final int SOFT_CHUNK_TARGET = 6;

    /** Largest legal single-orb value: the top rung of {@link #ORB_LADDER}. */
    public static final int MAX_ORB_VALUE = ORB_LADDER[ORB_LADDER.length - 1];

    /** Fill-stage bucket count, matching an 8-stage fill texture ladder. */
    public static final int FILL_STAGE_COUNT = 8;

    private static final int[] NO_CHUNKS = new int[0];

    /**
     * Slack allowed when flooring a measured XP delta. Vanilla stores the XP bar as a float, so a
     * difference of two samples carries a little noise; far below one XP point, well above noise.
     */
    public static final double ROUNDING_EPSILON = 1.0E-3D;

    /** Maximum distance from an integer XP total treated as float boundary noise. */
    private static final double SNAP_EPSILON = 0.01D;

    /** Highest level whose cumulative curve is evaluated exactly by {@link #experienceForLevel}. */
    private static final int MAX_EXACT_CURVE_LEVEL = 1_000_000_000;

    /** Exact integer level plus the representable vanilla bar progress within that level. */
    public record LevelAndProgress(int level, float progress) {}

    // -- Level curve ---------------------------------------------------------

    /**
     * Total XP required to reach {@code level} from zero: the exact arithmetic-progression sums
     * vanilla uses, widened to {@code long} and saturating rather than overflowing.
     */
    public static long experienceForLevel(int level) {
        if (level <= 0) return 0L;
        // Beyond this the quadratic exceeds Long.MAX_VALUE; no reachable player is anywhere near it.
        if (level > 1_000_000_000) return Long.MAX_VALUE;
        if (level <= 15) return progressionSum(level, 7L, 2L);
        if (level <= 30) return 315L + progressionSum(level - 15, 37L, 5L);
        return 1395L + progressionSum(level - 30, 112L, 9L);
    }

    /**
     * Capacity in XP points for a capacity in levels, clamped into {@code [1, Integer.MAX_VALUE]}
     * so a wildly out-of-range config cannot wrap into a negative capacity.
     */
    public static int capacityForLevels(int levels) {
        long xp = experienceForLevel(levels);
        if (xp < 1L) return 1;
        if (xp > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) xp;
    }

    /**
     * Level-equivalent of {@code storedXp}: the largest {@code L <= ceilingLevels} whose cumulative
     * XP cost fits inside it.
     */
    public static int levelForStored(int storedXp, int ceilingLevels) {
        if (storedXp <= 0) return 0;
        int level = 0;
        while (level < ceilingLevels && experienceForLevel(level + 1) <= storedXp) {
            level++;
        }
        return level;
    }

    /**
     * Exact inverse of {@link #experienceForLevel(int)} for a non-negative XP balance. A nearest
     * float quotient can multiply back to just below its integer remainder; when that happens it is
     * advanced by one representable float so {@link #playerTotalXp(Player)}'s floored readback
     * returns the same integer total. Whole-level thresholds keep an exact {@code 0.0F} progress.
     */
    public static LevelAndProgress levelAndProgressForExperience(long totalXp) {
        long target = Math.max(0L, totalXp);
        int low = 0;
        int high = MAX_EXACT_CURVE_LEVEL;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (experienceForLevel(middle) <= target) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }

        int level = low;
        long forLevel = experienceForLevel(level);
        long remainder = target - forLevel;
        if (remainder <= 0L) return new LevelAndProgress(level, 0.0F);

        long needed = experienceForLevel(level + 1) - forLevel;
        if (needed <= 0L) return new LevelAndProgress(level, 0.0F);

        float progress = (float) remainder / (float) needed;
        if ((long) Math.floor(progress * (float) needed) < remainder) {
            progress = Math.nextUp(progress);
        }
        return new LevelAndProgress(level, progress);
    }

    // -- Orb partitions ------------------------------------------------------

    /**
     * Deterministic greedy decomposition of {@code total} over {@link #ORB_LADDER}: repeatedly take
     * the largest rung that still fits.
     *
     * @return one entry per orb, largest first; empty for a non-positive total
     */
    public static int[] canonicalPartition(int total) {
        if (total <= 0) return NO_CHUNKS;

        int[] picked = new int[MAX_CHUNKS];
        int count = 0;
        int remaining = total;
        while (remaining > 0 && count < MAX_CHUNKS) {
            int rung = highestRungAtMost(remaining);
            if (rung < 0) break; // unreachable: rung 1 fits any positive remainder
            picked[count++] = rung;
            remaining -= rung;
        }
        // A remainder here means the total is larger than MAX_CHUNKS orbs can express.
        // Paying the expressible part and dropping the tail is the safe direction.
        return Arrays.copyOf(picked, count);
    }

    /**
     * Randomised decomposition of {@code total} over {@link #ORB_LADDER}: same guarantees as
     * {@link #canonicalPartition(int)} but with a varied shape. If either invariant would break the
     * canonical partition is returned instead.
     *
     * @param random may be null, in which case the canonical partition is returned
     */
    public static int[] partitionIntoOrbs(int total, RandomSource random) {
        if (total <= 0) return NO_CHUNKS;

        int[] canonical = canonicalPartition(total);
        if (random == null) return canonical;

        // The canonical sum, not `total`, is the target: both partitioners must agree on what they pay.
        int target = sumOf(canonical);
        if (target <= 0) return canonical;

        int[] picked = new int[MAX_CHUNKS];
        int count = 0;
        int remaining = target;
        while (remaining > 0 && count < MAX_CHUNKS) {
            int highest = highestRungIndexAtMost(remaining);
            if (highest < 0) break; // unreachable: rung 1 fits any positive remainder

            // Smallest rung that still keeps the payout on pace for SOFT_CHUNK_TARGET orbs.
            int pace = Math.max(1, SOFT_CHUNK_TARGET - count);
            int minimum = (remaining + pace - 1) / pace;

            int lowest = highest; // the ladder ascends, so the first match is the lowest one
            for (int i = Math.max(0, highest - 2); i < highest; i++) {
                if (ORB_LADDER[i] >= minimum) { lowest = i; break; }
            }

            int chosen = lowest + random.nextInt(highest - lowest + 1);
            // Out of room: only the largest fitting rung can still close the total.
            int budget = MAX_CHUNKS - count - 1; // orbs left AFTER the one about to be taken
            if (!fitsInBudget(remaining - ORB_LADDER[chosen], budget)) chosen = highest;

            picked[count++] = ORB_LADDER[chosen];
            remaining -= ORB_LADDER[chosen];
        }

        if (remaining != 0 || count > MAX_CHUNKS) return canonical;
        return Arrays.copyOf(picked, count);
    }

    /**
     * Sanitises a manifest that came off NBT (i.e. from anywhere a player or an op with {@code /data}
     * could have reached). Non-positive entries are dropped, every entry is clamped to a legal
     * single-orb value, the list stops the moment its running sum would exceed {@code cap}, and the
     * entry count is capped at {@link #MAX_CHUNKS}.
     */
    public static int[] normalizeChunks(int[] raw, int cap) {
        if (raw == null || raw.length == 0 || cap <= 0) return NO_CHUNKS;

        int[] kept = new int[Math.min(raw.length, MAX_CHUNKS)];
        int count = 0;
        long sum = 0L;
        for (int value : raw) {
            if (count >= MAX_CHUNKS) break;
            if (value <= 0) continue;
            int chunk = Math.min(value, MAX_ORB_VALUE);
            if (sum + chunk > cap) break; // running sum reached capacity; the rest is fabricated
            kept[count++] = chunk;
            sum += chunk;
        }
        return count == kept.length ? kept : Arrays.copyOf(kept, count);
    }

    /**
     * What a manifest for {@code total} is expected to sum to. Normally {@code total} itself; less
     * only where the ladder cannot express the total within {@link #MAX_CHUNKS}.
     */
    public static int expectedManifestSum(int total) {
        return sumOf(canonicalPartition(total));
    }

    /**
     * The manifest a vessel holding {@code stored} XP of {@code cap} should carry: the
     * fill-stage-biased deterministic {@link #stagePartition}, with {@code stored} clamped to
     * {@code cap}. Never varies between calls for the same {@code (stored, cap)} pair, which is what
     * lets two independently filled vessels of the same amount carry identical NBT and stack.
     */
    public static int[] regenerateManifest(int stored, int cap) {
        int clamped = Math.min(stored, cap);
        return stagePartition(clamped, fillStageFor(clamped, cap));
    }

    /** Fill-stage bucket (0..{@link #FILL_STAGE_COUNT}-1) for {@code stored} XP out of {@code cap}. */
    public static int fillStageFor(int stored, int cap) {
        if (cap <= 0) return 0;
        float fraction = Mth.clamp((float) stored / (float) cap, 0.0F, 1.0F);
        return Mth.clamp((int) (fraction * FILL_STAGE_COUNT), 0, FILL_STAGE_COUNT - 1);
    }

    /**
     * Deterministic decomposition of {@code total} over {@link #ORB_LADDER}, biased by fill
     * {@code stage} instead of randomised: a low stage prefers the smallest rung that still keeps
     * pace, a high stage prefers the largest. Same total + same stage always produce the same
     * manifest. At {@code stage == FILL_STAGE_COUNT - 1} it is identical to
     * {@link #canonicalPartition(int)}. In the extreme case (a total beyond {@code MAX_CHUNKS * 2477})
     * it degrades to exactly {@code canonicalPartition}'s behaviour.
     */
    public static int[] stagePartition(int total, int stage) {
        if (total <= 0) return NO_CHUNKS;

        int[] picked = new int[MAX_CHUNKS];
        int count = 0;
        int remaining = total;
        while (remaining > 0 && count < MAX_CHUNKS) {
            int highest = highestRungIndexAtMost(remaining);
            if (highest < 0) break; // unreachable: rung 1 fits any positive remainder

            // Smallest rung that still keeps the payout on pace for SOFT_CHUNK_TARGET orbs.
            int pace = Math.max(1, SOFT_CHUNK_TARGET - count);
            int minimum = (remaining + pace - 1) / pace;

            int lowest = highest; // the ladder ascends, so the first match is the lowest one
            for (int i = Math.max(0, highest - 2); i < highest; i++) {
                if (ORB_LADDER[i] >= minimum) { lowest = i; break; }
            }

            // Deterministic stand-in for partitionIntoOrbs' random pick: stage 0 takes `lowest`, the
            // top stage takes `highest`, stages between linearly interpolate across the window.
            int span = highest - lowest;
            int chosen = span <= 0
                    ? highest
                    : Math.min(highest, lowest + Math.round(span * (stage / (float) (FILL_STAGE_COUNT - 1))));

            // Out of room: only the largest fitting rung can still close the total.
            int budget = MAX_CHUNKS - count - 1; // orbs left AFTER the one about to be taken
            if (!fitsInBudget(remaining - ORB_LADDER[chosen], budget)) chosen = highest;

            picked[count++] = ORB_LADDER[chosen];
            remaining -= ORB_LADDER[chosen];
        }

        if (remaining != 0) return canonicalPartition(total);
        return Arrays.copyOf(picked, count);
    }

    /** Sum of a manifest. Entries are already bounded, so the total cannot overflow an int. */
    public static int sumOf(int[] chunks) {
        if (chunks == null) return 0;
        long sum = 0L;
        for (int value : chunks) {
            if (value > 0) sum += value;
        }
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    /** Highest ladder value that fits in {@code remaining}, or -1 when nothing does. */
    private static int highestRungAtMost(int remaining) {
        int index = highestRungIndexAtMost(remaining);
        return index < 0 ? -1 : ORB_LADDER[index];
    }

    /** Index into {@link #ORB_LADDER} of the highest value that fits, or -1 when nothing does. */
    private static int highestRungIndexAtMost(int remaining) {
        for (int i = ORB_LADDER.length - 1; i >= 0; i--) {
            if (ORB_LADDER[i] <= remaining) return i;
        }
        return -1;
    }

    /** True when {@code remaining} could still be paid off by {@code budget} more orbs. */
    private static boolean fitsInBudget(int remaining, int budget) {
        if (remaining <= 0) return true;
        return (long) remaining <= (long) budget * MAX_ORB_VALUE;
    }

    // -- Player XP accounting ------------------------------------------------

    /**
     * The player's current XP <b>balance</b>, derived from level + bar progress. Never read
     * {@code player.totalExperience}: it is a lifetime counter that enchanting spend is never
     * subtracted from. Arithmetic is saturating {@code long} because {@code /xp} can set levels far
     * beyond what an {@code int} formula survives.
     */
    public static long playerTotalXp(Player player) {
        long forLevel = experienceForLevel(player.experienceLevel);
        if (forLevel == Long.MAX_VALUE) return Long.MAX_VALUE;

        float progress = Mth.clamp(player.experienceProgress, 0.0F, 1.0F);
        long partial = (long) Math.floor(progress * player.getXpNeededForNextLevel());
        return saturatedAdd(forLevel, Math.max(0L, partial));
    }

    /**
     * The same balance, unfloored, for measuring how much XP a mutation actually moved. Only ever
     * used as a difference of two samples taken moments apart, never stored.
     */
    public static double playerTotalXpExact(Player player) {
        long forLevel = experienceForLevel(player.experienceLevel);
        float progress = Mth.clamp(player.experienceProgress, 0.0F, 1.0F);
        return forLevel + (double) progress * player.getXpNeededForNextLevel();
    }

    /**
     * Marginal levels {@code player} would actually gain by adding {@code storedXp} to their current
     * balance right now. Always {@code <=} the level-from-zero figure of {@code storedXp}.
     */
    public static int marginalLevels(Player player, int storedXp) {
        if (storedXp <= 0 || player == null) return 0;
        long combined = saturatedAdd(playerTotalXp(player), (long) storedXp);
        int projectedLevel = levelAndProgressForExperience(combined).level();
        return Math.max(0, projectedLevel - player.experienceLevel);
    }

    /**
     * XP the player could put into a store: either the whole balance, or (when
     * {@code storeUntilPreviousLevel}) only the progress above the current level, falling back to the
     * level below when the player sits on exactly N &gt; 0 levels. May be zero or negative.
     */
    public static long availableToStore(Player player, boolean storeUntilPreviousLevel) {
        if (!storeUntilPreviousLevel) return playerTotalXp(player);
        long xpForCurrentLevel = experienceForLevel(player.experienceLevel);
        long available = playerTotalXp(player) - xpForCurrentLevel;
        // Player sits on exactly N > 0 levels (the bar looks empty): take the level below.
        if (available <= 0L && player.experienceLevel > 0) {
            available = xpForCurrentLevel - experienceForLevel(player.experienceLevel - 1);
        }
        return available;
    }

    /**
     * Takes up to {@code wanted} XP off the player and returns what actually left them. Goes through
     * {@link Player#giveExperiencePoints(int)} (cancellable by listeners) and then <i>measures</i>
     * what moved, floored once, so a store can never be credited with more than the player lost.
     * Reconciles float drift afterwards.
     */
    public static int takeFromPlayer(Player player, int wanted) {
        if (wanted <= 0) return 0;
        double before = playerTotalXpExact(player);
        player.giveExperiencePoints(-wanted);
        long moved = (long) Math.floor(before - playerTotalXpExact(player) + ROUNDING_EPSILON);
        moved = Math.max(0L, Math.min(moved, wanted));
        reconcilePlayerXp(player);
        return (int) moved;
    }

    /**
     * Snaps only a near-integer float artefact in the player's own current XP balance. No payout
     * amount or pre-transfer prediction participates: XP consumed by Mending is already absent from
     * the sampled balance and therefore cannot be restored.
     */
    public static void reconcilePlayerXp(Player player) {
        double exact = playerTotalXpExact(player);
        long target = Math.round(exact);
        if (Math.abs(exact - target) > SNAP_EPSILON) return;
        if (target == playerTotalXp(player)) return;

        LevelAndProgress corrected = levelAndProgressForExperience(target);

        // Enforce the epsilon bound on the RESULT, not just on the input: one float ULP of progress
        // is worth more than SNAP_EPSILON past roughly level 18,600 (reachable only via /xp). A
        // correction that cannot be applied within the bound is simply not applied.
        if (!(corrected.progress() >= 0.0F) || corrected.progress() >= 1.0F) return;
        double correctedExact = exactBalanceOf(corrected);
        if (!Double.isFinite(correctedExact) || Math.abs(correctedExact - exact) > SNAP_EPSILON) return;

        player.experienceLevel = corrected.level();
        player.experienceProgress = corrected.progress();

        // Force the resync by hand: ServerPlayer#tick only sends ClientboundSetExperiencePacket when
        // totalExperience (the LIFETIME counter, which this never touches) differs from lastSentExp.
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetExperiencePacket(
                    serverPlayer.experienceProgress, serverPlayer.totalExperience,
                    serverPlayer.experienceLevel));
        }
    }

    /** The exact XP balance a {@link LevelAndProgress} pair represents, comparable to {@link #playerTotalXpExact}. */
    private static double exactBalanceOf(LevelAndProgress pair) {
        long forLevel = experienceForLevel(pair.level());
        long needed = experienceForLevel(pair.level() + 1) - forLevel;
        if (needed <= 0L) return forLevel;
        return forLevel + (double) pair.progress() * (double) needed;
    }

    private static long progressionSum(long n, long first, long step) {
        return n * (2L * first + (n - 1L) * step) / 2L;
    }

    /** Saturating long addition. */
    public static long saturatedAdd(long a, long b) {
        long sum = a + b;
        // Overflow only when both operands share a sign that the result does not.
        if (((a ^ sum) & (b ^ sum)) < 0L) return a > 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        return sum;
    }
}
