package pz.playablechildren;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.OnExit;

/**
 * Registers child-only PCH aliases after PZ has loaded its base + mod hairstyles.
 *
 * Goals:
 *  - Never override vanilla hairstyle IDs.
 *  - Clone every vanilla B42 hairstyle to PCH_Vanilla_* for children.
 *  - Girls use the same MALE vanilla model whenever the style exists for both sexes,
 *    because playable girls use the shared MaleBody head geometry.
 *  - Mirror bundled PCH_Spn_M_* styles into the female HairStyles list so girls can
 *    literally use the same fitted Spongie child mesh as boys.
 *  - Create PCH_Yaki_* aliases only when Yaki's Hair Salon is actually loaded. PCH
 *    carries hidden fitted copies of Yaki's male meshes, but they are never registered
 *    unless the original Yaki mod is active. Both child sexes use that same male mesh.
 */
public final class PatchChildHairRegistry {
    public static final String VANILLA_PREFIX = "PCH_Vanilla_";
    public static final String YAKI_PREFIX = "PCH_Yaki_";
    public static final String TTRP_MALE_PREFIX = "PCH_TTRP_M_";
    public static final String TTRP_FEMALE_PREFIX = "PCH_TTRP_F_";

    private static volatile Object registeredInstance;
    private static final Set<String> vanillaMale = new HashSet<>();
    private static final Set<String> vanillaFemale = new HashSet<>();
    private static final Set<String> yakiSource = new HashSet<>();
    private static final Set<String> yakiAliases = new HashSet<>();
    /** Snapshot of registered style IDs, built once per HairStyles instance for lock-free render lookups. */
    private static final Set<String> registeredMaleStyles = new HashSet<>();
    private static final Set<String> registeredFemaleStyles = new HashSet<>();
    /** Unique reverse links from hidden hat/compressed alternates to their canonical PCH base style.
     *  Built once after HairStyles.init; ambiguous shared alternates are intentionally not mapped. */
    private static final Map<String,String> maleCanonicalByAlternate = new HashMap<>();
    private static final Map<String,String> femaleCanonicalByAlternate = new HashMap<>();

    private PatchChildHairRegistry() {}

    public static boolean removedVanillaForChildren(String name) {
        if (name == null) return false;
        return name.equalsIgnoreCase("Picard")
            || name.equalsIgnoreCase("Baldspot")
            || name.equalsIgnoreCase("Long2")
            || name.equalsIgnoreCase("Long2Curly");
    }

    public static boolean removedYakiForChildren(String name) {
        if (name == null) return false;
        return name.equalsIgnoreCase("M_Yaki_Jessica")
            || name.equalsIgnoreCase("F_Yaki_Jessica")
            || name.equalsIgnoreCase("PCH_Yaki_Jessica");
    }

    @Patch(className="zombie.core.skinnedmodel.population.HairStyles", methodName="init")
    public static final class HairStylesInit {
        private HairStylesInit() {}
        @OnExit public static void exit() { ensureRegistered(); }
    }

    public static synchronized boolean ensureRegistered() {
        try {
            Class<?> hsClass = Class.forName("zombie.core.skinnedmodel.population.HairStyles");
            Object instance = hsClass.getField("instance").get(null);
            if (instance == null) return false;
            if (registeredInstance == instance) return true;

            vanillaMale.clear();
            vanillaFemale.clear();
            yakiSource.clear();
            yakiAliases.clear();
            registeredMaleStyles.clear();
            registeredFemaleStyles.clear();
            maleCanonicalByAlternate.clear();
            femaleCanonicalByAlternate.clear();

            Object base = parseBaseHairStyles(hsClass);
            if (base == null) return false;

            List<Object> baseMale = getList(base, "maleStyles");
            List<Object> baseFemale = getList(base, "femaleStyles");
            List<Object> liveMale = getList(instance, "maleStyles");
            List<Object> liveFemale = getList(instance, "femaleStyles");
            if (baseMale == null || baseFemale == null || liveMale == null || liveFemale == null) return false;

            Map<String,Object> baseMaleByName = indexByName(baseMale);
            Map<String,Object> baseFemaleByName = indexByName(baseFemale);
            Map<String,Object> liveMaleByName = indexByName(liveMale);
            Map<String,Object> liveFemaleByName = indexByName(liveFemale);
            for (Object style : baseMale) vanillaMale.add(lower(nameOf(style)));
            for (Object style : baseFemale) vanillaFemale.add(lower(nameOf(style)));

            // All vanilla aliases are PCH-owned definitions. We never override the base IDs.
            // Prefer the LIVE definition for a vanilla name so an active Fluffy Hair override
            // can contribute only its hat/compressed alternates. The selectable child style is
            // still PCH_Vanilla_*, and its base model/texture remain the game's original assets.
            for (Object baseSource : baseMale) {
                String name = nameOf(baseSource);
                if (removedVanillaForChildren(name)) continue;
                Object source = liveMaleByName.getOrDefault(lower(name), baseSource);
                addCloneIfMissing(liveMale, source, VANILLA_PREFIX + name,
                    s -> mapVanillaTarget(s, baseMaleByName), true);
            }
            for (Object femaleBaseSource : baseFemale) {
                String name = nameOf(femaleBaseSource);
                if (removedVanillaForChildren(name)) continue;
                Object source;
                Map<String,Object> sourceIndex;
                if (baseMaleByName.containsKey(lower(name))) {
                    // Shared child head = MaleBody, so both child sexes use the male geometry.
                    Object maleBase = baseMaleByName.get(lower(name));
                    source = liveMaleByName.getOrDefault(lower(name), maleBase);
                    sourceIndex = baseMaleByName;
                } else {
                    source = liveFemaleByName.getOrDefault(lower(name), femaleBaseSource);
                    sourceIndex = baseFemaleByName;
                }
                final Map<String,Object> idx = sourceIndex;
                addCloneIfMissing(liveFemale, source, VANILLA_PREFIX + name,
                    s -> mapVanillaTarget(s, idx), true);
            }

            // CHILD Female keeps isFemale() for voice and identity, but its head is the
            // shared MaleBody head. Some female base aliases point at male-only hidden
            // Group01..Group04 styles; mirror those PCH aliases so HairStyles can resolve
            // the same compressed mesh instead of silently losing the hat alternate.
            mirrorSharedChildHeadAliases(liveMale, liveFemale, VANILLA_PREFIX);

            // FIXED42: explicit hat-alternate substitutions requested for the child catalog.
            // Keep the selectable/base style untouched; only Group01..Group04 (headwear groups)
            // inherit the compressed alternate from a better-fitting sibling style.
            copyHatAlternatesByName(liveMale, VANILLA_PREFIX + "Metal", VANILLA_PREFIX + "Fabian");
            copyHatAlternatesByName(liveFemale, VANILLA_PREFIX + "Metal", VANILLA_PREFIX + "Fabian");
            copyHatAlternatesByName(liveMale, VANILLA_PREFIX + "LongBraids02", VANILLA_PREFIX + "LongBraids");
            copyHatAlternatesByName(liveFemale, VANILLA_PREFIX + "LongBraids02", VANILLA_PREFIX + "LongBraids");

            // Mikasa Short should use Marie's compressed G4 headwear mesh. Marie's Group01..04
            // all resolve to PCH_Spn_M_MarieG4, so copying those categories gives exactly that
            // result while leaving MikasaShort itself unchanged with no hat.
            copyHatAlternatesByName(liveMale, "PCH_Spn_M_MikasaShort", "PCH_Spn_M_Marie");

            // Spongie is already physically bundled/fitted in PlayableChildren. Mirror every
            // male child entry into the female registry so girls use the exact same mesh.
            List<Object> maleSnapshot = new ArrayList<>(liveMale);
            for (Object source : maleSnapshot) {
                String name = nameOf(source);
                if (name.startsWith("PCH_Spn_M_")) {
                    addCloneIfMissing(liveFemale, source, name, Function.identity(), true);
                }
            }

            // Yaki remains optional. If its source styles are loaded, create child-only aliases
            // from the male source and register the same alias for both child sexes.
            Map<String,Object> currentMaleByName = indexByName(liveMale);
            // Keep only selectable Yaki styles plus the hidden variants they directly reference.
            // The B42 BASE XML contains a few orphan noChoose entries whose model files are not
            // shipped by that archive; cloning those would create broken PCH aliases.
            Set<String> yakiNeeded = new HashSet<>();
            for (Object source : maleSnapshot) {
                String name = nameOf(source);
                if (name.startsWith("M_Yaki_") && !removedYakiForChildren(name) && !boolField(source, "noChoose")) {
                    yakiNeeded.add(lower(name));
                    collectYakiTargets(source, yakiNeeded);
                }
            }
            List<Object> yakiMaleSources = new ArrayList<>();
            for (Object source : maleSnapshot) {
                String name = nameOf(source);
                if (name.startsWith("M_Yaki_") && !removedYakiForChildren(name) && yakiNeeded.contains(lower(name))) yakiMaleSources.add(source);
            }
            for (Object source : yakiMaleSources) {
                String sourceName = nameOf(source);
                String suffix = sourceName.substring("M_Yaki_".length());
                String alias = YAKI_PREFIX + suffix;
                yakiSource.add(lower(sourceName));
                yakiSource.add(lower("F_Yaki_" + suffix));
                yakiAliases.add(lower(alias));
                Function<String,String> mapper = s -> mapYakiTarget(s, currentMaleByName, baseMaleByName);
                Object maleAlias = addCloneIfMissing(liveMale, source, alias, mapper, true);
                Object femaleAlias = addCloneIfMissing(liveFemale, source, alias, mapper, true);
                // Hidden internal copy. Files are shipped as PCH_Yaki_<suffix>.fbx.
                String internalModel = "skinned\\hair\\" + alias;
                // Keep Yaki's original texture ID. The alias is exposed only while Yaki is
                // active, so duplicating its textures inside PCH is unnecessary bloat.
                String sourceTexture = stringField(source, "texture");
                if (maleAlias != null) { setField(maleAlias, "model", internalModel); setField(maleAlias, "texture", sourceTexture); }
                if (femaleAlias != null) { setField(femaleAlias, "model", internalModel); setField(femaleAlias, "texture", sourceTexture); }
            }


            // TTRP Hairstyles stays fully external/optional. Unlike Spongie/Yaki we do not
            // bundle any of its FBX or textures: child-only PCH aliases point at the original
            // TTRP assets, and the render patch applies transforms only to child instances.
            // Register both M/F alias families in both registries so cross-sex alternates in
            // TTRP (some female styles reference M_TTRP_* hat variants) always resolve.
            List<Object> ttrpMaleSources = new ArrayList<>();
            List<Object> ttrpFemaleSources = new ArrayList<>();
            for (Object source : maleSnapshot) {
                String name = nameOf(source);
                if (name.startsWith("M_TTRP_")) ttrpMaleSources.add(source);
            }
            for (Object source : new ArrayList<>(liveFemale)) {
                String name = nameOf(source);
                if (name.startsWith("F_TTRP_")) ttrpFemaleSources.add(source);
            }
            Map<String,String> ttrpCanonical = new HashMap<>();
            for (Object source : ttrpMaleSources) ttrpCanonical.putIfAbsent(lower(nameOf(source)), nameOf(source));
            for (Object source : ttrpFemaleSources) ttrpCanonical.putIfAbsent(lower(nameOf(source)), nameOf(source));
            // TTRP 42.0 contains an obvious hidden-style typo: the Elliot hat entry is
            // named M_TTRP_Elliot instead of M_TTRP_Elliot_Hat. Preserve the source
            // mod untouched, but expose the intended target only inside PCH so its
            // Group01 alternate can resolve correctly for child characters.
            ttrpCanonical.putIfAbsent(lower("M_TTRP_Elliot_Hat"), "M_TTRP_Elliot_Hat");
            Function<String,String> ttrpMapper = s -> mapTtrpTarget(s, ttrpCanonical);
            for (Object source : ttrpMaleSources) {
                String sourceName = nameOf(source);
                String alias = TTRP_MALE_PREFIX + sourceName.substring("M_TTRP_".length());
                addCloneIfMissing(liveMale, source, alias, ttrpMapper, true);
                addCloneIfMissing(liveFemale, source, alias, ttrpMapper, true);
                if (sourceName.equalsIgnoreCase("M_TTRP_Elliot") && boolField(source, "noChoose")) {
                    String model = stringField(source, "model");
                    if (model != null && lower(model).contains("_hat")) {
                        addCloneIfMissing(liveMale, source, TTRP_MALE_PREFIX + "Elliot_Hat", ttrpMapper, true);
                        addCloneIfMissing(liveFemale, source, TTRP_MALE_PREFIX + "Elliot_Hat", ttrpMapper, true);
                    }
                }
            }
            for (Object source : ttrpFemaleSources) {
                String sourceName = nameOf(source);
                String alias = TTRP_FEMALE_PREFIX + sourceName.substring("F_TTRP_".length());
                addCloneIfMissing(liveMale, source, alias, ttrpMapper, true);
                addCloneIfMissing(liveFemale, source, alias, ttrpMapper, true);
            }

            // Vanilla/Fluffy keep the selected HumanVisual style canonical and choose a
            // compressed alternate only while a hat is actually rendered. Build the inverse
            // relationship for PCH aliases once so old/broken saves that persisted an
            // alternate (for example ...G1) can be repaired without guessing names.
            buildCanonicalAlternateMap(liveMale, maleCanonicalByAlternate);
            buildCanonicalAlternateMap(liveFemale, femaleCanonicalByAlternate);
            snapshotRegisteredStyles(liveMale, registeredMaleStyles);
            snapshotRegisteredStyles(liveFemale, registeredFemaleStyles);

            // Volatile publication happens last: render-thread readers either see the old
            // completed registry or this fully-built snapshot, never a partially-filled map.
            registeredInstance = instance;
            return true;
        } catch (Throwable t) {
            System.out.println("[PlayableChildren] Hair registry error: " + t);
            return false;
        }
    }


    /**
     * Return the canonical PCH base style when {@code styleId} is a unique hidden
     * headwear alternate. Shared generic alternates (Bald/Hat/etc.) are deliberately
     * left unchanged because they cannot be reversed without knowing the prior style.
     */
    public static String canonicalBaseForAlternate(String styleId, boolean female) {
        if (registeredInstance == null) ensureRegistered();
        return canonicalBaseForAlternateRegistered(styleId, female);
    }

    private static String canonicalBaseForAlternateRegistered(String styleId, boolean female) {
        if (styleId == null || styleId.isEmpty()) return "";
        Map<String,String> map = female ? femaleCanonicalByAlternate : maleCanonicalByAlternate;
        String current = styleId;
        for (int depth = 0; depth < 4; depth++) {
            String parent = map.get(lower(current));
            if (parent == null || parent.isEmpty() || parent.equalsIgnoreCase(current)) break;
            current = parent;
        }
        return current;
    }

    /**
     * Resolve a source/legacy style to the PCH style namespace used by CHILD fitting.
     * This method is called from the render hot path. HairStyles registration is performed
     * once, then all lookups below are plain set/map reads: no reflection and no synchronized
     * ensureRegistered() call on every rendered strand/model instance.
     */
    public static String childCalibrationStyle(String styleId, boolean female) {
        if (styleId == null || styleId.isEmpty()) return "";
        if (registeredInstance == null) ensureRegistered();

        String canonical = canonicalBaseForAlternateRegistered(styleId, female);
        if (canonical.regionMatches(true, 0, "PCH_", 0, 4)) return canonical;

        String low = lower(canonical);
        Set<String> vanilla = female ? vanillaFemale : vanillaMale;
        Set<String> registered = female ? registeredFemaleStyles : registeredMaleStyles;

        if (vanilla.contains(low) && !canonical.equalsIgnoreCase("Bald") && !removedVanillaForChildren(canonical)) {
            String alias = VANILLA_PREFIX + canonical;
            if (registered.contains(lower(alias))) return alias;
        }

        if (yakiSource.contains(low)) {
            String suffix = null;
            if (canonical.regionMatches(true, 0, "M_Yaki_", 0, 7) || canonical.regionMatches(true, 0, "F_Yaki_", 0, 7)) {
                suffix = canonical.substring(7);
            }
            if (suffix != null) {
                String alias = YAKI_PREFIX + suffix;
                if (registered.contains(lower(alias))) return alias;
            }
        }

        if (canonical.regionMatches(true, 0, "M_TTRP_", 0, 7)) {
            String alias = TTRP_MALE_PREFIX + canonical.substring(7);
            if (registered.contains(lower(alias))) return alias;
        } else if (canonical.regionMatches(true, 0, "F_TTRP_", 0, 7)) {
            String alias = TTRP_FEMALE_PREFIX + canonical.substring(7);
            if (registered.contains(lower(alias))) return alias;
        }

        if (canonical.regionMatches(true, 0, "M_", 0, 2) || canonical.regionMatches(true, 0, "F_", 0, 2)) {
            String core = canonical.substring(2);
            if (core.equalsIgnoreCase("Goth2")) core = "Goth";
            String spongie = "PCH_Spn_M_" + core;
            if (registered.contains(lower(spongie))) return spongie;
            String harry = "PCH_Harry_" + canonical;
            if (registered.contains(lower(harry))) return harry;
        }
        return canonical;
    }


    private static void snapshotRegisteredStyles(List<Object> styles, Set<String> out) {
        for (Object style : styles) {
            String name = nameOf(style);
            if (name != null && !name.isEmpty()) out.add(lower(name));
        }
    }

    @SuppressWarnings("unchecked")
    private static void buildCanonicalAlternateMap(List<Object> styles, Map<String,String> out) {
        Map<String,String> candidate = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (Object parentStyle : styles) {
            String parent = nameOf(parentStyle);
            if (!isPchStyle(parent)) continue;
            try {
                List<Object> alternates = (List<Object>)parentStyle.getClass().getField("alternate").get(parentStyle);
                for (Object alt : alternates) {
                    String category = stringField(alt, "category");
                    String target = stringField(alt, "style");
                    if (!isHatAlternateCategory(category) || !isPchStyle(target) || target.equalsIgnoreCase(parent)) continue;
                    String key = lower(target);
                    String existing = candidate.get(key);
                    if (existing == null) candidate.put(key, parent);
                    else if (!existing.equalsIgnoreCase(parent)) ambiguous.add(key);
                }
            } catch (Throwable ignored) {}
        }
        for (Map.Entry<String,String> entry : candidate.entrySet()) {
            if (!ambiguous.contains(entry.getKey())) out.put(entry.getKey(), entry.getValue());
        }
    }

    private static boolean isPchStyle(String value) {
        return value != null && value.regionMatches(true, 0, "PCH_", 0, 4);
    }

    private static boolean isHatAlternateCategory(String category) {
        if (category == null) return false;
        String value = lower(category);
        return value.equals("default") || value.equals("group01") || value.equals("group02")
            || value.equals("group03") || value.equals("group04");
    }

    public static boolean isVanillaStyle(String styleId, boolean female) {
        ensureRegistered();
        if (styleId == null) return false;
        return (female ? vanillaFemale : vanillaMale).contains(lower(styleId));
    }

    public static String toVanillaAlias(String styleId, boolean female) {
        if (!isVanillaStyle(styleId, female)) return "";
        if (styleId == null || styleId.isEmpty() || styleId.equalsIgnoreCase("Bald")) return "";
        if (removedVanillaForChildren(styleId)) return "";
        return VANILLA_PREFIX + styleId;
    }

    public static boolean isYakiSource(String styleId) {
        ensureRegistered();
        return styleId != null && yakiSource.contains(lower(styleId));
    }

    public static String toYakiAlias(String styleId) {
        ensureRegistered();
        if (styleId == null) return "";
        String low = lower(styleId);
        if (!yakiSource.contains(low)) return "";
        String suffix;
        if (styleId.regionMatches(true, 0, "M_Yaki_", 0, 7)) suffix = styleId.substring(7);
        else if (styleId.regionMatches(true, 0, "F_Yaki_", 0, 7)) suffix = styleId.substring(7);
        else return "";
        String alias = YAKI_PREFIX + suffix;
        return yakiAliases.contains(lower(alias)) ? alias : "";
    }

    public static boolean hasStyle(String styleId, boolean female) {
        ensureRegistered();
        if (styleId == null) return false;
        try {
            Class<?> hsClass = Class.forName("zombie.core.skinnedmodel.population.HairStyles");
            Object instance = hsClass.getField("instance").get(null);
            if (instance == null) return false;
            Method m = hsClass.getMethod(female ? "FindFemaleStyle" : "FindMaleStyle", String.class);
            return m.invoke(instance, styleId) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object parseBaseHairStyles(Class<?> hsClass) throws Exception {
        Class<?> zfsClass = Class.forName("zombie.ZomboidFileSystem");
        Object zfs = zfsClass.getField("instance").get(null);
        Object baseFolder = zfsClass.getField("base").get(zfs);
        File canonical = (File)baseFolder.getClass().getField("canonicalFile").get(baseFolder);
        if (canonical == null) return null;
        File xml = new File(canonical, "media" + File.separator + "hairStyles" + File.separator + "hairStyles.xml");
        Method parse = hsClass.getMethod("Parse", String.class);
        return parse.invoke(null, xml.getAbsolutePath());
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getList(Object owner, String field) throws Exception {
        return (List<Object>)owner.getClass().getField(field).get(owner);
    }

    private static Map<String,Object> indexByName(List<Object> styles) {
        Map<String,Object> out = new HashMap<>();
        for (Object style : styles) out.put(lower(nameOf(style)), style);
        return out;
    }

    private static String nameOf(Object style) {
        if (style == null) return "";
        try {
            Object v = style.getClass().getField("name").get(style);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable ignored) { return ""; }
    }

    private static String mapVanillaTarget(String target, Map<String,Object> sourceIndex) {
        if (target == null || target.isEmpty()) return target;
        return sourceIndex.containsKey(lower(target)) ? VANILLA_PREFIX + target : target;
    }

    @SuppressWarnings("unchecked")
    private static void collectYakiTargets(Object style, Set<String> out) {
        try {
            Class<?> c = style.getClass();
            List<String> trim = (List<String>)c.getField("trimChoices").get(style);
            for (String s : trim) if (s != null && s.startsWith("M_Yaki_") && !removedYakiForChildren(s)) out.add(lower(s));
            List<Object> alt = (List<Object>)c.getField("alternate").get(style);
            for (Object a : alt) {
                Object raw = a.getClass().getField("style").get(a);
                String s = raw == null ? "" : String.valueOf(raw);
                if (s.startsWith("M_Yaki_") && !removedYakiForChildren(s)) out.add(lower(s));
            }
        } catch (Throwable ignored) {}
    }

    private static String mapTtrpTarget(String target, Map<String,String> canonicalNames) {
        if (target == null || target.isEmpty()) return target;
        String canonical = canonicalNames.get(lower(target));
        if (canonical == null) return target;
        if (canonical.regionMatches(true, 0, "M_TTRP_", 0, 7)) {
            return TTRP_MALE_PREFIX + canonical.substring(7);
        }
        if (canonical.regionMatches(true, 0, "F_TTRP_", 0, 7)) {
            return TTRP_FEMALE_PREFIX + canonical.substring(7);
        }
        return target;
    }

    private static String mapYakiTarget(String target, Map<String,Object> maleIndex, Map<String,Object> vanillaIndex) {
        if (target == null || target.isEmpty()) return target;
        if (removedYakiForChildren(target)) return "";
        if (target.regionMatches(true, 0, "M_Yaki_", 0, 7)) {
            return YAKI_PREFIX + target.substring(7);
        }
        if (target.regionMatches(true, 0, "F_Yaki_", 0, 7)) {
            return YAKI_PREFIX + target.substring(7);
        }
        if (vanillaIndex.containsKey(lower(target))) return VANILLA_PREFIX + target;
        return target;
    }

    @SuppressWarnings("unchecked")
    private static void copyHatAlternatesByName(List<Object> styles, String targetName, String sourceName) {
        Object target = findByName(styles, targetName);
        Object source = findByName(styles, sourceName);
        if (target == null || source == null) return;
        try {
            List<Object> srcAlt = (List<Object>)source.getClass().getField("alternate").get(source);
            List<Object> dstAlt = (List<Object>)target.getClass().getField("alternate").get(target);
            Map<String,String> sourceByCategory = new HashMap<>();
            for (Object alt : srcAlt) {
                String category = stringField(alt, "category");
                String style = stringField(alt, "style");
                if (category != null && style != null) sourceByCategory.put(lower(category), style);
            }
            for (Object alt : dstAlt) {
                String category = stringField(alt, "category");
                if (category == null) continue;
                String low = lower(category);
                if (!low.equals("group01") && !low.equals("group02")
                        && !low.equals("group03") && !low.equals("group04")) continue;
                String replacement = sourceByCategory.get(low);
                if (replacement != null && !replacement.isEmpty()) setField(alt, "style", replacement);
            }
        } catch (Throwable t) {
            System.out.println("[PlayableChildren] Hat alternate remap failed "
                + targetName + " <- " + sourceName + ": " + t);
        }
    }

    private static Object addCloneIfMissing(List<Object> targetList, Object source, String newName,
                                            Function<String,String> targetMapper, boolean forceNoChoose) throws Exception {
        if (source == null || newName == null || newName.isEmpty()) return null;
        Object existing = findByName(targetList, newName);
        if (existing != null) return existing;
        Object clone = cloneStyle(source, newName, targetMapper, forceNoChoose);
        targetList.add(clone);
        return clone;
    }

    private static void mirrorSharedChildHeadAliases(List<Object> maleStyles, List<Object> femaleStyles,
                                                     String prefix) throws Exception {
        for (Object source : new ArrayList<>(maleStyles)) {
            String name = nameOf(source);
            if (name.regionMatches(true, 0, prefix, 0, prefix.length())) {
                addCloneIfMissing(femaleStyles, source, name, Function.identity(), true);
            }
        }
    }

    private static Object findByName(List<Object> list, String name) {
        for (Object style : list) if (name.equalsIgnoreCase(nameOf(style))) return style;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Object cloneStyle(Object source, String newName, Function<String,String> targetMapper,
                                     boolean forceNoChoose) throws Exception {
        Class<?> styleClass = source.getClass();
        Constructor<?> ctor = styleClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object clone = ctor.newInstance();

        setField(clone, "name", newName);
        copyField(source, clone, "model");
        copyField(source, clone, "texture");
        copyField(source, clone, "level");
        copyField(source, clone, "growReference");
        copyField(source, clone, "attachedHair");
        setField(clone, "noChoose", forceNoChoose || boolField(source, "noChoose"));

        List<String> srcTrim = (List<String>)styleClass.getField("trimChoices").get(source);
        List<String> dstTrim = (List<String>)styleClass.getField("trimChoices").get(clone);
        for (String s : srcTrim) dstTrim.add(targetMapper.apply(s));

        List<Object> srcAlt = (List<Object>)styleClass.getField("alternate").get(source);
        List<Object> dstAlt = (List<Object>)styleClass.getField("alternate").get(clone);
        for (Object alt : srcAlt) {
            Class<?> altClass = alt.getClass();
            Constructor<?> altCtor = altClass.getDeclaredConstructor();
            altCtor.setAccessible(true);
            Object a = altCtor.newInstance();
            copyField(alt, a, "category");
            Object raw = altClass.getField("style").get(alt);
            setField(a, "style", targetMapper.apply(raw == null ? "" : String.valueOf(raw)));
            dstAlt.add(a);
        }
        return clone;
    }

    private static String stringField(Object o, String name) {
        try { Object v = o.getClass().getField(name).get(o); return v == null ? null : String.valueOf(v); }
        catch (Throwable ignored) { return null; }
    }

    private static boolean boolField(Object o, String name) {
        try { return o.getClass().getField(name).getBoolean(o); }
        catch (Throwable ignored) { return false; }
    }
    private static void copyField(Object src, Object dst, String name) throws Exception {
        Field a = src.getClass().getField(name);
        Field b = dst.getClass().getField(name);
        b.set(dst, a.get(src));
    }
    private static void setField(Object dst, String name, Object value) throws Exception {
        dst.getClass().getField(name).set(dst, value);
    }
    private static String lower(String s) { return s == null ? "" : s.toLowerCase(Locale.ENGLISH); }
}
