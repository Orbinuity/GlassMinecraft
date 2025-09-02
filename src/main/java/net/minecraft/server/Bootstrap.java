package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import org.slf4j.Logger;

public class Bootstrap {
    public static final PrintStream STDOUT;
    private static volatile boolean isBootstrapped;
    private static final Logger LOGGER;
    public static final AtomicLong bootstrapDuration;

    public static void bootStrap() {
        if (!isBootstrapped) {
            isBootstrapped = true;
            Instant $$0 = Instant.now();
            if (BuiltInRegistries.REGISTRY.keySet().isEmpty()) {
                throw new IllegalStateException("Unable to load registries");
            } else {
                FireBlock.bootStrap();
                ComposterBlock.bootStrap();
                // Glass insert
                io.github.Orbinuity.GlassMinecraft.engine.Bootstrap.bootStrap();
                if (EntityType.getKey(EntityType.PLAYER) == null) {
                    throw new IllegalStateException("Failed loading EntityTypes");
                } else {
                    PotionBrewing.bootStrap();
                    EntitySelectorOptions.bootStrap();
                    DispenseItemBehavior.bootStrap();
                    CauldronInteraction.bootStrap();
                    BuiltInRegistries.bootStrap();
                    CreativeModeTabs.validate();
                    wrapStreams();
                    bootstrapDuration.set(Duration.between($$0, Instant.now()).toMillis());
                }
            }
        }
    }

    private static <T> void checkTranslations(Iterable<T> $$0, Function<T, String> $$1, Set<String> $$2) {
        Language $$3 = Language.getInstance();
        $$0.forEach(($$3x) -> {
            String $$4 = (String)$$1.apply($$3x);
            if (!$$3.has($$4)) {
                $$2.add($$4);
            }

        });
    }

    private static void checkGameruleTranslations(final Set<String> $$0) {
        final Language $$1 = Language.getInstance();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> $$0x, GameRules.Type<T> $$1x) {
                if (!$$1.has($$0x.getDescriptionId())) {
                    $$0.add($$0x.getId());
                }

            }
        });
    }

    public static Set<String> getMissingTranslations() {
        Set<String> $$0 = new TreeSet();
        checkTranslations(BuiltInRegistries.ATTRIBUTE, Attribute::getDescriptionId, $$0);
        checkTranslations(BuiltInRegistries.ENTITY_TYPE, EntityType::getDescriptionId, $$0);
        checkTranslations(BuiltInRegistries.MOB_EFFECT, MobEffect::getDescriptionId, $$0);
        checkTranslations(BuiltInRegistries.ITEM, Item::getDescriptionId, $$0);
        checkTranslations(BuiltInRegistries.ENCHANTMENT, Enchantment::getDescriptionId, $$0);
        checkTranslations(BuiltInRegistries.BLOCK, Block::getDescriptionId, $$0);
        checkTranslations(BuiltInRegistries.CUSTOM_STAT, ($$0x) -> {
            String var10000 = $$0x.toString();
            return "stat." + var10000.replace(':', '.');
        }, $$0);
        checkGameruleTranslations($$0);
        return $$0;
    }

    public static void checkBootstrapCalled(Supplier<String> $$0) {
        if (!isBootstrapped) {
            throw createBootstrapException($$0);
        }
    }

    private static RuntimeException createBootstrapException(Supplier<String> $$0) {
        try {
            String $$1 = (String)$$0.get();
            return new IllegalArgumentException("Not bootstrapped (called from " + $$1 + ")");
        } catch (Exception $$2) {
            RuntimeException $$3 = new IllegalArgumentException("Not bootstrapped (failed to resolve location)");
            $$3.addSuppressed($$2);
            return $$3;
        }
    }

    public static void validate() {
        checkBootstrapCalled(() -> "validate");
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            getMissingTranslations().forEach(($$0) -> LOGGER.error("Missing translations: {}", $$0));
            Commands.validate();
        }

        DefaultAttributes.validate();
    }

    private static void wrapStreams() {
        if (LOGGER.isDebugEnabled()) {
            System.setErr(new DebugLoggedPrintStream("STDERR", System.err));
            System.setOut(new DebugLoggedPrintStream("STDOUT", STDOUT));
        } else {
            System.setErr(new LoggedPrintStream("STDERR", System.err));
            System.setOut(new LoggedPrintStream("STDOUT", STDOUT));
        }

    }

    public static void realStdoutPrintln(String $$0) {
        STDOUT.println($$0);
    }

    static {
        STDOUT = System.out;
        LOGGER = LogUtils.getLogger();
        bootstrapDuration = new AtomicLong(-1L);
    }
}