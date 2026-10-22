package pz.playablechildren;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Argument;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.characterTextures.CharacterSmartTexture;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;

/** Stage-specific additions baked into the existing full-character texture. */
public final class PatchChildTextureBake {
    public static final String MALE_TOP_TEXTURE = "media/textures/Clothes/Jumper/PlayableChildren_MaleVestWhite.png";
    public static final String CHILD_FEMALE_TOP_TEXTURE = "media/textures/Clothes/Jumper/PlayableChildren_FemaleVestBlack.png";
    public static final String FEMALE_TOP_TEXTURE = "media/textures/Clothes/Shorts_Textures/PlayableChildren_FemaleSwimsuitBlack.png";
    public static final String BOTTOM_TEXTURE = "media/textures/Clothes/Underwear/PlayableChildren_BoxersBlack.png";
    public static final String ELDER_TEXTURE = "media/textures/PlayableChildren/ElderOverlay.png";

    private static final byte NONE = 0;
    private static final byte CHILD_MALE = 1;
    private static final byte CHILD_FEMALE = 2;
    private static final byte TEEN_MALE = 3;
    private static final byte TEEN_FEMALE = 4;
    private static final byte ELDER = 5;

    private static final Map<Object, Byte> STAGE_CREATORS = Collections.synchronizedMap(new WeakHashMap<>());

    private PatchChildTextureBake() {}

    /**
     * FIXED101: use the exact stable FIXED84-FIXED98 capture path again.
     * Stage comes from the HumanVisual itself, just as it does for the already-working
     * CHILD/TEEN male routes. No body-model inference and no extra init argument.
     */
    public static void remember(Object creator, HumanVisual humanVisual) {
        if (creator == null) return;
        synchronized (STAGE_CREATORS) {
            if (humanVisual == null) {
                STAGE_CREATORS.remove(creator);
                return;
            }
            LifeStage stage = LifeStageResolver.stageOf(humanVisual);
            if (stage == LifeStage.CHILD) {
                STAGE_CREATORS.put(creator, humanVisual.isFemale() ? CHILD_FEMALE : CHILD_MALE);
            } else if (stage == LifeStage.TEEN) {
                STAGE_CREATORS.put(creator, humanVisual.isFemale() ? TEEN_FEMALE : TEEN_MALE);
            } else if (stage == LifeStage.ELDER) {
                STAGE_CREATORS.put(creator, ELDER);
            } else {
                STAGE_CREATORS.remove(creator);
            }
        }
    }

    public static byte stateFor(Object creator) {
        if (creator == null) return NONE;
        synchronized (STAGE_CREATORS) {
            Byte value = STAGE_CREATORS.get(creator);
            return value == null ? NONE : value.byteValue();
        }
    }

    private static void awaitTexture(ArrayList<Texture> pending, String path) {
        Texture texture = Texture.getSharedTexture(path, ModelManager.instance.getTextureFlags());
        if (texture != null && !texture.isReady() && !pending.contains(texture)) pending.add(texture);
    }

    public static void awaitTextures(Object creator, ArrayList<Texture> pending) {
        if (pending == null) return;
        byte state = stateFor(creator);
        if (state == CHILD_MALE) {
            awaitTexture(pending, MALE_TOP_TEXTURE);
            awaitTexture(pending, BOTTOM_TEXTURE);
        } else if (state == CHILD_FEMALE) {
            awaitTexture(pending, CHILD_FEMALE_TOP_TEXTURE);
            awaitTexture(pending, BOTTOM_TEXTURE);
        } else if (state == TEEN_MALE) {
            awaitTexture(pending, BOTTOM_TEXTURE);
        } else if (state == TEEN_FEMALE) {
            awaitTexture(pending, FEMALE_TOP_TEXTURE);
            awaitTexture(pending, BOTTOM_TEXTURE);
        } else if (state == ELDER) {
            awaitTexture(pending, ELDER_TEXTURE);
        }
    }

    public static void bake(Object creator, CharacterSmartTexture texture) {
        if (texture == null) return;
        byte state = stateFor(creator);
        if (state == NONE) return;

        Texture previous = texture.result;
        if (state == CHILD_MALE) {
            texture.addTexture(MALE_TOP_TEXTURE, 2, ImmutableColor.white, 0F);
            texture.addTexture(BOTTOM_TEXTURE, 1, ImmutableColor.white, 0F);
        } else if (state == CHILD_FEMALE) {
            // Permanent CHILD-female protection: pure-black tank + pure-black boxers.
            texture.addTexture(CHILD_FEMALE_TOP_TEXTURE, 2, ImmutableColor.white, 0F);
            texture.addTexture(BOTTOM_TEXTURE, 1, ImmutableColor.white, 0F);
        } else if (state == TEEN_MALE) {
            // TEEN male keeps the known-good boxer-only protection.
            texture.addTexture(BOTTOM_TEXTURE, 1, ImmutableColor.white, 0F);
        } else if (state == TEEN_FEMALE) {
            // FIXED113: permanent TEEN-female protection combines the pure-black
            // Base.Swimsuit_TINT mask with the established pure-black boxer layer.
            texture.addTexture(FEMALE_TOP_TEXTURE, 2, ImmutableColor.white, 0F);
            texture.addTexture(BOTTOM_TEXTURE, 1, ImmutableColor.white, 0F);
        } else if (state == ELDER) {
            texture.addTexture(ELDER_TEXTURE, CharacterSmartTexture.BODY_CATEGORY, ImmutableColor.white, 0F);
        }

        texture.calculate();
        if (previous != null && previous != texture.result) TextureCombiner.instance.releaseTexture(previous);
    }

    @Patch(className = "zombie.core.skinnedmodel.model.ModelInstanceTextureCreator", methodName = "init")
    public static final class CaptureHumanVisual {
        @OnEnter
        public static void enter(@This Object creator, @Argument(0) HumanVisual humanVisual) {
            remember(creator, humanVisual);
        }

        @OnExit
        @SuppressWarnings("unchecked")
        public static void exit(@This Object creator) {
            awaitTextures(creator, (ArrayList<Texture>)ReflectionAccess.tryGet(creator, "texturesNotReady", null));
        }
    }

    @Patch(className = "zombie.core.skinnedmodel.model.ModelInstanceTextureCreator", methodName = "createFullCharacterTexture")
    public static final class BakeCharacterTexture {
        @OnExit
        public static void exit(@This Object creator, @Return CharacterSmartTexture texture) {
            bake(creator, texture);
        }
    }
}
