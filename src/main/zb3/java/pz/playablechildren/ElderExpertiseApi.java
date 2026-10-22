package pz.playablechildren;

import me.zed_0xff.zombie_buddy.Exposer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.skills.PerkFactory;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Recipe;

/** Manual actions opt in; native recipe timing and machine processing stay vanilla. */
@Exposer.LuaClass(name = "PlayableChildren.ElderExpertise")
public final class ElderExpertiseApi {
    private ElderExpertiseApi() {}

    private static boolean mastered(IsoGameCharacter character, PerkFactory.Perk perk) {
        return perk != null && AgeGameplayApi.expertiseFactor(
            LifeStage.ELDER, perk, character.getPerkLevel(perk)) > 1.0f;
    }

    private static boolean relatedMastery(IsoGameCharacter character, Object recipe) {
        if (recipe instanceof CraftRecipe craft) {
            for (int i = 0; i < craft.getRequiredSkillCount(); i++) {
                CraftRecipe.RequiredSkill skill = craft.getRequiredSkill(i);
                if (skill != null && mastered(character, skill.getPerk())) return true;
            }
            for (int i = 0; i < craft.getXPAwardCount(); i++) {
                CraftRecipe.XpAward award = craft.getXPAward(i);
                if (award != null && award.getAmount() > 0
                    && mastered(character, award.getPerk())) return true;
            }
        } else if (recipe instanceof Recipe legacy) {
            for (int i = 0; i < legacy.getRequiredSkillCount(); i++) {
                Recipe.RequiredSkill skill = legacy.getRequiredSkill(i);
                if (skill != null && mastered(character, skill.getPerk())) return true;
            }
        }
        return false;
    }

    public static double scaleManualDuration(Object value, Object recipe, double duration) {
        if (!Double.isFinite(duration) || duration <= 0
            || !(value instanceof IsoGameCharacter character)
            || AgeGameplayApi.stageOf(character) != LifeStage.ELDER) return duration;
        return relatedMastery(character, recipe) ? duration * 0.90 : duration;
    }
}
