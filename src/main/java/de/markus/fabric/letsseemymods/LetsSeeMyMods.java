package de.markus.fabric.letsseemymods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LetsSeeMyMods implements ModInitializer {
	public static final String MOD_ID = "letsseemymods";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@Override
	public void onInitialize() {
		ModVisibilityConfig config = ModVisibilityConfig.load();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher, config));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, ModVisibilityConfig config) {
		dispatcher.register(Commands.literal("mods")
				.executes(context -> {
					sendModList(context.getSource(), config);
					return 1;
				}));
	}

	private static void sendModList(CommandSourceStack source, ModVisibilityConfig config) {
		List<VisibleMod> mods = FabricLoader.getInstance()
				.getAllMods()
				.stream()
				.filter(config::shouldShow)
				.map(VisibleMod::from)
				.sorted(Comparator.comparing(VisibleMod::displayName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(VisibleMod::id))
				.toList();

		MutableComponent message = Component.literal("Mods (" + mods.size() + "): ")
				.withStyle(ChatFormatting.YELLOW);

		for (int i = 0; i < mods.size(); i++) {
			if (i > 0) {
				message.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
			}

			VisibleMod mod = mods.get(i);
			message.append(Component.literal(mod.displayName()).withStyle(ChatFormatting.GREEN));

			if (config.showVersions()) {
				message.append(Component.literal(" " + mod.version()).withStyle(ChatFormatting.DARK_GRAY));
			}
		}

		source.sendSuccess(() -> message, false);
	}

	private record VisibleMod(String id, String displayName, String version) {
		private static VisibleMod from(ModContainer container) {
			ModMetadata metadata = container.getMetadata();
			String name = metadata.getName();
			if (name == null || name.isBlank()) {
				name = metadata.getId();
			}

			return new VisibleMod(
					metadata.getId(),
					name,
					metadata.getVersion().getFriendlyString()
			);
		}
	}

	private static final class ModVisibilityConfig {
		private static final Set<String> DEFAULT_HIDDEN_IDS = Set.of(
				"authlib",
				"brigadier",
				"datafixerupper",
				"fabric-api",
				"fabricloader",
				"java",
				"letsseemymods",
				"minecraft",
				"mixinextras"
		);

		private static final Set<String> DEFAULT_HIDDEN_PREFIXES = Set.of(
				"fabric-",
				"fabric_"
		);

		private static final Set<String> DEFAULT_HIDDEN_SUFFIXES = Set.of(
				"-api",
				"-lib",
				"-library",
				"_api",
				"_lib",
				"_library"
		);

		private final Set<String> hiddenIds;
		private final List<String> hiddenPrefixes;
		private final List<String> hiddenSuffixes;
		private final boolean hideNestedHiddenMods;
		private final boolean hideApiOrLibraryNames;
		private final boolean showVersions;

		private ModVisibilityConfig(
				Collection<String> hiddenIds,
				Collection<String> hiddenPrefixes,
				Collection<String> hiddenSuffixes,
				boolean hideNestedHiddenMods,
				boolean hideApiOrLibraryNames,
				boolean showVersions
		) {
			this.hiddenIds = normalizeSet(hiddenIds);
			this.hiddenPrefixes = normalizeList(hiddenPrefixes);
			this.hiddenSuffixes = normalizeList(hiddenSuffixes);
			this.hideNestedHiddenMods = hideNestedHiddenMods;
			this.hideApiOrLibraryNames = hideApiOrLibraryNames;
			this.showVersions = showVersions;
		}

		private static ModVisibilityConfig load() {
			Path configPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
			ConfigFile defaults = ConfigFile.defaults();

			try {
				if (Files.notExists(configPath)) {
					Files.createDirectories(configPath.getParent());
					writeConfig(configPath, defaults);
					return defaults.toConfig();
				}

				try (Reader reader = Files.newBufferedReader(configPath)) {
					ConfigFile file = GSON.fromJson(reader, ConfigFile.class);
					if (file == null) {
						writeConfig(configPath, defaults);
						return defaults.toConfig();
					}

					return file.withDefaults(defaults).toConfig();
				}
			} catch (IOException exception) {
				return defaults.toConfig();
			}
		}

		private boolean shouldShow(ModContainer container) {
			ModMetadata metadata = container.getMetadata();
			String id = normalize(metadata.getId());

			if (isHiddenId(id) || startsWithHiddenPrefix(id) || endsWithHiddenSuffix(id)) {
				return false;
			}

			if (hideNestedHiddenMods && isContainedByHiddenMod(container)) {
				return false;
			}

			if (hideApiOrLibraryNames && hasApiOrLibraryDisplayName(metadata.getName())) {
				return false;
			}

			return true;
		}

		private boolean showVersions() {
			return showVersions;
		}

		private boolean isContainedByHiddenMod(ModContainer container) {
			return container.getContainingMod()
					.map(ModContainer::getMetadata)
					.map(ModMetadata::getId)
					.map(ModVisibilityConfig::normalize)
					.map(this::isHiddenId)
					.orElse(false);
		}

		private boolean isHiddenId(String id) {
			return hiddenIds.contains(id);
		}

		private boolean startsWithHiddenPrefix(String id) {
			return hiddenPrefixes.stream().anyMatch(id::startsWith);
		}

		private boolean endsWithHiddenSuffix(String id) {
			return hiddenSuffixes.stream().anyMatch(id::endsWith);
		}

		private boolean hasApiOrLibraryDisplayName(String name) {
			String normalizedName = normalize(name).trim();
			return normalizedName.endsWith(" api")
					|| normalizedName.endsWith(" lib")
					|| normalizedName.endsWith(" library");
		}

		private static Set<String> normalizeSet(Collection<String> values) {
			LinkedHashSet<String> normalized = new LinkedHashSet<>();
			for (String value : values) {
				String next = normalize(value);
				if (!next.isBlank()) {
					normalized.add(next);
				}
			}
			return Set.copyOf(normalized);
		}

		private static List<String> normalizeList(Collection<String> values) {
			ArrayList<String> normalized = new ArrayList<>();
			for (String value : values) {
				String next = normalize(value);
				if (!next.isBlank()) {
					normalized.add(next);
				}
			}
			return List.copyOf(normalized);
		}

		private static String normalize(String value) {
			return value == null ? "" : value.toLowerCase(Locale.ROOT);
		}

		private static void writeConfig(Path configPath, ConfigFile config) throws IOException {
			try (Writer writer = Files.newBufferedWriter(configPath)) {
				GSON.toJson(config, writer);
			}
		}
	}

	private static final class ConfigFile {
		private List<String> hiddenIds;
		private List<String> hiddenPrefixes;
		private List<String> hiddenSuffixes;
		private Boolean hideNestedHiddenMods;
		private Boolean hideApiOrLibraryNames;
		private Boolean showVersions;

		private static ConfigFile defaults() {
			ConfigFile config = new ConfigFile();
			config.hiddenIds = new ArrayList<>(ModVisibilityConfig.DEFAULT_HIDDEN_IDS);
			config.hiddenPrefixes = new ArrayList<>(ModVisibilityConfig.DEFAULT_HIDDEN_PREFIXES);
			config.hiddenSuffixes = new ArrayList<>(ModVisibilityConfig.DEFAULT_HIDDEN_SUFFIXES);
			config.hideNestedHiddenMods = true;
			config.hideApiOrLibraryNames = true;
			config.showVersions = true;
			return config;
		}

		private ConfigFile withDefaults(ConfigFile defaults) {
			if (hiddenIds == null) {
				hiddenIds = defaults.hiddenIds;
			}
			if (hiddenPrefixes == null) {
				hiddenPrefixes = defaults.hiddenPrefixes;
			}
			if (hiddenSuffixes == null) {
				hiddenSuffixes = defaults.hiddenSuffixes;
			}
			if (hideNestedHiddenMods == null) {
				hideNestedHiddenMods = defaults.hideNestedHiddenMods;
			}
			if (hideApiOrLibraryNames == null) {
				hideApiOrLibraryNames = defaults.hideApiOrLibraryNames;
			}
			if (showVersions == null) {
				showVersions = defaults.showVersions;
			}
			return this;
		}

		private ModVisibilityConfig toConfig() {
			return new ModVisibilityConfig(
					hiddenIds,
					hiddenPrefixes,
					hiddenSuffixes,
					hideNestedHiddenMods,
					hideApiOrLibraryNames,
					showVersions
			);
		}
	}
}
