package net.baldy.pixelstats.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.registries.PixelmonForms;
import com.pixelmonmod.pixelmon.client.gui.Resources;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.gui.inventory.InventoryPixelmonExtendedScreen;
import net.baldy.pixelstats.client.ClientIvsCache;
import net.baldy.pixelstats.network.NetworkHandler;
import net.baldy.pixelstats.network.RequestPokemonIvsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ScreenHelper.class, remap = false)
public abstract class ScreenHelperMixin {
	/**
	 * @author Baldy
	 * @reason Ajoute-les stats au tooltip hover du PC via sync serveur
	 */
	@Overwrite
	public static void drawPokemonHoverInfo(MatrixStack matrix, Pokemon pokemon, int x, int y) {
		if (pokemon == null) return;

		Minecraft mc = Minecraft.getInstance();
		FontRenderer fr = mc.font;

		ClientIvsCache.IvData cached = ClientIvsCache.get(pokemon.getUUID());

		if (NetworkHandler.INSTANCE != null && ClientIvsCache.shouldRequest(pokemon.getUUID())) {
			ClientIvsCache.markRequesting(pokemon.getUUID());
			NetworkHandler.INSTANCE.sendToServer(new RequestPokemonIvsPacket(pokemon.getUUID()));
		}

		boolean sneak = Screen.hasShiftDown();
		boolean showMoreInfo = false;

		String namePlain = pokemon.getFormattedDisplayName().getString();

		String formLine = null;
		String paletteLine = null;

		if (!pokemon.isEgg()) {
			if (pokemon.getForm() != null && !pokemon.getForm().isForm(PixelmonForms.NONE)) {
				formLine = I18n.get("gui.screenpokechecker.form",
					I18n.get(pokemon.getForm().getTranslationKey()));
			}

			if (!pokemon.isDefaultPalette() && pokemon.getPalette() != null) {
				paletteLine = I18n.get("gui.screenpokechecker.palette",
					pokemon.getPalette().getTranslatedName().getString());
			}
		}

		String level = I18n.get("gui.screenpokechecker.lvl") + pokemon.getPokemonLevel();
		String health = pokemon.getHealth() > 0
			? I18n.get("nbt.hp") + pokemon.getHealth() + "/" + pokemon.getMaxHealth()
			: I18n.get("gui.creativeinv.fainted");

		String bottomLine = null;
		String ivsLine = null;
		String abilityLine = null;
		String natureLine = null;
		String happinessLine = null;

		if (cached == null) {
			showMoreInfo = false;
			bottomLine = null;
		} else if (!cached.valid) {
			showMoreInfo = false;
			bottomLine = TextFormatting.GRAY + "Loading...";
		} else if (sneak) {
			showMoreInfo = true;

			int total = cached.total;
			int percent = (int) Math.round((cached.total / 186.0) * 100.0);
			TextFormatting totalColor = cached.hyperTrained ? TextFormatting.YELLOW : TextFormatting.AQUA;

			ivsLine = TextFormatting.GOLD + "IVs: "
				+ totalColor + total
				+ TextFormatting.AQUA + "/186"
				+ TextFormatting.GRAY + " ("
				+ totalColor + percent + "%"
				+ TextFormatting.RESET + TextFormatting.GRAY + ")";

			String abilityName = pokemon.getAbility() != null
				? pokemon.getAbility().getLocalizedName()
				: "N/A";

			String natureName = pokemon.getNature() != null
				? pokemon.getNature().getLocalizedName()
				: "N/A";

			int friendship = Math.max(pokemon.getFriendship(), 0);
			boolean isHa = pokemon.getAbility() != null && pokemon.hasHiddenAbility();

			abilityLine = TextFormatting.BLUE + I18n.get("gui.screenpokechecker.ability") + ": "
				+ (isHa ? TextFormatting.YELLOW : TextFormatting.WHITE) + abilityName
				+ (isHa
				? TextFormatting.GRAY + " (" + TextFormatting.RED + TextFormatting.BOLD + "HA"
				+ TextFormatting.RESET + TextFormatting.GRAY + ")"
				: "");

			natureLine = TextFormatting.GREEN + I18n.get("gui.screenpokechecker.nature") + ": "
				+ TextFormatting.WHITE + natureName;

			happinessLine = TextFormatting.LIGHT_PURPLE + I18n.get("gui.screenpokechecker.happiness") + ": "
				+ TextFormatting.WHITE + friendship + "/255";

		} else {
			showMoreInfo = false;
			bottomLine = TextFormatting.GRAY + "Sneak for more info";
		}

		List<String> baseLines = new ArrayList<>();
		if (formLine != null) {
			baseLines.add(formLine);
		}
		if (paletteLine != null) {
			baseLines.add(paletteLine);
		}
		baseLines.add(level + " " + health);
		if (bottomLine != null) {
			baseLines.add(bottomLine);
		}

		List<String> statsLines = new ArrayList<>();
		if (showMoreInfo) {
			statsLines.add(ivsLine);
			statsLines.add(abilityLine);
			statsLines.add(natureLine);
			statsLines.add(happinessLine);
		}

		int baseMaxWidth = fr.width(namePlain);
		for (String line : baseLines) {
			if (line != null && !line.isEmpty()) {
				baseMaxWidth = Math.max(baseMaxWidth, fr.width(line));
			}
		}

		int statsMaxWidth = 0;
		for (String line : statsLines) {
			if (line != null && !line.isEmpty()) {
				statsMaxWidth = Math.max(statsMaxWidth, fr.width(line));
			}
		}

		int padding = 2;
		int sharedWidth = Math.max(baseMaxWidth, statsMaxWidth) + padding + padding;

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();

		boolean isInventoryLike = mc.screen instanceof InventoryPixelmonExtendedScreen;

		int left;
		if (isInventoryLike) {
			left = x - sharedWidth - 8;
			if (left < 4) {
				left = x + 12 + 5;
				if (left + sharedWidth > screenWidth) left = screenWidth - sharedWidth - 4;
			}
		} else {
			left = x + 12 + 5;
			if (left + sharedWidth > screenWidth) left -= 28 + sharedWidth;
			if (left < 4) left = 4;
		}

		int baseTop = y - 12;
		int baseHeight = 10 + 2 + (baseLines.size() * 10);
		int baseRight = left + sharedWidth;
		int baseBottom = baseTop + baseHeight;

		int statsTop = baseBottom + 2;
		int statsHeight = 0;
		int statsBottom = statsTop;

		if (!statsLines.isEmpty()) {
			statsHeight = 3 + (statsLines.size() * 10);
			statsBottom = statsTop + statsHeight;
		}

		if (!statsLines.isEmpty()) {
			int totalHeight = baseHeight + 2 + statsHeight;
			if (baseTop + totalHeight + 6 > screenHeight) {
				baseTop = screenHeight - totalHeight - 6;
				if (baseTop < 4) {
					baseTop = 4;
				}
				baseBottom = baseTop + baseHeight;
				statsTop = baseBottom + 2;
				statsBottom = statsTop + statsHeight;
			}
		} else {
			if (baseBottom + 6 > screenHeight) {
				baseTop = screenHeight - baseHeight - 6;
				if (baseTop < 4) {
					baseTop = 4;
				}
				baseBottom = baseTop + baseHeight;
			}
		}

		if (baseTop < 4) {
			baseTop = 4;
			baseBottom = baseTop + baseHeight;
			if (!statsLines.isEmpty()) {
				statsTop = baseBottom + 2;
				statsBottom = statsTop + statsHeight;
			}
		}

		AbstractGui.fill(matrix, left, baseTop, baseRight, baseBottom, -1437248170);

		fr.drawShadow(matrix, namePlain, (float) (left + 2), (float) (baseTop + 2), 0xFFFFFF);

		mc.getTextureManager().bind(Resources.pixelmonOverlay);

		if (!pokemon.isEgg() && pokemon.getGender() != null && pokemon.getGender() != Gender.NONE) {
			ResourceLocation rl = null;

			if (pokemon.getGender() == Gender.MALE) {
				rl = Resources.male;
			} else if (pokemon.getGender() == Gender.FEMALE) {
				rl = Resources.female;
			}

			if (rl != null) {
				ScreenHelper.drawImageQuad(
					rl,
					matrix,
					(float) (left + 3 + fr.width(namePlain)),
					(float) (baseTop + 2),
					5.0F,
					8.0F,
					0.0F,
					0.0F,
					1.0F,
					1.0F,
					1.0F,
					1.0F,
					1.0F,
					1.0F,
					0.0F
				);
			}
		}

		int baseTextY = baseTop + 12;
		for (String line : baseLines) {
			if (line != null && !line.isEmpty()) {
				fr.drawShadow(matrix, line, (float) (left + 2), (float) baseTextY, 0xFFFFFF);
			}
			baseTextY += 10;
		}

		if (!statsLines.isEmpty()) {
			int statsRight = left + sharedWidth;
			AbstractGui.fill(matrix, left, statsTop, statsRight, statsBottom, -1437248170);

			int statsTextY = statsTop + 3;
			for (String line : statsLines) {
				if (line != null && !line.isEmpty()) {
					fr.drawShadow(matrix, line, (float) (left + 2), (float) statsTextY, 0xFFFFFF);
				}
				statsTextY += 10;
			}
		}

		if (!pokemon.getHeldItem().isEmpty()) {
			mc.getItemRenderer().renderGuiItem(pokemon.getHeldItem(), baseRight - 18, baseTop + 1);
		}
	}
}